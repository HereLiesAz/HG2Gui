package com.hereliesaz.hg2gui.managers

import android.content.Context
import android.util.Log
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.ansiHueOf
import com.hereliesaz.hg2gui.ui.files.FilePreview
import com.hereliesaz.hg2gui.util.collectOutput
import java.io.File
import java.io.IOException

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
        // Drains stdout/stderr while waiting - a highlighted file bigger than the pipe buffer
        // would otherwise block the highlighter until the timeout killed it.
        val result = process.collectOutput(HIGHLIGHT_TIMEOUT_MS, MAX_HIGHLIGHT_OUTPUT_BYTES.toInt()) ?: return null
        if (result.exitCode != 0 || result.truncated) return null
        return parseAnsi(result.stdout.toString(Charsets.UTF_8))
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
            // CSI: parameter bytes 0x30-0x3F, intermediates 0x20-0x2F, then one final byte
            // 0x40-0x7E. Only a final 'm' is SGR; every other CSI (cursor moves, erase, ...) is
            // dropped whole instead of being scanned up to some later, unrelated 'm'.
            var end = i + 2
            while (end < line.length && line[end].code in 0x30..0x3F) end++
            val paramsEnd = end
            while (end < line.length && line[end].code in 0x20..0x2F) end++
            if (end >= line.length || line[end].code !in 0x40..0x7E) break // unterminated: drop the rest
            if (line[end] == 'm' && paramsEnd == end) {
                flush()
                applySgrParams(line.substring(i + 2, paramsEnd), onColor = { colorIndex = it }, onBold = { bold = it })
            }
            i = end + 1
        } else {
            text.append(line[i])
            i++
        }
    }
    flush()
    return spans
}

private const val SGR_FG_EXTENDED = 38
private const val SGR_BG_EXTENDED = 48
private const val SGR_EXTENDED_256 = 5
private const val SGR_EXTENDED_RGB = 2
private const val BASE_PALETTE_SIZE = 16

private fun applySgrParams(params: String, onColor: (Int?) -> Unit, onBold: (Boolean) -> Unit) {
    // Empty fields count as 0, per ECMA-48 ("ESC[;1m" is reset-then-bold).
    val codes = if (params.isEmpty()) listOf(SGR_RESET) else params.split(';', ':').map { it.toIntOrNull() ?: 0 }
    var k = 0
    while (k < codes.size) {
        val code = codes[k]
        when {
            code == SGR_RESET -> {
                onColor(null)
                onBold(false)
            }
            code == SGR_FG_EXTENDED || code == SGR_BG_EXTENDED -> {
                // 38;5;N / 38;2;R;G;B (and 48;...): consume the sub-parameters so they are never
                // misread as standalone codes (the 1 in 38;5;1 is not bold). Only a 256-colour
                // index inside the base 16 maps to a named hue; cube/greyscale/truecolor report
                // no mapped colour, and backgrounds are not rendered at all.
                when (codes.getOrNull(k + 1)) {
                    SGR_EXTENDED_256 -> {
                        if (code == SGR_FG_EXTENDED) onColor(codes.getOrNull(k + 2)?.takeIf { it in 0 until BASE_PALETTE_SIZE })
                        k += 2
                    }
                    SGR_EXTENDED_RGB -> {
                        if (code == SGR_FG_EXTENDED) onColor(null)
                        k += 4
                    }
                    else -> k += 1
                }
            }
            code == SGR_BOLD -> onBold(true)
            code == SGR_BOLD_OFF -> onBold(false)
            code == SGR_FG_DEFAULT -> onColor(null)
            code in SGR_FG_DIM_RANGE -> onColor(code - SGR_FG_DIM_RANGE.first)
            code in SGR_FG_BRIGHT_RANGE -> onColor(code - SGR_FG_BRIGHT_RANGE.first + (SGR_FG_DIM_RANGE.last - SGR_FG_DIM_RANGE.first + 1))
        }
        k++
    }
}
