package com.hereliesaz.hg2gui.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposerKeysTest {

    @Test
    fun typedCharIsOnlyAFinalSingleAppend() {
        assertEquals('c', ComposerKeys.detectTypedChar("ls ", "ls c"))
        assertEquals('x', ComposerKeys.detectTypedChar("", "x"))
        assertNull(ComposerKeys.detectTypedChar("ls", "ls -la"))
        assertNull(ComposerKeys.detectTypedChar("ls", "l"))
        assertNull(ComposerKeys.detectTypedChar("ls", "lxs"))
    }

    @Test
    fun ctrlBindingsEditTheEndOfTheLine() {
        fun ctrl(key: Char, text: String) = ComposerKeys.apply(ComposerModifier.Ctrl, key, text, null)
        assertEquals(ModifiedKey.Replace(""), ctrl('u', "git status"))
        assertEquals(ModifiedKey.Replace("git "), ctrl('w', "git status"))
        assertEquals(ModifiedKey.Replace("git "), ctrl('w', "git status  "))
        assertEquals(ModifiedKey.Replace(""), ctrl('w', "git"))
        assertEquals(ModifiedKey.Replace("gi"), ctrl('H', "git"))
        assertEquals(ModifiedKey.Interrupt, ctrl('C', "x"))
        assertEquals(ModifiedKey.Run, ctrl('j', "x"))
        assertEquals(ModifiedKey.HistoryPrevious, ctrl('p', ""))
        assertEquals(ModifiedKey.HistoryNext, ctrl('n', ""))
    }

    @Test
    fun ctrlDOnlyEndsInputOnAnEmptyLine() {
        assertEquals(ModifiedKey.EndOfInput, ComposerKeys.apply(ComposerModifier.Ctrl, 'd', "", null))
        assertIs<ModifiedKey.Unsupported>(ComposerKeys.apply(ComposerModifier.Ctrl, 'd', "ls", null))
    }

    @Test
    fun unknownCombinationsAreReportedNotSwallowed() {
        val result = ComposerKeys.apply(ComposerModifier.Ctrl, 'q', "", null)
        assertIs<ModifiedKey.Unsupported>(result)
        assertTrue("CTRL+Q" in result.reason)
        assertIs<ModifiedKey.Unsupported>(ComposerKeys.apply(ComposerModifier.Alt, 'b', "", null))
    }

    @Test
    fun altDotInsertsTheLastArgumentOfThePreviousCommand() {
        assertEquals(
            ModifiedKey.Replace("cat notes.txt"),
            ComposerKeys.apply(ComposerModifier.Alt, '.', "cat", "vim notes.txt")
        )
        assertEquals(
            ModifiedKey.Replace("notes.txt"),
            ComposerKeys.apply(ComposerModifier.Alt, '.', "", "vim notes.txt")
        )
        assertIs<ModifiedKey.Unsupported>(ComposerKeys.apply(ComposerModifier.Alt, '.', "cat", null))
    }

    @Test
    fun altCaseKeysActOnTheLastWord() {
        assertEquals(ModifiedKey.Replace("echo HELLO"), ComposerKeys.apply(ComposerModifier.Alt, 'u', "echo hello", null))
        assertEquals(ModifiedKey.Replace("echo hello"), ComposerKeys.apply(ComposerModifier.Alt, 'l', "echo HELLO", null))
        assertEquals(ModifiedKey.Replace("echo Hello"), ComposerKeys.apply(ComposerModifier.Alt, 'c', "echo hELLO", null))
    }
}
