package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput

class DummyTerminalOutput : TerminalOutput() {
    override fun write(data: ByteArray, offset: Int, count: Int) {}
    override fun titleChanged(oldTitle: String?, newTitle: String?) {}
    override fun onCopyTextToClipboard(text: String) {}
    override fun onPasteTextFromClipboard() {}
    override fun onBell() {}
    override fun onColorsChanged() {}
}

actual class ShellSession private constructor(
    home: File?,
    command: Array<String>,
    ldLibraryPath: String?
) {

    companion object {
        private const val SENTINEL = "__HG2GUI_EOC_a7f3__"
        private const val DEFAULT_SHELL = "/system/bin/sh"
        private const val ZSH_LIB_NAME = "libzsh.so"
        private const val TIMEOUT_MS = 15_000L
        private const val STARTUP_PROBE_MS = 300L

        private fun setupZshrc(home: File?) {
            if (home == null) return
            try {
                if (!home.exists()) home.mkdirs()
                val zshrc = File(home, ".zshrc")
                if (!zshrc.exists()) {
                    zshrc.writeText(
                        "export ZSH=\"\$HOME/.oh-my-zsh\"\n" +
                        "if command -v git &> /dev/null; then\n" +
                        "  if [ ! -d \"\$ZSH\" ]; then\n" +
                        "      git clone https://github.com/ohmyzsh/ohmyzsh.git \"\$ZSH\"\n" +
                        "  fi\n" +
                        "  ZSH_CUSTOM=\"\$ZSH/custom\"\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/zsh-completions\" ]; then\n" +
                        "      git clone https://github.com/zsh-users/zsh-completions \"\$ZSH_CUSTOM/plugins/zsh-completions\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/zsh-autosuggestions\" ]; then\n" +
                        "      git clone https://github.com/zsh-users/zsh-autosuggestions \"\$ZSH_CUSTOM/plugins/zsh-autosuggestions\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/zsh-syntax-highlighting\" ]; then\n" +
                        "      git clone https://github.com/zsh-users/zsh-syntax-highlighting.git \"\$ZSH_CUSTOM/plugins/zsh-syntax-highlighting\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/zsh-history-substring-search\" ]; then\n" +
                        "      git clone https://github.com/zsh-users/zsh-history-substring-search \"\$ZSH_CUSTOM/plugins/zsh-history-substring-search\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/you-should-use\" ]; then\n" +
                        "      git clone https://github.com/MichaelAquilina/zsh-you-should-use.git \"\$ZSH_CUSTOM/plugins/you-should-use\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/zsh-bat\" ]; then\n" +
                        "      git clone https://github.com/fdellutri/zsh-bat.git \"\$ZSH_CUSTOM/plugins/zsh-bat\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/zsh-defer\" ]; then\n" +
                        "      git clone https://github.com/romkatv/zsh-defer.git \"\$ZSH_CUSTOM/plugins/zsh-defer\"\n" +
                        "  fi\n" +
                        "  if [ ! -d \"\$ZSH_CUSTOM/plugins/fzf-zsh-plugin\" ]; then\n" +
                        "      git clone https://github.com/unixorn/fzf-zsh-plugin.git \"\$ZSH_CUSTOM/plugins/fzf-zsh-plugin\"\n" +
                        "  fi\n" +
                        "fi\n" +
                        "plugins=(zsh-completions zsh-history-substring-search zsh-autosuggestions zsh-syntax-highlighting thefuck you-should-use sudo zsh-bat copypath copyfile zsh-defer dotenv command-not-found fzf-zsh-plugin extract warp zsh-vi-man zsnapshot)\n" +
                        "if [ -f \"\$ZSH/oh-my-zsh.sh\" ]; then\n" +
                        "  source \"\$ZSH/oh-my-zsh.sh\"\n" +
                        "fi\n" +
                        "zmodload zsh/parameter\n" +
                        "autoload -Uz add-zsh-hook\n" +
                        "autoload -Uz zsh-capture-completion\n" +
                        "autoload -Uz zsh-mime-setup\n" +
                        "zsh-mime-setup\n" +
                        "if command -v starship &> /dev/null; then\n" +
                        "    eval \"\$(starship init zsh)\"\n" +
                        "fi\n" +
                        "if command -v atuin &> /dev/null; then\n" +
                        "    eval \"\$(atuin init zsh)\"\n" +
                        "fi\n" +
                        "# Virtual Terminal State Integration (VT Sequences)\n" +
                        "printf '\\033P\$q\"p\\033\\\\'\n"
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        fun forAndroid(home: File?, context: Context): ShellSession {
            val zsh = File(context.applicationInfo.nativeLibraryDir, ZSH_LIB_NAME)
            if (zsh.canExecute()) {
                setupZshrc(home)
                val session = ShellSession(
                    home, arrayOf(zsh.absolutePath), zsh.parent
                )
                if (session.survivedStartup()) return session
                session.close()
            }
            return ShellSession(home)
        }
    }

    private var process: Process? = null
    private var stdin: BufferedWriter? = null
    private var stdout: BufferedReader? = null

    private val alive = AtomicBoolean(false)

    @Volatile
    private var _workingDirectory: String = home?.absolutePath ?: "/"

    actual val workingDirectory: String
        get() = _workingDirectory

    actual val isAlive: Boolean
        get() = alive.get() && process != null && processStillRunning()

    constructor(home: File?) : this(home, arrayOf(DEFAULT_SHELL), null)

    constructor(home: File?, shellPath: String) : this(home, arrayOf(shellPath), null)

    actual constructor(homePath: String?) : this(homePath?.let { File(it) })

    actual constructor(homePath: String?, shellPath: String) : this(homePath?.let { File(it) }, shellPath)

    init {
        try {
            val builder = ProcessBuilder(*command)
            builder.redirectErrorStream(true)
            if (home != null && home.isDirectory) builder.directory(home)
            if (ldLibraryPath != null) {
                builder.environment()["LD_LIBRARY_PATH"] = ldLibraryPath
                if (home != null) builder.environment()["HOME"] = home.absolutePath
            }

            val p = builder.start()
            process = p
            stdin = BufferedWriter(OutputStreamWriter(p.outputStream))
            stdout = BufferedReader(InputStreamReader(p.inputStream))

            alive.set(true)
        } catch (e: IOException) {
            alive.set(false)
        }
    }

    private fun survivedStartup(): Boolean {
        if (!alive.get()) return false
        try {
            Thread.sleep(STARTUP_PROBE_MS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return processStillRunning()
    }

    private fun processStillRunning(): Boolean {
        return try {
            process?.exitValue()
            false
        } catch (stillRunning: IllegalThreadStateException) {
            true
        }
    }

    actual fun exec(command: String): ShellSessionResult {
        val collected = StringBuilder()
        val exitCode = stream(command) { line ->
            if (collected.isNotEmpty()) collected.append('\n')
            collected.append(line)
        }
        return ShellSessionResult(collected.toString(), exitCode, _workingDirectory)
    }

    actual fun stream(command: String, onLine: (line: String) -> Unit): Int {
        if (!isAlive) {
            onLine("shell is not running")
            return -1
        }

        var exitCode = -1

        try {
            val sin = stdin ?: throw IOException("stdin is null")
            val sout = stdout ?: throw IOException("stdout is null")
            
            // Create a headless VT100 parser for this execution
            val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)

            sin.write(command)
            sin.write("\n")
            sin.write("printf '%s%d:%s\\n' \"$SENTINEL\" \"$?\" \"\$PWD\"\n")
            sin.flush()

            val deadline = System.currentTimeMillis() + TIMEOUT_MS

            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    val bytes = "\n[timed out after ${TIMEOUT_MS / 1000}s]".toByteArray()
                    emulator.append(bytes, bytes.size)
                    onLine(emulator.mScreen.transcriptTextWithFullLinesJoined)
                    break
                }

                if (!sout.ready()) {
                    try {
                        Thread.sleep(10L)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    if (!processStillRunning()) {
                        alive.set(false)
                        break
                    }
                    continue
                }

                val line = sout.readLine()
                if (line == null) {
                    alive.set(false)
                    break
                }

                val marker = line.indexOf(SENTINEL)
                if (marker >= 0) {
                    if (marker > 0) {
                        val bytes = (line.substring(0, marker) + "\n").toByteArray(Charsets.UTF_8)
                        emulator.append(bytes, bytes.size)
                    }

                    val tail = line.substring(marker + SENTINEL.length)
                    val split = tail.indexOf(':')
                    if (split > 0) {
                        try {
                            exitCode = tail.substring(0, split).toInt()
                        } catch (ignored: NumberFormatException) {
                        }
                        val pwd = tail.substring(split + 1)
                        if (pwd.isNotEmpty()) _workingDirectory = pwd
                    }
                    onLine(emulator.mScreen.transcriptTextWithFullLinesJoined)
                    break
                }

                val bytes = (line + "\n").toByteArray(Charsets.UTF_8)
                emulator.append(bytes, bytes.size)
                onLine(emulator.mScreen.transcriptTextWithFullLinesJoined)
            }
        } catch (e: IOException) {
            alive.set(false)
            onLine("shell died: ${e.message}")
            return -1
        }

        return exitCode
    }

    actual fun close() {
        alive.set(false)
        try {
            stdin?.let {
                it.write("exit\n")
                it.flush()
                it.close()
            }
        } catch (ignored: IOException) {
        }
        process?.destroy()
    }
}
