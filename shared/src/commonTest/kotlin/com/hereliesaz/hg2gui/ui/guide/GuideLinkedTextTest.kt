package com.hereliesaz.hg2gui.ui.guide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GuideLinkedTextTest {

    private fun GuideCommandHit.slice(text: String) = text.substring(start, end)

    @Test
    fun findsEachCommandInTextOrder() {
        val text = "Use chown after chmod."
        val hits = findCommandHits(text, setOf("chmod", "chown"))
        assertEquals(listOf("chown", "chmod"), hits.map { it.command })
        assertEquals(listOf("chown", "chmod"), hits.map { it.slice(text) })
    }

    @Test
    fun longestCommandWinsAndHitsNeverOverlap() {
        val text = "Run git commit now, git alone is not enough."
        val hits = findCommandHits(text, setOf("git", "git commit"))
        assertEquals(listOf("git commit", "git"), hits.map { it.command })
        for (i in 1 until hits.size) assertTrue(hits[i - 1].end <= hits[i].start)
    }

    @Test
    fun ignoresNamesInsideLongerWords() {
        val text = "lsof, ls-files, my.ls and ls_old are not links, but ls is."
        val hits = findCommandHits(text, setOf("ls"))
        assertEquals(1, hits.size)
        assertEquals(text.lastIndexOf("ls is"), hits.single().start)
    }

    @Test
    fun blankAndEmptyInputsYieldNothing() {
        assertTrue(findCommandHits("", setOf("ls")).isEmpty())
        assertTrue(findCommandHits("ls", setOf("", "  ")).isEmpty())
    }
}
