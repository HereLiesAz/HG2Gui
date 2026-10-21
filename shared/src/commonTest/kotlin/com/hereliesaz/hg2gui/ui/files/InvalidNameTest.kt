package com.hereliesaz.hg2gui.ui.files

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InvalidNameTest {
    @Test
    fun slashesAndDotNamesAreRejected() {
        assertNotNull(invalidNameMessage("a/b"))
        assertNotNull(invalidNameMessage("/"))
        assertNotNull(invalidNameMessage("."))
        assertNotNull(invalidNameMessage(".."))
    }

    @Test
    fun ordinaryNamesPass() {
        assertNull(invalidNameMessage("notes.txt"))
        assertNull(invalidNameMessage(".bashrc"))
        assertNull(invalidNameMessage("a..b"))
        assertNull(invalidNameMessage(""))
    }
}
