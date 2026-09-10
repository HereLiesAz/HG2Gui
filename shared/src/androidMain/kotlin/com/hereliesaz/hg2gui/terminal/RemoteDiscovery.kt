package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.util.AtomicFile
import com.hereliesaz.hg2gui.managers.SshPresets
import com.hereliesaz.hg2gui.ui.ssh.SshPreset
import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

/** Live, target-scoped SSH metadata. Discovery is cached only after a successful remote query. */
object RemoteDiscovery {
    private const val DIRECTORY = "remote-discovery"
    private const val TIMEOUT_SECONDS = 15L
    private const val MAX_OUTPUT_BYTES = 512 * 1024
    private const val MAX_COMMANDS = 600
    private const val MAX_PACKAGES = 1_500
    private val safeCommand = Regex("[A-Za-z0-9_.+:/-]{1,120}")

    data class Inventory(
        val presetName: String,
        val os: String,
        val shell: String,
        val packageManager: String?,
        val commands: List<String>,
        val packages: List<String>,
        val updatedAtMillis: Long
    )

    fun run(context: Context, args: List<String>): String {
        val action = args.firstOrNull() ?: return usage()
        val presetName = args.getOrNull(1) ?: return usage()
        val preset = SshPresets.list(context).firstOrNull { it.name == presetName }
            ?: return "No SSH preset named '$presetName'."
        return when (action) {
            "discover" -> discover(context, preset)
            "help" -> {
                val command = args.getOrNull(2) ?: return "Usage: remote help <preset> <command>"
                if (!safeCommand.matches(command)) return "Refusing unsafe remote command name."
                remoteHelp(context, preset, command)
            }
            "packages" -> remotePackages(context, preset)
            "status" -> cached(context, presetName)?.let(::summary) ?: "No cached discovery for '$presetName'. Run: remote discover '$presetName'"
            else -> usage()
        }
    }

    fun cached(context: Context, presetName: String): Inventory? {
        val file = cacheFile(context, presetName)
        if (!file.isFile) return null
        return runCatching { parse(JSONObject(file.readText())) }.getOrNull()
    }

    fun remoteCommand(preset: SshPreset, command: String): String =
        buildSshDisplayCommand(preset) + " " + shellQuote(command)

    private fun discover(context: Context, preset: SshPreset): String {
        val marker = "__HG2GUI__"
        val script = """
            printf '$marker OS\\t'; (uname -s 2>/dev/null || printf unknown)
            printf '$marker SHELL\\t%s\\n' "${'$'}{SHELL:-unknown}"
            printf '$marker PM\\t'; if command -v apt >/dev/null 2>&1; then printf apt; elif command -v dnf >/dev/null 2>&1; then printf dnf; elif command -v yum >/dev/null 2>&1; then printf yum; elif command -v pacman >/dev/null 2>&1; then printf pacman; elif command -v apk >/dev/null 2>&1; then printf apk; elif command -v brew >/dev/null 2>&1; then printf brew; else printf none; fi; printf '\\n'
            oldifs="${'$'}IFS"; IFS=:; for d in ${'$'}PATH; do [ -d "${'$'}d" ] || continue; for f in "${'$'}d"/*; do [ -f "${'$'}f" ] && [ -x "${'$'}f" ] && printf '$marker CMD\\t%s\\n' "${'$'}{f##*/}"; done; done; IFS="${'$'}oldifs"
            if command -v dpkg-query >/dev/null 2>&1; then dpkg-query -W -f='${'$'}{binary:Package}\\n' 2>/dev/null | sed 's/^/$marker PKG\\t/';
            elif command -v rpm >/dev/null 2>&1; then rpm -qa --qf '%{NAME}\\n' 2>/dev/null | sed 's/^/$marker PKG\\t/';
            elif command -v pacman >/dev/null 2>&1; then pacman -Qq 2>/dev/null | sed 's/^/$marker PKG\\t/';
            elif command -v apk >/dev/null 2>&1; then apk info 2>/dev/null | sed 's/^/$marker PKG\\t/';
            elif command -v brew >/dev/null 2>&1; then brew list --formula 2>/dev/null | sed 's/^/$marker PKG\\t/'; fi
        """.trimIndent()
        val result = execute(context, preset, script)
        if (result.exitCode != 0) return "Remote discovery failed (${result.exitCode}):\n${result.output.take(4_000)}"

        var os = "unknown"
        var shell = "unknown"
        var manager: String? = null
        val commands = linkedSetOf<String>()
        val packages = linkedSetOf<String>()
        result.output.lineSequence().forEach { line ->
            if (!line.startsWith(marker)) return@forEach
            val body = line.removePrefix(marker).trimStart()
            val split = body.indexOf('\t')
            if (split <= 0) return@forEach
            val kind = body.substring(0, split).trim()
            val value = body.substring(split + 1).trim()
            when (kind) {
                "OS" -> os = value
                "SHELL" -> shell = value
                "PM" -> manager = value.takeUnless { it == "none" || it.isBlank() }
                "CMD" -> if (commands.size < MAX_COMMANDS && safeCommand.matches(value)) commands += value
                "PKG" -> if (packages.size < MAX_PACKAGES && value.isNotBlank()) packages += value
            }
        }
        val inventory = Inventory(
            presetName = preset.name,
            os = os,
            shell = shell,
            packageManager = manager,
            commands = commands.sorted(),
            packages = packages.sorted(),
            updatedAtMillis = System.currentTimeMillis()
        )
        save(context, inventory)
        return summary(inventory)
    }

