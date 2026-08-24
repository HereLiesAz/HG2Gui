package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.util.Log
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.ansiHueOf
import com.hereliesaz.hg2gui.ui.files.FilePreview
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/*
 * F3: "syntax highlighting from whichever linters the user has chosen to install through Termux"
 * - a pluggable highlighter that reports what's actually present in $PREFIX/bin rather than
 * assuming any one of them is there. Shells out to whichever candidate is found (never through
 * the interactive PTY session - this is a one-shot, non-interactive read, same tier ShellSession's
 * own startPipe uses for a bootstrap command) and reuses StyledTranscript.kt's own ANSI-to-hue
 * mapping (ansiHueOf) to turn its coloured stdout into the same StyledSpan shape the terminal's
 * own output already renders with.
 */

private const val MAX_PREVIEW_BYTES = 2L * 1024 * 1024
private const val MAX_HIGHLIGHT_OUTPUT_BYTES = 2L * 1024 * 1024
private const val HIGHLIGHT_TIMEOUT_MS = 4000L
private val MARKDOWN_EXT = setOf("md", "markdown")

/** Reads and (where possible) highlights [path] for the file explorer's own in-place preview -
 *  the whole point being that a tapped file says what it is rather than appearing inert. Runs
 *  file I/O and, when available, an external process - call this off the main thread. */
fun previewFile(context: Context, path: String): FilePreview {
    val file = VfsManager.resolve(context, path)
    return when {
        file == null || !file.isFile -> FilePreview(text = null)
        file.length() > MAX_PREVIEW_BYTES -> FilePreview(text = null, truncated = true)
        else -> readPreview(context, file)
    }
}

private fun readPreview(context: Context, file: File): FilePreview {
    val bytes = file.readBytes()
    if (looksBinary(bytes)) return FilePreview(text = null)
    val text = bytes.toString(Charsets.UTF_8)
    val isMarkdown = file.extension.lowercase() in MARKDOWN_EXT
    val styled = if (isMarkdown) emptyList() else FileHighlighter.highlight(context, file).orEmpty()
    return FilePreview(text = text, styled = styled, isMarkdown = isMarkdown)
}

private const val BINARY_SAMPLE_BYTES = 8000

/** A sampled null-byte check, the same heuristic `file`/git use to call something binary - real
 *  text (including UTF-8 multi-byte sequences) never contains a literal 0x00. */
internal fun looksBinary(bytes: ByteArray): Boolean {
    val sampleSize = minOf(bytes.size, BINARY_SAMPLE_BYTES)
    for (i in 0 until sampleSize) {
        if (bytes[i] == 0.toByte()) return true
    }
    return false
}

private object FileHighlighter {
    private data class Candidate(val bin: String, val args: (File) -> List<String>)

    // Preference order: bat's the most commonly installed pretty-printer in a Termux pkg install,
    // falling back to progressively less common ones - the first one actually present wins.
    private val CANDIDATES = listOf(
        Candidate("bat") { listOf("--color=always", "--paging=never", "--style=plain", it.absolutePath) },
        Candidate("batcat") { listOf("--color=always", "--paging=never", "--style=plain", it.absolutePath) },
        Candidate("highlight") { listOf("-O", "ansi", it.absolutePath) },
        Candidate("pygmentize") { listOf("-f", "terminal256", it.absolutePath) }
    )

    private const val TAG = "FileHighlighter"

    fun highlight(context: Context, file: File): List<List<StyledSpan>>? {
        val prefix = DistroManager.prefixDir(context)
        val binDir = File(prefix, "bin")
        val candidate = CANDIDATES.firstOrNull { File(binDir, it.bin).canExecute() }
        if (candidate == null) return null
        return try {
            runCandidate(prefix, binDir, candidate, file)
        } catch (e: IOException) {
            Log.w(TAG, "highlight via ${candidate.bin} failed", e)
            null
        } catch (e: InterruptedException) {
            Log.w(TAG, "highlight via ${candidate.bin} interrupted", e)
            null
        }
    }

    private fun runCandidate(prefix: File, binDir: File, candidate: Candidate, file: File): List<List<StyledSpan>>? {
        val command = listOf(File(binDir, candidate.bin).absolutePath) + candidate.args(file)
        val builder = ProcessBuilder(command)
        // Same env shape ShellSession.forAndroid() builds for a real bootstrap command - a
        // Termux-installed binary won't resolve its own shared libraries or subprocesses without it.
        builder.environment()["PREFIX"] = prefix.absolutePath
        builder.environment()["PATH"] = "${prefix.absolutePath}/bin"
        builder.environment()["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib"
        val process = builder.start()
        val finished = process.waitFor(HIGHLIGHT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        val out = if (finished && process.exitValue() == 0) process.inputStream.readBytes() else null
        return out?.takeIf { it.size.toLong() <= MAX_HIGHLIGHT_OUTPUT_BYTES }?.let { parseAnsi(it.toString(Charsets.UTF_8)) }
    }
}

internal fun parseAnsi(raw: String): List<List<StyledSpan>> = raw.split("\r\n", "\n").map(::parseAnsiLine)

private const val SGR_RESET = 0
private const val SGR_BOLD = 1
private const val SGR_BOLD_OFF = 22
private const val SGR_FG_DEFAULT = 39
private const val SGR_FG_DIM_START = 30
private const val SGR_FG_DIM_END = 37
private const val SGR_FG_BRIGHT_START = 90
private const val SGR_FG_BRIGHT_END = 97
private val SGR_FG_DIM_RANGE = SGR_FG_DIM_START..SGR_FG_DIM_END
private val SGR_FG_BRIGHT_RANGE = SGR_FG_BRIGHT_START..SGR_FG_BRIGHT_END

internal fun parseAnsiLine(line: String): List<StyledSpan> {
    val spans = mutableListOf<StyledSpan>()
    var colorIndex: Int? = null
    var bold = false
    val text = StringBuilder()
    fun flush() {
        if (text.isNotEmpty()) {
            spans += StyledSpan(text.toString(), colorIndex?.let(::ansiHueOf), bold)
            text.clear()
        }
    }
    var i = 0
    while (i < line.length) {
        if (line[i] == '\u001B' && i + 1 < line.length && line[i + 1] == '[') {
            val end = line.indexOf('m', i)
            if (end < 0) break
            flush()
            applySgrParams(line.substring(i + 2, end), onColor = { colorIndex = it }, onBold = { bold = it })
            i = end + 1
        } else {
            text.append(line[i])
            i++
        }
    }
    flush()
    return spans
}

private fun applySgrParams(params: String, onColor: (Int?) -> Unit, onBold: (Boolean) -> Unit) {
    val codes = params.split(';').mapNotNull { it.toIntOrNull() }
    if (codes.isEmpty() || codes.contains(SGR_RESET)) {
        onColor(null)
        onBold(false)
    }
    for (code in codes) {
        when {
            code == SGR_BOLD -> onBold(true)
            code == SGR_BOLD_OFF -> onBold(false)
            code == SGR_FG_DEFAULT -> onColor(null)
            code in SGR_FG_DIM_RANGE -> onColor(code - SGR_FG_DIM_RANGE.first)
            code in SGR_FG_BRIGHT_RANGE -> onColor(code - SGR_FG_BRIGHT_RANGE.first + (SGR_FG_DIM_RANGE.last - SGR_FG_DIM_RANGE.first + 1))
        }
    }
}
