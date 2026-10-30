package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ShellAliasesExpandTest {

    @Test
    fun expandsALeadingAliasWord() {
        assertEquals("git status", ShellAliases.expand("gs", isRealCommand = { false }))
        assertEquals("git commit -m \"msg\"", ShellAliases.expand("gcm \"msg\"", isRealCommand = { false }))
        assertEquals("ls -la /sdcard", ShellAliases.expand("ll /sdcard", isRealCommand = { false }))
        assertEquals("cd ..", ShellAliases.expand("..", isRealCommand = { false }))
    }

    @Test
    fun leavesNonAliasesAlone() {
        for (line in listOf("git status", "echo gs", "ls gs", "gsx", "")) {
            assertEquals(line, ShellAliases.expand(line, isRealCommand = { false }))
        }
    }

    @Test
    fun aRealInstalledCommandWinsOverTheAlias() {
        // n-2: `gs` is Ghostscript once it is installed; `c`, `h`, `l`, `gb`, `gd` can be real tools too.
        val installed = setOf("gs", "l", "c")
        for (alias in installed) {
            assertEquals("$alias file.pdf", ShellAliases.expand("$alias file.pdf", isRealCommand = { it in installed }))
        }
        // Aliases that nothing installed shadows still expand.
        assertEquals("git diff", ShellAliases.expand("gd", isRealCommand = { it in installed }))
    }

    @Test
    fun defaultProbeComesFromTheInstalledPlatformHook() {
        val previous = ShellAliases.realCommandProbe
        try {
            ShellAliases.realCommandProbe = { it == "gs" }
            assertEquals("gs x.pdf", ShellAliases.expand("gs x.pdf"))
            assertEquals("git add", ShellAliases.expand("ga"))
        } finally {
            ShellAliases.realCommandProbe = previous
        }
    }
}