    private fun remoteHelp(context: Context, preset: SshPreset, command: String): String {
        val script = "command -v ${shellQuote(command)} >/dev/null 2>&1 || { printf 'Command not found: %s\\n' ${shellQuote(command)}; exit 127; }; ${shellQuote(command)} --help 2>&1 | head -n 160"
        val result = execute(context, preset, script)
        return result.output.ifBlank { "Remote help exited ${result.exitCode} with no output." }
    }

    private fun remotePackages(context: Context, preset: SshPreset): String {
        val inventory = cached(context, preset.name) ?: return discover(context, preset)
        val manager = inventory.packageManager ?: return "No supported remote package manager was discovered on ${preset.name}."
        val command = when (manager) {
            "apt" -> "apt list --installed 2>/dev/null"
            "dnf" -> "dnf list installed"
            "yum" -> "yum list installed"
            "pacman" -> "pacman -Q"
            "apk" -> "apk info -v"
            "brew" -> "brew list --versions"
            else -> return "Unsupported remote package manager: $manager"
        }
        val result = execute(context, preset, command)
        return result.output.ifBlank { "Remote package inventory exited ${result.exitCode} with no output." }
    }

    private data class ExecResult(val output: String, val exitCode: Int)

    private fun execute(context: Context, preset: SshPreset, script: String): ExecResult {
        val ssh = findSsh(context) ?: return ExecResult("SSH client is unavailable. Install openssh first.", 127)
        val command = mutableListOf(
            ssh.absolutePath,
            "-o", "BatchMode=yes",
            "-o", "ConnectTimeout=8",
            "-p", preset.port.toString()
        )
        preset.keyPath?.takeIf(String::isNotBlank)?.let { command += listOf("-i", it) }
        command += target(preset)
        command += "sh -lc ${shellQuote(script)}"

        return runCatching {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val buffer = ByteArray(MAX_OUTPUT_BYTES)
            var used = 0
            val reader = Thread {
                process.inputStream.use { input ->
                    while (used < buffer.size) {
                        val count = input.read(buffer, used, buffer.size - used)
                        if (count <= 0) break
                        used += count
                    }
                }
            }.apply { start() }
            val finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            reader.join(1_000)
            ExecResult(String(buffer, 0, used, Charsets.UTF_8), if (finished) process.exitValue() else 124)
        }.getOrElse { ExecResult(it.message ?: it.javaClass.simpleName, 126) }
    }

    private fun save(context: Context, inventory: Inventory) {
        val file = cacheFile(context, inventory.presetName)
        file.parentFile?.mkdirs()
        val objectJson = JSONObject()
            .put("presetName", inventory.presetName)
            .put("os", inventory.os)
            .put("shell", inventory.shell)
            .put("packageManager", inventory.packageManager ?: JSONObject.NULL)
            .put("commands", JSONArray(inventory.commands))
            .put("packages", JSONArray(inventory.packages))
            .put("updatedAtMillis", inventory.updatedAtMillis)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try {
            stream.write(objectJson)
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    private fun parse(json: JSONObject): Inventory = Inventory(
        presetName = json.getString("presetName"),
        os = json.optString("os", "unknown"),
        shell = json.optString("shell", "unknown"),
        packageManager = if (json.isNull("packageManager")) null else json.optString("packageManager").takeIf(String::isNotBlank),
        commands = json.optJSONArray("commands").strings(MAX_COMMANDS),
        packages = json.optJSONArray("packages").strings(MAX_PACKAGES),
        updatedAtMillis = json.optLong("updatedAtMillis")
    )

    private fun JSONArray?.strings(limit: Int): List<String> = if (this == null) emptyList() else buildList {
        for (index in 0 until length().coerceAtMost(limit)) optString(index).takeIf(String::isNotBlank)?.let(::add)
    }

    private fun summary(inventory: Inventory): String = buildString {
        append("${inventory.presetName}: ${inventory.os}, ${inventory.shell}, ")
        append(inventory.packageManager ?: "no supported package manager")
        append(" · ${inventory.commands.size} commands · ${inventory.packages.size} packages")
    }

    private fun findSsh(context: Context): File? {
        val prefix = DistroManager.prefixDir(context)
        val native = File(context.applicationInfo.nativeLibraryDir)
        return listOf(File(native, "libbin_ssh.so"), File(prefix, "bin/ssh"))
            .firstOrNull { runCatching { it.isFile && it.canExecute() }.getOrDefault(false) }
    }

    private fun cacheFile(context: Context, presetName: String): File =
        File(File(context.filesDir, DIRECTORY), safeKey(presetName) + ".json")

    private fun target(preset: SshPreset): String = if (preset.user.isBlank()) preset.host else "${preset.user}@${preset.host}"

    private fun buildSshDisplayCommand(preset: SshPreset): String = buildString {
        append("ssh -p ").append(preset.port)
        preset.keyPath?.takeIf(String::isNotBlank)?.let { append(" -i ").append(shellQuote(it)) }
        append(' ').append(shellQuote(target(preset)))
    }

    private fun usage(): String = "Usage: remote <discover|status|help|packages> <preset> [command]"
    private fun safeKey(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_")
    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
}
