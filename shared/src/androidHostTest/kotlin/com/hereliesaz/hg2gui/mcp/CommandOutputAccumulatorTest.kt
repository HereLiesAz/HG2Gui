package com.hereliesaz.hg2gui.mcp

import org.junit.Assert.assertEquals
import org.junit.Test

/** Both shapes of TerminalEngine's output flow - see CommandOutputAccumulator. */
class CommandOutputAccumulatorTest {

    private fun collect(chunks: List<String>, max: Int = 1000, finalSnapshot: Boolean = false): String {
        val acc = CommandOutputAccumulator(max)
        chunks.forEach { acc.add(it) }
        if (finalSnapshot) acc.markFinalSnapshot()
        return acc.text()
    }

    @Test
    fun cumulativeTranscripts_supersedeEachOther() {
        // A shell command flushes the whole transcript so far, then the whole transcript again.
        assertEquals("a\nb\nc", collect(listOf("a", "a\nb", "a\nb\nc", "a\nb\nc")))
    }

    @Test
    fun incrementalLines_areAppended() {
        // pkg / download / builtins emit one new line per emission.
        assertEquals("Reading lists\nBuilding tree\nDone", collect(listOf("Reading lists", "Building tree", "Done")))
    }

    @Test
    fun aLongerLineThatContainsThePreviousOneLosesNothing() {
        assertEquals("Fetching... done", collect(listOf("Fetching...", "Fetching... done")))
    }

    @Test
    fun finalSnapshot_winsOutright() {
        // Transcript rewrote an earlier line (carriage-return progress bar), so it is not a prefix extension.
        val chunks = listOf("progress 10%", "progress 100%\nfinished")
        assertEquals("progress 10%\nprogress 100%\nfinished", collect(chunks))
        assertEquals("progress 100%\nfinished", collect(chunks, finalSnapshot = true))
    }

    @Test
    fun emptyFlow_isEmpty() {
        assertEquals("", collect(emptyList()))
    }

    @Test
    fun outputIsCapped_keepingTheHead() {
        assertEquals("abcde", collect(listOf("abcdefghij"), max = 5))
        // Appended lines stop once the cap is reached; the head is what is kept.
        assertEquals("abc\nd", collect(listOf("abc", "def", "ghi"), max = 5))
        assertEquals("abcde", collect(listOf("x", "abcdefghij"), max = 5, finalSnapshot = true))
    }
}
