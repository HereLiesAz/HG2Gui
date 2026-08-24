package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FullScreenCommandsTest {

    @Test
    fun plainOutputCommands_areNotRoutedFullScreen() {
        assertNull(fullScreenCommandOf("ls -la"))
        assertNull(fullScreenCommandOf("cat foo.txt"))
        assertNull(fullScreenCommandOf("git status"))
        assertNull(fullScreenCommandOf("git log --oneline"))
        assertNull(fullScreenCommandOf(""))
        assertNull(fullScreenCommandOf("   "))
    }

    @Test
    fun editorsAndPagersAndMonitors_areRoutedFullScreen() {
        assertEquals("vim", fullScreenCommandOf("vim foo.txt"))
        assertEquals("nano", fullScreenCommandOf("nano  foo.txt"))
        assertEquals("htop", fullScreenCommandOf("htop"))
        assertEquals("tmux", fullScreenCommandOf("tmux new -s work"))
        assertEquals("less", fullScreenCommandOf("less README.md"))
        assertEquals("man", fullScreenCommandOf("man ls"))
        assertEquals("ssh", fullScreenCommandOf("ssh user@host"))
    }

    @Test
    fun interactiveGitRebase_isRoutedFullScreen_butOtherRebasesAreNot() {
        assertEquals("git rebase -i", fullScreenCommandOf("git rebase -i HEAD~3"))
        assertEquals("git rebase -i", fullScreenCommandOf("git rebase --interactive HEAD~3"))
        assertNull(fullScreenCommandOf("git rebase main"))
        assertNull(fullScreenCommandOf("git rebase --onto main topic"))
    }

    @Test
    fun leadingWhitespaceAndExtraSpaces_areTolerated() {
        assertEquals("vim", fullScreenCommandOf("   vim   foo.txt"))
    }
}
