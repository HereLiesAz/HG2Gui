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
    extraEnv: Map<String, String>
) {

    companion object {
        private const val SENTINEL = "__HG2GUI_EOC_a7f3__"
        private const val DEFAULT_SHELL = "/system/bin/sh"
        private const val ZSH_LIB_NAME = "libzsh.so"
        private const val TIMEOUT_MS = 15_000L
        private const val STARTUP_PROBE_MS = 300L
        private const val PROMPT_IDLE_MS = 400L

        private fun setupZshrc(home: File?) {
            if (home == null) return
            try {
                if (!home.exists()) home.mkdirs()
                val zshrc = File(home, ".zshrc")
                if (!zshrc.exists()) {
                    zshrc.writeText(
                        "# --- Completion ---\n" +
                        "autoload -Uz compinit\n" +
                        "compinit -C\n" +
                        "zstyle ':completion:*' menu select\n" +
                        "\n" +
                        "# --- History search on the arrow keys, prefix-aware ---\n" +
                        "autoload -Uz up-line-or-beginning-search down-line-or-beginning-search\n" +
                        "zle -N up-line-or-beginning-search\n" +
                        "zle -N down-line-or-beginning-search\n" +
                        "bindkey '^[[A' up-line-or-beginning-search\n" +
                        "bindkey '^[[B' down-line-or-beginning-search\n" +
                        "\n" +
                        "# --- command-not-found hints ---\n" +
                        "command_not_found_handler() {\n" +
                        "  local cmd=\$1 best=\"\" bestlen=999\n" +
                        "  for c in \${(k)commands} \${(k)aliases}; do\n" +
                        "    if [[ \$c == \${cmd}* || \$cmd == \${c}* ]]; then\n" +
                        "      local len=\${#c}\n" +
                        "      (( len < bestlen )) && { best=\$c; bestlen=\$len }\n" +
                        "    fi\n" +
                        "  done\n" +
                        "  if [[ -n \$best ]]; then\n" +
                        "    print -u2 \"zsh: command not found: \$cmd (did you mean: \$best?)\"\n" +
                        "  else\n" +
                        "    print -u2 \"zsh: command not found: \$cmd\"\n" +
                        "  fi\n" +
                        "  return 127\n" +
                        "}\n" +
                        "\n" +
                        "# --- Autosuggestions ---\n" +
                        "_hg2gui_suggest() {\n" +
                        "  POSTDISPLAY=\"\"\n" +
                        "  [[ -z \$BUFFER ]] && return\n" +
                        "  local i\n" +
                        "  for (( i = HISTCMD; i >= 1; i-- )); do\n" +
                        "    local h=\${history[\$i]}\n" +
                        "    if [[ -n \$h && \$h == \${BUFFER}* && \$h != \$BUFFER ]]; then\n" +
                        "      POSTDISPLAY=\${h#\$BUFFER}\n" +
                        "      return\n" +
                        "    fi\n" +
                        "  done\n" +
                        "}\n" +
                        "_hg2gui_suggest_widget() {\n" +
                        "  zle .\$WIDGET\n" +
                        "  _hg2gui_suggest\n" +
                        "}\n" +
                        "for w in self-insert backward-delete-char delete-char backward-kill-word; do\n" +
                        "  zle -N \$w _hg2gui_suggest_widget\n" +
                        "done\n" +
                        "_hg2gui_accept_suggestion() {\n" +
                        "  if [[ -n \$POSTDISPLAY && \$CURSOR -eq \${#BUFFER} ]]; then\n" +
                        "    BUFFER+=\"\$POSTDISPLAY\"\n" +
                        "    POSTDISPLAY=\"\"\n" +
                        "    CURSOR=\${#BUFFER}\n" +
                        "  else\n" +
                        "    zle forward-char\n" +
                        "  fi\n" +
                        "}\n" +
                        "zle -N _hg2gui_accept_suggestion\n" +
                        "bindkey '^[[C' _hg2gui_accept_suggestion\n" +
                        "\n" +
                        "# --- \"you should use\" style alias hints ---\n" +
                        "_hg2gui_alias_hint() {\n" +
                        "  local ran=\"\$1\" name expansion\n" +
                        "  for name in \${(k)aliases}; do\n" +
                        "    expansion=\${aliases[\$name]}\n" +
                        "    if [[ -n \$expansion && \"\$ran\" == \"\$expansion\"* && \"\$ran\" != \"\$name\"* ]]; then\n" +
                        "      print -u2 \"hint: alias \$name='\$expansion'\"\n" +
                        "      return\n" +
                        "    fi\n" +
                        "  done\n" +
                        "}\n" +
                        "autoload -Uz add-zsh-hook\n" +
                        "add-zsh-hook preexec _hg2gui_alias_hint\n" +
                        "\n" +
                        "if command -v bat >/dev/null 2>&1; then\n" +
                        "  alias cat='bat --paging=never'\n" +
                        "fi\n" +
                        "if command -v fzf >/dev/null 2>&1; then\n" +
                        "  eval \"\$(fzf --zsh 2>/dev/null)\"\n" +
                        "fi\n" +
                        "\n" +
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
                val env = buildMap {
                    put("LD_LIBRARY_PATH", zsh.parent.orEmpty())
                    if (home != null) put("HOME", home.absolutePath)
                }
                val session = ShellSession(home, arrayOf(zsh.absolutePath), env)
                if (session.survivedStartup()) return session
                session.close()
            }

            if (DistroManager.isInstalled(context)) {
                val prefix = DistroManager.prefixDir(context)
                val bash = File(prefix, "bin/bash")
                if (bash.canExecute()) {
                    val bootstrapHome = home ?: DistroManager.homeDir(context)
                    if (!bootstrapHome.exists()) bootstrapHome.mkdirs()
                    val env = mapOf(
                        "HOME" to bootstrapHome.absolutePath,
                        "PREFIX" to prefix.absolutePath,
                        "PATH" to "${prefix.absolutePath}/bin",
                        "LD_LIBRARY_PATH" to "${prefix.absolutePath}/lib",
                        "TMPDIR" to "${prefix.absolutePath}/tmp",
                        "LANG" to "en_US.UTF-8"
                    )
                    val session = ShellSession(bootstrapHome, arrayOf(bash.absolutePath, "-l"), env)
                    if (session.survivedStartup()) return session
                    session.close()
                }
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

    constructor(home: File?) : this(home, arrayOf(DEFAULT_SHELL), emptyMap())

    constructor(home: File?, shellPath: String) : this(home, arrayOf(shellPath), emptyMap())

    actual constructor(homePath: String?) : this(homePath?.let { File(it) })

    actual constructor(homePath: String?, shellPath: String) : this(homePath?.let { File(it) }, shellPath)

    init {
        try {
            val builder = ProcessBuilder(*command)
            builder.redirectErrorStream(true)
            if (home != null && home.isDirectory) builder.directory(home)
            if (extraEnv.isNotEmpty()) {
                builder.environment().putAll(extraEnv)
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
        var output = ""
        val exitCode = stream(command, onLine = { line -> output = line }, onNeedInput = { null })
        return ShellSessionResult(output, exitCode, _workingDirectory)
    }

    actual fun stream(command: String, onLine: (line: String) -> Unit, onNeedInput: (prompt: String) -> String?): Int {
        if (!isAlive) {
            onLine("shell is not running")
            return -1
        }

        var exitCode = -1

        try {
            val sin = stdin ?: throw IOException("stdin is null")
            val sout = stdout ?: throw IOException("stdout is null")

            val emulator = TerminalEmulator(DummyTerminalOutput(), 120, 24, 10, 10, 1000, null)

            sin.write(command)
            sin.write("\n")
            sin.write("printf '%s%d:%s\\n' \"$SENTINEL\" \"$?\" \"\$PWD\"\n")
            sin.flush()

            val pending = StringBuilder()
            var emittedUpTo = 0
            var deadline = System.currentTimeMillis() + TIMEOUT_MS
            var lastDataAt = System.currentTimeMillis()
            var promptOfferedForThisStall = false
            val buf = CharArray(4096)

            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    val bytes = "\r\n[timed out after ${TIMEOUT_MS / 1000}s]".toByteArray()
                    emulator.append(bytes, bytes.size)
                    onLine(emulator.mScreen.transcriptTextWithFullLinesJoined)
                    break
                }

                if (sout.ready()) {
                    val n = sout.read(buf)
                    if (n == -1) {
                        alive.set(false)
                        break
                    }
                    if (n > 0) {
                        pending.append(buf, 0, n)
                        lastDataAt = System.currentTimeMillis()
                        promptOfferedForThisStall = false
                    }
                } else {
                    val marker = pending.indexOf(SENTINEL, emittedUpTo)
                    val idleMs = System.currentTimeMillis() - lastDataAt
                    val tailIsUnterminated = pending.length > emittedUpTo &&
                        pending[pending.length - 1] != '\n' && pending[pending.length - 1] != '\r'

                    if (!promptOfferedForThisStall && marker < 0 && tailIsUnterminated && idleMs > PROMPT_IDLE_MS) {
                        promptOfferedForThisStall = true
                        emittedUpTo = flush(pending, emittedUpTo, pending.length, emulator, onLine)
                        val answer = onNeedInput(pending.substring(0, pending.length))
                        if (answer != null) {
                            sin.write(answer)
                            sin.write("\n")
                            sin.flush()
                            lastDataAt = System.currentTimeMillis()
                            deadline = System.currentTimeMillis() + TIMEOUT_MS
                        }
                    }

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

                val marker = pending.indexOf(SENTINEL, emittedUpTo)
                if (marker < 0) {
                    val safeEnd = (pending.length - SENTINEL.length).coerceAtLeast(emittedUpTo)
                    emittedUpTo = flush(pending, emittedUpTo, safeEnd, emulator, onLine)
                    continue
                }

                val newlineIdx = pending.indexOf("\n", marker + SENTINEL.length)
                if (newlineIdx < 0) continue

                emittedUpTo = flush(pending, emittedUpTo, marker, emulator, onLine)
                val tail = pending.substring(marker + SENTINEL.length, newlineIdx).trimEnd('\r')
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
        } catch (e: IOException) {
            alive.set(false)
            onLine("shell died: ${e.message}")
            return -1
        }

        return exitCode
    }

    private fun flush(
        pending: StringBuilder,
        from: Int,
        to: Int,
        emulator: TerminalEmulator,
        onLine: (String) -> Unit
    ): Int {
        if (to <= from) return from
        val chunk = pending.substring(from, to)
        val bytes = chunk.replace("\r\n", "\n").replace("\n", "\r\n").toByteArray(Charsets.UTF_8)
        emulator.append(bytes, bytes.size)
        onLine(emulator.mScreen.transcriptTextWithFullLinesJoined)
        return to
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
