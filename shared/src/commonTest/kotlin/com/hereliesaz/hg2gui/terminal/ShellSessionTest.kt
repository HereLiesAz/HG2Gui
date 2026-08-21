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
        // D3: this used to assert "oops" landed in r.output - the pipe tier merged stdout and
        // stderr together (redirectErrorStream(true)) at the time, so that was actually correct
        // for what the code did then. Now that the two are kept apart, stderr text belongs in
        // its own field and stdout stays untouched by a command that never wrote to it.
        val s = shell ?: return
        val r = s.exec("echo oops >&2")
        assertEquals("", r.output)
        assertEquals("oops", r.stderr)
    }

    @Test
    fun stdoutAndStderrDoNotLeakIntoEachOther() {
        val s = shell ?: return
        var stdoutSeen = ""
        var stderrSeen = ""
        val exitCode = s.stream(
            "echo out-line; echo err-line >&2",
            onLine = { line -> stdoutSeen = line },
            onNeedInput = { null },
            onStderrLine = { line -> stderrSeen = line }
        )
        assertEquals(0, exitCode)
        assertEquals("out-line", stdoutSeen)
        assertEquals("err-line", stderrSeen)
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

    @Test
    fun outputExceedingTheScrollbackBufferIsFlaggedAsTruncated() {
        // MCP-13: the headless TerminalEmulator's scrollback is a fixed-size circular buffer
        // (1000 rows) - a command producing more lines than that silently loses its earliest
        // ones with no signal, which matters most for an MCP caller with no live screen to
        // notice the scroll. 1200 lines comfortably exceeds the buffer without being slow.
        val s = shell ?: return
        val r = s.exec("i=1; while [ \$i -le 1200 ]; do echo line\$i; i=\$((i+1)); done")
        assertEquals(0, r.exitCode)
        assertTrue(
            r.output.startsWith("[earlier output truncated"),
            "expected a truncation marker once output exceeds the scrollback buffer: ${r.output.take(120)}"
        )
        assertTrue(r.output.contains("line1200"), "the most recent lines must still be present")
    }
}
