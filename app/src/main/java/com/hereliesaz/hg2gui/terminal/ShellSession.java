package com.hereliesaz.hg2gui.terminal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A long-lived shell process, framed into discrete commands.
 *
 * The obvious way to run a command on Android is one Runtime.exec per line, but then every
 * line starts in a fresh process: `cd` goes nowhere, exported variables evaporate, and `$?`
 * is meaningless. That is a command runner, not a terminal.
 *
 * So the shell is started once and kept. Commands are written to its stdin and the output is
 * read back until a sentinel appears. The sentinel carries the exit status and the working
 * directory, which is how the caller learns where the session ended up without spawning a
 * second process to ask.
 *
 * There is no pty here. Nothing allocates a terminal device, so there is no job control, no
 * cursor addressing and no SIGWINCH — programs that demand a real terminal (top, vim) will
 * not behave. That is deliberate: the UI renders each command as a discrete record, not as a
 * scrollback buffer with a cursor in it, so a pty would be machinery nothing draws.
 *
 * Not thread-safe by design — {@link #exec} is synchronized because a single shell has a
 * single stdin, and interleaving two commands into it would splice their output together.
 */
public class ShellSession {

    /** Marks end-of-command in the output stream. Long enough not to collide with real output. */
    private static final String SENTINEL = "__HG2GUI_EOC_a7f3__";

    /** Fallback shell. Every Android device has this; it is toybox/mksh, not bash. */
    private static final String DEFAULT_SHELL = "/system/bin/sh";

    /** How long a single command may run before we give up reading its output. */
    private static final long TIMEOUT_MS = 15_000L;

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;

    private final AtomicBoolean alive = new AtomicBoolean(false);
    private volatile String workingDirectory;

    /** The result of one command: what it printed, and where it left the session. */
    public static class Result {
        public final String output;
        public final int exitCode;
        public final String workingDirectory;

        Result(String output, int exitCode, String workingDirectory) {
            this.output = output;
            this.exitCode = exitCode;
            this.workingDirectory = workingDirectory;
        }
    }

    /**
     * Starts the shell.
     *
     * @param home directory the session starts in; falls back to the shell's own default
     *             if null or missing.
     */
    public ShellSession(File home) {
        this(home, DEFAULT_SHELL);
    }

    /**
     * @param shellPath which shell binary to run. Exists so the framing logic can be exercised
     *                  on a host JVM, where the Android shell path does not exist.
     */
    public ShellSession(File home, String shellPath) {
        try {
            ProcessBuilder builder = new ProcessBuilder(shellPath);
            // Interleave stderr into stdout. A terminal shows both in one stream, and keeping
            // them separate would need a second reader thread to avoid deadlocking on a full
            // stderr pipe buffer.
            builder.redirectErrorStream(true);
            if (home != null && home.isDirectory()) builder.directory(home);

            process = builder.start();
            stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

            alive.set(true);
            workingDirectory = home != null ? home.getAbsolutePath() : "/";
        } catch (IOException e) {
            alive.set(false);
        }
    }

    public boolean isAlive() {
        return alive.get() && process != null && processStillRunning();
    }

    private boolean processStillRunning() {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException stillRunning) {
            return true;
        }
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Runs one command and blocks until it completes. Call this off the main thread.
     *
     * The sentinel line is emitted by the shell itself, after the command, so it cannot appear
     * before the command's own output has been flushed. It carries `$?` and `$PWD` so a single
     * round trip reports status and location together.
     */
    public synchronized Result exec(String command) {
        if (!isAlive()) {
            return new Result("shell is not running", -1, workingDirectory);
        }

        StringBuilder collected = new StringBuilder();
        int exitCode = -1;

        try {
            stdin.write(command);
            stdin.write("\n");
            // Echo the sentinel with status and cwd. Captured before anything else can reset $?.
            stdin.write("printf '%s%d:%s\\n' \"" + SENTINEL + "\" \"$?\" \"$PWD\"\n");
            stdin.flush();

            long deadline = System.currentTimeMillis() + TIMEOUT_MS;

            while (true) {
                if (System.currentTimeMillis() > deadline) {
                    collected.append("\n[timed out after ")
                            .append(TIMEOUT_MS / 1000).append("s]");
                    break;
                }

                // Avoid blocking past the deadline on a command that never returns.
                if (!stdout.ready()) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    if (!processStillRunning()) {
                        alive.set(false);
                        break;
                    }
                    continue;
                }

                String line = stdout.readLine();
                if (line == null) {
                    alive.set(false);
                    break;
                }

                int marker = line.indexOf(SENTINEL);
                if (marker >= 0) {
                    // Anything before the sentinel on this line is real output with no trailing
                    // newline, so it must not be discarded.
                    if (marker > 0) collected.append(line, 0, marker);

                    String tail = line.substring(marker + SENTINEL.length());
                    int split = tail.indexOf(':');
                    if (split > 0) {
                        try {
                            exitCode = Integer.parseInt(tail.substring(0, split));
                        } catch (NumberFormatException ignored) {
                            // Leave exitCode at -1; the output is still valid.
                        }
                        String pwd = tail.substring(split + 1);
                        if (!pwd.isEmpty()) workingDirectory = pwd;
                    }
                    break;
                }

                if (collected.length() > 0) collected.append('\n');
                collected.append(line);
            }
        } catch (IOException e) {
            alive.set(false);
            return new Result("shell died: " + e.getMessage(), -1, workingDirectory);
        }

        return new Result(collected.toString(), exitCode, workingDirectory);
    }

    /** Ends the session. The shell exits when its stdin closes. */
    public void close() {
        alive.set(false);
        try {
            if (stdin != null) {
                stdin.write("exit\n");
                stdin.flush();
                stdin.close();
            }
        } catch (IOException ignored) {
            // Already gone; destroy below covers it.
        }
        if (process != null) process.destroy();
    }
}
