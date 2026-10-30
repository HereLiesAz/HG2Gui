package com.hereliesaz.hg2gui.ui

import com.hereliesaz.hg2gui.terminal.ShellFamily
import com.hereliesaz.hg2gui.terminal.ShellPresentation
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs a suspend block that never actually suspends (the fake probes answer immediately). */
private fun <T> runNow(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

class ShellStatusProjectionTest {

    @Test
    fun parsesBranchAndCleanState() {
        val state = ShellStatusProjector.parseGitStatus("## main...origin/main [ahead 1]\n")
        assertEquals(GitState("main", dirty = false), state)
    }

    @Test
    fun anyChangeLineMakesItDirty() {
        val state = ShellStatusProjector.parseGitStatus("## feature/x\n M src/a.kt\n?? new.txt\n")
        assertEquals(GitState("feature/x", dirty = true), state)
    }

    @Test
    fun handlesUnbornAndDetachedHeads() {
        assertEquals("main", ShellStatusProjector.parseGitStatus("## No commits yet on main")?.branch)
        assertEquals("main", ShellStatusProjector.parseGitStatus("## Initial commit on main")?.branch)
        assertEquals(ShellStatusProjector.DETACHED_LABEL, ShellStatusProjector.parseGitStatus("## HEAD (no branch)")?.branch)
    }

    @Test
    fun nonGitOutputIsNoRepository() {
        assertNull(ShellStatusProjector.parseGitStatus(null))
        assertNull(ShellStatusProjector.parseGitStatus(""))
        assertNull(ShellStatusProjector.parseGitStatus("fatal: not a git repository"))
    }

    @Test
    fun projectCarriesIdentityAndReplacesGitAndExit() {
        val previous = ShellPresentation(shell = ShellFamily.BASH, cwd = "/old", user = "me", host = "box", gitBranch = "stale", gitDirty = true)
        val next = runNow {
            ShellStatusProjector.project(previous, "/repo", 2) { cwd, command ->
                assertEquals("/repo", cwd)
                assertEquals(ShellStatusProjector.GIT_STATUS_COMMAND, command)
                "## dev\n M a\n"
            }
        }
        assertEquals("/repo", next.cwd)
        assertEquals("dev", next.gitBranch)
        assertTrue(next.gitDirty)
        assertEquals(2, next.lastExitCode)
        assertEquals("me", next.user)
        assertEquals(ShellFamily.BASH, next.shell)
    }

    @Test
    fun noopProbeClearsStaleGitState() {
        val previous = ShellPresentation(cwd = "/old", gitBranch = "stale", gitDirty = true)
        val next = runNow { ShellStatusProjector.project(previous, "/new", 0, NoShellProbe) }
        assertNull(next.gitBranch)
        assertFalse(next.gitDirty)
        assertEquals("/new", next.cwd)
        assertEquals(0, next.lastExitCode)
    }

    @Test
    fun aThrowingProbeIsTreatedAsNoRepository() {
        val next = runNow {
            ShellStatusProjector.project(ShellPresentation(), "/x", null) { _, _ -> error("boom") }
        }
        assertNull(next.gitBranch)
    }
}
