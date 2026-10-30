package com.hereliesaz.hg2gui.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/** What a bounded one-shot child process produced. [stdout] holds at most the requested cap. */
internal class ProcessOutput(val exitCode: Int, val stdout: ByteArray, val truncated: Boolean) {
    /**
     * [stdout] decoded as UTF-8, or null when the process exited with a code outside
     * [acceptedExitCodes]. A probe whose exit code is ignored turns a failed launch (a missing
     * shared library, `fatal: not a git repository`) into rows the caller then parses as data;
     * callers say which non-zero codes are a normal answer (`compgen` exits 1 for "no match").
     */
    fun textIfExit(acceptedExitCodes: Set<Int> = setOf(0)): String? =
        if (exitCode in acceptedExitCodes) stdout.toString(Charsets.UTF_8) else null
}

/**
 * Waits up to [timeoutMs] for this process while draining stdout (and stderr, unless it was
 * merged into stdout) on background threads. Waiting first and reading afterwards deadlocks as
 * soon as the child fills the ~64 KiB pipe buffer: it blocks writing, never exits, and the wait
 * times out on a perfectly healthy command. Returns null on timeout (the child is killed).
 * Bytes past [maxStdoutBytes] are read and discarded so the child can still finish.
 */
internal fun Process.collectOutput(timeoutMs: Long, maxStdoutBytes: Int): ProcessOutput? {
    val buffer = ByteArrayOutputStream()
    var truncated = false
    val stdoutReader = drainThread(inputStream) { chunk, length ->
        synchronized(buffer) {
            val room = maxStdoutBytes - buffer.size()
            if (room > 0) buffer.write(chunk, 0, minOf(room, length))
            if (length > room) truncated = true
        }
    }
    val stderrReader = drainThread(errorStream) { _, _ -> }
    return try {
        if (!waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
            destroyForcibly()
            null
        } else {
            stdoutReader.join(timeoutMs)
            stderrReader.join(timeoutMs)
            synchronized(buffer) { ProcessOutput(exitValue(), buffer.toByteArray(), truncated) }
        }
    } catch (e: InterruptedException) {
        destroyForcibly()
        Thread.currentThread().interrupt()
        null
    }
}

private fun drainThread(stream: InputStream, sink: (ByteArray, Int) -> Unit): Thread =
    Thread {
        try {
            stream.use { input ->
                val chunk = ByteArray(8192)
                while (true) {
                    val read = input.read(chunk)
                    if (read < 0) break
                    sink(chunk, read)
                }
            }
        } catch (_: Exception) {
            // The process was killed or its pipe closed; whatever arrived so far is kept.
        }
    }.apply {
        isDaemon = true
        start()
    }
