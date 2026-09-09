package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TuiSemanticModelTest {

    @Test
    fun layeredPythonStyleMenu_isRecognized() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Settings"),
                TuiRow(1, "> General", highlighted = true),
                TuiRow(2, "  Appearance"),
                TuiRow(3, "  Accounts"),
                TuiRow(5, "Profile"),
                TuiRow(6, "[ ] Personal"),
                TuiRow(7, "[x] Work")
            ),
            alternateScreen = true
        )

        assertTrue(snapshot.isWrappable)
        assertEquals("Settings", snapshot.title)
        assertEquals(2, snapshot.layers.size)
        assertEquals(listOf("General", "Appearance", "Accounts"), snapshot.layers[0].items.map { it.label })
        assertEquals(0, snapshot.layers[0].activeIndex)
        assertEquals(listOf("Personal", "Work"), snapshot.layers[1].items.map { it.label })
    }

    @Test
    fun highlightedPlainRows_canFormMenuWithoutGlyphs() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Choose model"),
                TuiRow(1, "Claude Sonnet", highlighted = true),
                TuiRow(2, "Claude Opus", highlighted = true)
            ),
            alternateScreen = true
        )

        assertTrue(snapshot.isWrappable)
        assertEquals(1, snapshot.layers.size)
    }

    @Test
    fun passwordPrompt_isTypedAndWrappable() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Authentication"),
                TuiRow(4, "API key: ", cursorColumn = 9)
            ),
            alternateScreen = true
        )

        assertTrue(snapshot.isWrappable)
        val prompt = assertNotNull(snapshot.prompt)
        assertEquals(TuiPromptKind.PASSWORD, prompt.kind)
    }

    @Test
    fun sameRowsOutsideAlternateScreen_doNotForceWrapper() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Menu"),
                TuiRow(1, "> One", highlighted = true),
                TuiRow(2, "  Two")
            ),
            alternateScreen = false
        )

        assertFalse(snapshot.isWrappable)
    }
}
