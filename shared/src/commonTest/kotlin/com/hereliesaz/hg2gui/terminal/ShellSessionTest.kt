package com.hereliesaz.hg2gui.terminal

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellSessionTest {

    private var shell: ShellSession? = null

    @BeforeTest
    fun setUp() {
        shell = ShellSession("/tmp", "/bin/sh")
        if (shell?.isAlive != true) {
            shell = null
        }
    }

    @AfterTest
    fun tearDown() {
        shell?.close()
    }

    @Test
    fun capturesSingleLineOutput() {
        val s = shell ?: return
        val r = s.exec("echo hello")
        assertEquals("hello", r.output)
        assertEquals(0, r.exitCode)
    }

    @Test
    fun capturesMultiLineOutput() {
        val s = shell ?: return
        val r = s.exec("printf 'a\\nb\\nc\\n'")
        assertEquals("a\nb\nc", r.output)
        assertEquals(0, r.exitCode)
    }

    @Test
    fun reportsSpecificNonZeroExitStatus() {
        val s = shell ?: return
        val r = s.exec("(exit 3)")
        assertEquals(3, r.exitCode)
        assertTrue(s.isAlive, "a subshell exit must not end the session")
    }

    @Test
    fun failedCommandStillReturnsAndKeepsShellAlive() {
        val s = shell ?: return
        val r = s.exec("false")
        assertEquals(1, r.exitCode)
        assertTrue(s.isAlive, "shell must survive a failing command")
    }

    @Test
    fun workingDirectoryPersistsAcrossCommands() {
        val s = shell ?: return
        s.exec("cd /")
        val r = s.exec("pwd")
        assertEquals("/", r.output)
        assertEquals("/", r.workingDirectory)
        assertEquals("/", s.workingDirectory)
    }

    @Test
    fun environmentPersistsAcrossCommands() {
        val s = shell ?: return
        s.exec("MARKER=42")
        val r = s.exec("echo \$MARKER")
        assertEquals("42", r.output)
    }

    @Test
    fun outputContainingSentinelPrefixIsNotTruncated() {
        val s = shell ?: return
        val r = s.exec("echo '__HG2GUI_ __HG2GUI__ done'")
        assertEquals("__HG2GUI_ __HG2GUI__ done", r.output)
    }

    @Test
    fun capturesStandardError() {
        val s = shell ?: return
        val r = s.exec("echo oops >&2")
        assertEquals("oops", r.output)
    }

    @Test
    fun emptyOutputIsEmptyNotNull() {
        val s = shell ?: return
        val r = s.exec("true")
        assertEquals("", r.output)
        assertEquals(0, r.exitCode)
    }

    @Test
    fun consecutiveCommandsDoNotLeakOutput() {
        val s = shell ?: return
        assertEquals("first", s.exec("echo first").output)
        assertEquals("second", s.exec("echo second").output)
        assertEquals("third", s.exec("echo third").output)
    }

    @Test
    fun surfacesAndAnswersAPromptInsteadOfHanging() {
        val s = shell ?: return
        var promptSeen: String? = null
        var finalOutput = ""
        val exitCode = s.stream(
            "printf 'Continue? [y/N] '; read ans; echo \"got:\$ans\"",
            onLine = { line -> finalOutput = line },
            onNeedInput = { prompt ->
                promptSeen = prompt
                "yes"
            }
        )
        assertEquals(0, exitCode)
        assertTrue(promptSeen?.contains("Continue?") == true, "expected the stalled prompt text to be surfaced: $promptSeen")
        assertTrue(finalOutput.contains("got:yes"), "expected the supplied answer to reach the running command: $finalOutput")
    }

    @Test
    fun decliningToAnswerTearsDownTheSessionInsteadOfDesyncingIt() {
        val s = shell ?: return
        var promptOffered = false
        s.stream(
            "printf 'Continue? [y/N] '; read ans; echo \"got:\$ans\"",
            onLine = { },
            onNeedInput = { promptOffered = true; null }
        )
        assertTrue(promptOffered, "expected the stall to still be offered even with no answer available")
        assertFalse(
            s.isAlive,
            "a declined/timed-out prompt must tear the session down; leaving it alive but abandoned " +
                "mid-read means the next command silently gets consumed as the old prompt's answer"
        )

        var nextOutput = ""
        val exitCode = s.stream("echo next", onLine = { nextOutput = it }, onNeedInput = { null })
        assertEquals(-1, exitCode, "a torn-down session must not silently swallow the next command")
        assertTrue(nextOutput.contains("shell is not running"))
    }
}
