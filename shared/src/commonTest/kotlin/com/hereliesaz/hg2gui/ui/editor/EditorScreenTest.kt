package com.hereliesaz.hg2gui.ui.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorScreenTest {

    @Test
    fun codeAndConfigFilesUseMonospace() {
        listOf("run.sh", "nginx.conf", "settings.INI", "AndroidManifest.xml", ".bashrc", "Makefile", "app.log")
            .forEach { assertTrue(editorUsesMonospace(it), it) }
    }

    @Test
    fun proseFilesKeepTheProportionalFace() {
        listOf("notes.txt", "README.md", "todo.TXT", "draft.markdown").forEach { assertFalse(editorUsesMonospace(it), it) }
    }

    @Test
    fun unwrappedWidthIsTheLongestLinePlusRoom() {
        // Longest line is 10 columns; 4 columns of trailing room for the caret.
        assertEquals(14, unwrappedColumns("short\n0123456789\nmid", minColumns = 5))
    }

    @Test
    fun unwrappedWidthNeverDropsBelowTheViewport() {
        assertEquals(40, unwrappedColumns("a\nb", minColumns = 40))
        assertEquals(40, unwrappedColumns("", minColumns = 40))
    }

    @Test
    fun aFinalLineWithoutNewlineIsMeasured() {
        assertEquals(24, unwrappedColumns("x\n" + "y".repeat(20), minColumns = 1))
    }
}
