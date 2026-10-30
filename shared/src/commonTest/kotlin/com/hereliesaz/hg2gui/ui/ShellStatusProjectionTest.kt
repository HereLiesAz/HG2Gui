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

    @Test
    fun parsesIdentityFromTwoLines() {
        assertEquals(ShellIdentity("u0_a123", "localhost"), ShellStatusProjector.parseIdentity("u0_a123\nlocalhost\n"))
        assertEquals(ShellIdentity("me", "box"), ShellStatusProjector.parseIdentity("\n me \r\n box \nextra"))
    }

    @Test
    fun incompleteOrMalformedIdentityIsUnknown() {
        assertNull(ShellStatusProjector.parseIdentity(null))
        assertNull(ShellStatusProjector.parseIdentity(""))
        assertNull(ShellStatusProjector.parseIdentity("onlyuser\n"))
        assertNull(ShellStatusProjector.parseIdentity("two words\nhost"))
        assertNull(ShellStatusProjector.parseIdentity("user\n" + "h".repeat(65)))
    }

    @Test
    fun formatIdentityRoundTripsAndRejectsUnusableParts() {
        val text = ShellStatusProjector.formatIdentity("me", "box")
        assertEquals("me\nbox", text)
        assertEquals(ShellIdentity("me", "box"), ShellStatusProjector.parseIdentity(text))
        assertNull(ShellStatusProjector.formatIdentity(null, "box"))
        assertNull(ShellStatusProjector.formatIdentity("me", " "))
        assertNull(ShellStatusProjector.formatIdentity("me", "a b"))
    }

    @Test
    fun probeIdentityAsksTheIdentityQuestionOnly() {
        var asked: String? = null
        val identity = runNow {
            ShellStatusProjector.probeIdentity("/x") { _, command ->
                asked = command
                "me\nbox"
            }
        }
        assertEquals(ShellStatusProjector.IDENTITY_COMMAND, asked)
        assertEquals(ShellIdentity("me", "box"), identity)
    }

    @Test
    fun probeIdentityIsNullWhenTheProbeCannotAnswerOrThrows() {
        assertNull(runNow { ShellStatusProjector.probeIdentity("/x", NoShellProbe) })
        assertNull(runNow { ShellStatusProjector.probeIdentity("/x") { _, _ -> error("boom") } })
    }

    @Test
    fun androidAppUidNamesMatchIdOutput() {
        assertEquals("u0_a123", ShellIdentitySource.androidAppUserName(10123))
        assertEquals("u0_a0", ShellIdentitySource.androidAppUserName(10000))
        assertEquals("u10_a45", ShellIdentitySource.androidAppUserName(1_010_045))
        assertNull(ShellIdentitySource.androidAppUserName(0))
        assertNull(ShellIdentitySource.androidAppUserName(2000))
        assertNull(ShellIdentitySource.androidAppUserName(99_000))
        assertNull(ShellIdentitySource.androidAppUserName(-1))
    }

    @Test
    fun userPrefersTheAppUidNameThenEnvironmentThenProperty() {
        assertEquals("u0_a7", ShellIdentitySource.pickUser(10007, "env", "prop"))
        assertEquals("env", ShellIdentitySource.pickUser(2000, "env", "prop"))
        assertEquals("prop", ShellIdentitySource.pickUser(2000, " ", "prop"))
        assertNull(ShellIdentitySource.pickUser(2000, null, ""))
    }

    @Test
    fun hostPrefersHostnameThenEnvironmentThenDeviceModel() {
        assertEquals("localhost", ShellIdentitySource.pickHost("localhost\n", "env", "Pixel 7"))
        assertEquals("env", ShellIdentitySource.pickHost("  \n", "env", "Pixel 7"))
        assertEquals("Pixel-7", ShellIdentitySource.pickHost(null, null, "Pixel 7"))
        assertNull(ShellIdentitySource.pickHost(null, "", null))
    }

    @Test
    fun sessionAsksForIdentityOnceAndKeepsAnswerAcrossRefreshes() {
        val session = SessionUiState(id = "1", name = "s", cwd = "/home")
        var identityAsks = 0
        val probe: ShellProbe = { _, command ->
            when (command) {
                ShellStatusProjector.IDENTITY_COMMAND -> { identityAsks++; "me\nbox" }
                else -> null
            }
        }
        runNow { session.refreshShellPresentation(0, probe) }
        assertEquals("me", session.shellPresentation.user)
        assertEquals("box", session.shellPresentation.host)
        session.cwd = "/elsewhere"
        runNow { session.refreshShellPresentation(1, probe) }
        assertEquals(1, identityAsks)
        assertEquals("me", session.shellPresentation.user)
        assertEquals("box", session.shellPresentation.host)
        assertEquals("/elsewhere", session.shellPresentation.cwd)
    }

    @Test
    fun sessionDoesNotRetryAnUnknownIdentityOrOverwriteAKnownOne() {
        val unknown = SessionUiState(id = "1", name = "s", cwd = "/home")
        var asks = 0
        val silent: ShellProbe = { _, command ->
            if (command == ShellStatusProjector.IDENTITY_COMMAND) asks++
            null
        }
        runNow { unknown.refreshShellPresentation(0, silent) }
        runNow { unknown.refreshShellPresentation(0, silent) }
        assertEquals(1, asks)
        assertNull(unknown.shellPresentation.user)

        val known = SessionUiState(id = "2", name = "s", cwd = "/home")
        known.shellPresentation = known.shellPresentation.copy(user = "ssh-user", host = "remote")
        runNow { known.refreshShellPresentation(0) { _, _ -> "me\nbox" } }
        assertEquals("ssh-user", known.shellPresentation.user)
        assertEquals("remote", known.shellPresentation.host)
    }
}
