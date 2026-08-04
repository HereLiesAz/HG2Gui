package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

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

        fun forAndroid(home: File?, context: Context): ShellSession {
            val zsh = File(context.applicationInfo.nativeLibraryDir, ZSH_LIB_NAME)
            if (zsh.canExecute()) {
                val session = ShellSession(
                    home, arrayOf(zsh.absolutePath, "-f"), zsh.parent
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

            sin.write(command)
            sin.write("\n")
            sin.write("printf '%s%d:%s\\n' \"$SENTINEL\" \"$?\" \"\$PWD\"\n")
            sin.flush()

            val deadline = System.currentTimeMillis() + TIMEOUT_MS

            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    onLine("\n[timed out after ${TIMEOUT_MS / 1000}s]")
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
                    if (marker > 0) onLine(line.substring(0, marker))

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
                    break
                }

                onLine(line)
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
