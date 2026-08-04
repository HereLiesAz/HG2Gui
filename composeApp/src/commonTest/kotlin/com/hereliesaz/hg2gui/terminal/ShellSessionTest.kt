package com.hereliesaz.hg2gui.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Exercises the sentinel framing against a real shell.
 *
 * These run on the host JVM, so they use /bin/sh rather than Android's /system/bin/sh. What is
 * under test is the framing — that output is captured whole, that the sentinel is not mistaken
 * for output, that the exit status and working directory come back, and above all that state
 * persists across commands, which is the entire reason the process is kept alive.
 */
public class ShellSessionTest {

    private ShellSession shell;

    @Before
    public void setUp() {
        assumeTrue("needs a POSIX shell on the host", new File("/bin/sh").exists());
        shell = new ShellSession(new File("/tmp"), "/bin/sh");
        assumeTrue("shell failed to start", shell.isAlive());
    }

    @After
    public void tearDown() {
        if (shell != null) shell.close();
    }

    @Test
    public void capturesSingleLineOutput() {
        ShellSession.Result r = shell.exec("echo hello");
        assertEquals("hello", r.output);
        assertEquals(0, r.exitCode);
    }

    @Test
    public void capturesMultiLineOutput() {
        ShellSession.Result r = shell.exec("printf 'a\\nb\\nc\\n'");
        assertEquals("a\nb\nc", r.output);
        assertEquals(0, r.exitCode);
    }

    /**
     * A specific status, in a subshell — a bare `exit 3` would take the session down with it,
     * and then this would pass on the dead-shell sentinel value rather than on the real code.
     */
    @Test
    public void reportsSpecificNonZeroExitStatus() {
        ShellSession.Result r = shell.exec("(exit 3)");
        assertEquals(3, r.exitCode);
        assertTrue("a subshell exit must not end the session", shell.isAlive());
    }

    @Test
    public void failedCommandStillReturnsAndKeepsShellAlive() {
        ShellSession.Result r = shell.exec("false");
        assertEquals(1, r.exitCode);
        assertTrue("shell must survive a failing command", shell.isAlive());
    }

    /** The whole point of a persistent session: cd has to stick. */
    @Test
    public void workingDirectoryPersistsAcrossCommands() {
        shell.exec("cd /");
        ShellSession.Result r = shell.exec("pwd");
        assertEquals("/", r.output);
        assertEquals("/", r.workingDirectory);
        assertEquals("/", shell.getWorkingDirectory());
    }

    /** Likewise environment: a fresh process per command would lose this. */
    @Test
    public void environmentPersistsAcrossCommands() {
        shell.exec("MARKER=42");
        ShellSession.Result r = shell.exec("echo $MARKER");
        assertEquals("42", r.output);
    }

    /** Output that merely resembles the sentinel must not end the command early. */
    @Test
    public void outputContainingSentinelPrefixIsNotTruncated() {
        ShellSession.Result r = shell.exec("echo '__HG2GUI_ __HG2GUI__ done'");
        assertEquals("__HG2GUI_ __HG2GUI__ done", r.output);
    }

    /** stderr is interleaved, the way a terminal shows it. */
    @Test
    public void capturesStandardError() {
        ShellSession.Result r = shell.exec("echo oops >&2");
        assertEquals("oops", r.output);
    }

    @Test
    public void emptyOutputIsEmptyNotNull() {
        ShellSession.Result r = shell.exec("true");
        assertEquals("", r.output);
        assertEquals(0, r.exitCode);
    }

    /** Commands run in sequence must not bleed into one another. */
    @Test
    public void consecutiveCommandsDoNotLeakOutput() {
        assertEquals("first", shell.exec("echo first").output);
        assertEquals("second", shell.exec("echo second").output);
        assertEquals("third", shell.exec("echo third").output);
    }
}
