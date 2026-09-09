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
        assertEquals(TuiControlKind.CHECKBOX, snapshot.layers[1].items[0].control)
        assertFalse(snapshot.layers[1].items[0].checked ?: true)
        assertTrue(snapshot.layers[1].items[1].checked == true)
    }

    @Test
    fun indentedLaterMenu_isModeledAsTopModalLayer() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Settings"),
                TuiRow(1, "> General", highlighted = true),
                TuiRow(2, "  Accounts"),
                TuiRow(5, "    Confirm action"),
                TuiRow(6, "    > Continue", highlighted = true),
                TuiRow(7, "      Cancel")
            ),
            alternateScreen = true
        )

        assertEquals(2, snapshot.layers.size)
        assertEquals(0, snapshot.layers[0].depth)
        assertFalse(snapshot.layers[0].modal)
        assertEquals(1, snapshot.layers[1].depth)
        assertTrue(snapshot.layers[1].modal)
        assertEquals(1, snapshot.activeLayerIndex)
    }

    @Test
    fun emphasizedRows_canExposeSelectionWithoutInverseVideo() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Choose model"),
                TuiRow(1, "  Claude Sonnet", emphasized = true),
                TuiRow(2, "  Claude Opus")
            ),
            alternateScreen = true
        )
        assertTrue(snapshot.isWrappable)
        assertEquals(0, snapshot.layers.single().activeIndex)
    }

    @Test
    fun radioControls_areTypedAndChecked() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "Mode"),
                TuiRow(1, "( ) Safe", highlighted = true),
                TuiRow(2, "(*) Fast")
            ),
            alternateScreen = true
        )
        assertEquals(TuiControlKind.RADIO, snapshot.layers.single().items[0].control)
        assertFalse(snapshot.layers.single().items[0].checked ?: true)
        assertTrue(snapshot.layers.single().items[1].checked == true)
    }

    @Test
    fun tabsProgressTableAndPanes_areProjected() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(
                TuiRow(0, "[Chat] | Files | Settings"),
                TuiRow(2, "Downloading 42%"),
                TuiRow(4, "| NAME | VALUE |"),
                TuiRow(5, "| foo  | bar   |"),
                TuiRow(7, "left │ right"),
                TuiRow(8, "more │ detail")
            ),
            alternateScreen = true,
            mouseAware = true
        )
        assertTrue(snapshot.tabs.size >= 2)
        assertTrue(snapshot.regions.any { it.kind == TuiRegionKind.PROGRESS && it.progress == .42f })
        assertTrue(snapshot.regions.any { it.kind == TuiRegionKind.TABLE })
        assertTrue(snapshot.regions.any { it.kind == TuiRegionKind.PANE })
        assertTrue(snapshot.mouseAware)
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
    fun confirmationPrompt_isTyped() {
        val snapshot = TuiSemanticParser.parse(
            rows = listOf(TuiRow(0, "Delete this item? [y/n]", cursorColumn = 24)),
            alternateScreen = true
        )
        assertEquals(TuiPromptKind.CONFIRMATION, assertNotNull(snapshot.prompt).kind)
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
