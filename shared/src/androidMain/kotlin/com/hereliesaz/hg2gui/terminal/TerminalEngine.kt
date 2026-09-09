package com.hereliesaz.hg2gui.terminal

import android.content.Context
import com.hereliesaz.hg2gui.managers.StyledSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/** Decides whether a command belongs to HG2Gui itself or the interactive shell. */
class TerminalEngine(
    private val context: Context,
    private val home: File? = null
) {
    private var shell = ShellSession.forAndroid(home, context)
    private var pendingBackendNotice: String? = shell.fallbackNoticeOrNull()
    private val client = sharedHttpClient(context)
    private val downloader by lazy { Hg2Downloader(context.applicationContext, client) }
    private val packages by lazy { Hg2PackageManager(context.applicationContext, client, downloader) }

    private fun ShellSession.fallbackNoticeOrNull(): String? =
        backendDescription.takeIf { it.startsWith("the bare system shell") }
            ?.let { "[using $it]" }

    val workingDirectory: String get() = shell.workingDirectory

    fun run(
        line: String,
        onNeedInput: suspend (prompt: String) -> String,
        onExit: (Int?) -> Unit = {},
        onStderr: (String) -> Unit = {},
        onStyledOutput: (lines: List<List<StyledSpan>>) -> Unit = {}
    ): Flow<String> = callbackFlow {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            close()
            return@callbackFlow
        }

        val verb = trimmed.substringBefore(' ')
        val aptPackageCommand = translateAptPackageCommand(trimmed)

        when {
            verb == "bootstrap" -> launch(Dispatchers.IO) {
                DistroManager.bootstrap(context, client).collect { trySend(it) }
                val old = shell
                shell = ShellSession.forAndroid(home, context)
                pendingBackendNotice = shell.fallbackNoticeOrNull()
                old.close()
                onExit(null)
                close()
            }

            verb == "download" || verb == "hg2download" -> launch(Dispatchers.IO) {
                try {
                    downloader.run(trimmed).collect { trySend(it) }
                    onExit(0)
                } catch (e: Exception) {
                    trySend("HG2Gui download error: ${e.message ?: e.javaClass.simpleName}")
                    onExit(-1)
                } finally {
                    close()
                }
            }

            verb == "pkg" || verb == "hg2pkg" || aptPackageCommand != null -> launch(Dispatchers.IO) {
                val packageLine = aptPackageCommand ?: trimmed
                try {
                    packages.run(packageLine).collect { trySend(it) }
                    onExit(0)
                } catch (e: Exception) {
                    trySend("HG2Gui package error: ${e.message ?: e.javaClass.simpleName}")
                    onExit(-1)
                } finally {
                    close()
                }
            }

            verb in Builtins.NAMES -> launch(Dispatchers.IO) {
                send(Builtins.run(context, trimmed))
                onExit(null)
                close()
            }

            else -> launch(Dispatchers.IO) {
                val notice = pendingBackendNotice
                pendingBackendNotice = null
                val exitCode = shell.stream(
                    trimmed,
                    onLine = { output -> trySend(if (notice != null) "$notice\n$output" else output) },
                    onNeedInput = { prompt -> runBlocking { onNeedInput(prompt) } },
                    onStderrLine = { output -> onStderr(output) },
                    onStyledLine = { lines -> onStyledOutput(lines) }
                )
                onExit(exitCode)
                close()
            }
        }
        awaitClose { }
    }

    /** Headless one-shot execution used by installers and MCP callers. */
    suspend fun runToCompletion(line: String): Pair<String, Int> = withContext(Dispatchers.IO) {
        val trimmed = line.trim()
        val verb = trimmed.substringBefore(' ')
        val aptPackageCommand = translateAptPackageCommand(trimmed)
        val hg2Flow = when {
            verb == "download" || verb == "hg2download" -> downloader.run(trimmed)
            verb == "pkg" || verb == "hg2pkg" -> packages.run(trimmed)
            aptPackageCommand != null -> packages.run(aptPackageCommand)
            else -> null
        }
        if (hg2Flow != null) {
            val transcript = StringBuilder()
            return@withContext try {
                hg2Flow.collect { output ->
                    if (transcript.isNotEmpty()) transcript.append('\n')
                    transcript.append(output)
                }
                transcript.toString() to 0
            } catch (e: Exception) {
                if (transcript.isNotEmpty()) transcript.append('\n')
                transcript.append("HG2Gui error: ${e.message ?: e.javaClass.simpleName}")
                transcript.toString() to -1
            }
        }

        var transcript = ""
        val exitCode = shell.stream(
            trimmed,
            onLine = { transcript = it },
            onNeedInput = { null },
            onStderrLine = {},
            onStyledLine = {}
        )
        transcript to exitCode
    }

    fun interrupt() {
        shell.interrupt()
    }

    fun destroy() {
        shell.close()
    }

    private fun translateAptPackageCommand(line: String): String? {
        val words = shellWords(line)
        if (words.isEmpty() || words.first() !in setOf("apt", "apt-get")) return null

        val operationIndex = words.indexOfFirst { it in APT_MUTATING_OPERATIONS }
        if (operationIndex < 0) return null

        val operation = words[operationIndex]
        val translatedOperation = when (operation) {
            "install" -> "install"
            "remove" -> "remove"
            "purge" -> "purge"
            "update" -> "update"
            "upgrade", "dist-upgrade", "full-upgrade" -> "upgrade"
            else -> return null
        }

        val packages = words.drop(operationIndex + 1).filterNot { it.startsWith("-") }
        return when (translatedOperation) {
            "update", "upgrade" -> "pkg $translatedOperation"
            else -> if (packages.isEmpty()) null else "pkg $translatedOperation ${packages.joinToString(" ")}"
        }
    }

    private fun shellWords(line: String): List<String> = Regex("""(?:[^\s\"']+|\"[^\"]*\"|'[^']*')+""")
        .findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    companion object {
        private val APT_MUTATING_OPERATIONS = setOf(
            "install",
            "remove",
            "purge",
            "update",
            "upgrade",
            "dist-upgrade",
            "full-upgrade"
        )

        @Volatile
        private var instance: OkHttpClient? = null

        private fun sharedHttpClient(context: Context): OkHttpClient {
            return instance ?: synchronized(this) {
                instance ?: OkHttpClient.Builder()
                    .cache(Cache(File(context.applicationContext.cacheDir, "http"), (10 * 1024 * 1024).toLong()))
                    .build()
                    .also { instance = it }
            }
        }
    }
}
