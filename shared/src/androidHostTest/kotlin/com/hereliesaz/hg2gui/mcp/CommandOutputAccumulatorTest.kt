package com.hereliesaz.hg2gui.mcp

import org.junit.Assert.assertEquals
import org.junit.Test

/** Both shapes of TerminalEngine's output flow - see CommandOutputAccumulator. */
class CommandOutputAccumulatorTest {

    private fun collect(max: Int = 1000, finalSnapshot: Boolean = false, vararg chunks: String): String {
        val acc = CommandOutputAccumulator(max)
        chunks.forEach(acc::add)
        if (finalSnapshot) acc.markFinalSnapshot()
        return acc.text()
    }

    @Test
    fun cumulativeTranscripts_supersedeEachOther() {
        // A shell command flushes the whole transcript so far, then the whole transcript again.
        assertEquals("a\nb\nc", collect(chunks = arrayOf("a", "a\nb", "a\nb\nc", "a\nb\nc")))
    }

    @Test
    fun incrementalLines_areAppended() {
        // pkg / download / builtins emit one new line per emission.
        assertEquals("Reading lists\nBuilding tree\nDone", collect(chunks = arrayOf("Reading lists", "Building tree", "Done")))
    }

    @Test
    fun aLongerLineThatContainsThePreviousOneLosesNothing() {
        assertEquals("Fetching... done", collect(chunks = arrayOf("Fetching...", "Fetching... done")))
    }

    @Test
    fun finalSnapshot_winsOutright() {
        // Transcript rewrote an earlier line (carriage-return progress bar), so it is not a prefix extension.
        val chunks = arrayOf("progress 10%", "progress 100%\nfinished")
        assertEquals("progress 10%\nprogress 100%\nfinished", collect(chunks = chunks))
        assertEquals("progress 100%\nfinished", collect(finalSnapshot = true, chunks = chunks))
    }

    @Test
    fun emptyFlow_isEmpty() {
        assertEquals("", collect(chunks = emptyArray()))
    }

    @Test
    fun outputIsCapped_keepingTheHead() {
        assertEquals("abcde", collect(max = 5, chunks = arrayOf("abcdefghij")))
        assertEquals("abcde", collect(max = 5, chunks = arrayOf("abc", "def", "ghi")))
        assertEquals("abcde", collect(max = 5, finalSnapshot = true, chunks = arrayOf("x", "abcdefghij")))
    }
}
