package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellPresentationTest {
    @Test
    fun powerlineStylePrompt_extractsPathBranchAndDirtyState() {
        val parsed = ShellPromptParser.parse(
            "az@pixel ~/src/hg2gui  main ✗ ❯",
            shell = ShellFamily.ZSH,
            framework = PromptFramework.POWERLEVEL10K
        )
        assertEquals(ShellFamily.ZSH, parsed.shell)
        assertEquals(PromptFramework.POWERLEVEL10K, parsed.framework)
        assertEquals("az", parsed.user)
        assertEquals("pixel", parsed.host)
        assertEquals("~/src/hg2gui", parsed.cwd)
        assertEquals("main", parsed.gitBranch)
        assertTrue(parsed.gitDirty)
        assertEquals(ShellCompletionProvider.ZSH_COMPLETION, parsed.completionProvider)
    }

    @Test
    fun simplePrompt_usesFallbackCwdWithoutInventingGitState() {
        val parsed = ShellPromptParser.parse(
            "$ ",
            shell = ShellFamily.BASH,
            fallbackCwd = "/data/data/example/files/home"
        )
        assertEquals("/data/data/example/files/home", parsed.cwd)
        assertEquals(null, parsed.gitBranch)
        assertFalse(parsed.gitDirty)
    }
}
