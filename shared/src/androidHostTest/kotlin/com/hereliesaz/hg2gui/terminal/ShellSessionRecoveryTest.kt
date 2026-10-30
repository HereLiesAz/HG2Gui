package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.util.Collections
import kotlin.concurrent.thread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Audit B-1 / n-6: the shell of a tab must survive (or be replaced after) a syntax error, an
 * `exit`, a silent command and an interrupt. Runs against a real /bin/sh and skips cleanly (JUnit
 * assumption) where there is none. Lives in androidHostTest because it shortens the quiet period
 * through the internal [ShellSession.quietNoticeMs].
 */
class ShellSessionRecoveryTest {

    private val sessions = mutableListOf<ShellSession>()
    private val tempFiles = mutableListOf<File>()

    @After
    fun tearDown() {
        sessions.forEach { it.close() }
        tempFiles.forEach { it.delete() }
    }

    private fun session(quietMs: Long = 15_000L): ShellSession {
        assumeTrue("/bin/sh is not available", File("/bin/sh").canExecute())
        val s = ShellSession("/tmp", "/bin/sh")
        sessions += s
        assumeTrue("/bin/sh did not start", s.isAlive)
        s.quietNoticeMs = quietMs
        return s
    }

    private fun tempFile(prefix: String): File =
        File.createTempFile(prefix, ".tmp").also { tempFiles += it }

    private fun waitForPid(file: File, timeoutMs: Long): Int? {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val pid = file.readText().trim().toIntOrNull()
            if (pid != null) return pid
            Thread.sleep(25L)
        }
        return null
    }

    private fun waitUntilGone(pid: Int, timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (!File("/proc/$pid").exists()) return true
            Thread.sleep(25L)
        }
        return !File("/proc/$pid").exists()
    }

    @Test
    fun syntaxErrorDoesNotKillTheSessionAndTheNextCommandRuns() {
        val s = session()
        s.exec("KEEP=yes")
        for (broken in listOf("echo (", "echo \"abc", "if true", "echo it's")) {
            val r = s.exec(broken)
            assertNotEquals("'$broken' must report a failing status", 0, r.exitCode)
            assertTrue("'$broken' must not end the shell", s.isAlive)
            val next = s.exec("echo ok-\$KEEP")
            assertEquals("ok-yes", next.output)
            assertEquals(0, next.exitCode)
        }
    }

    @Test
    fun exitInsideACommandIsFollowedByARespawnedShell() {
        val s = session()
        s.exec("KEEP=yes")
        val exited = s.exec("echo bye; exit 3")
        assertEquals(-1, exited.exitCode)
        assertTrue("the transcript says the shell ended: ${exited.output}", exited.output.contains("bye"))
        assertTrue(exited.output.contains("[shell exited"))

        val next = s.exec("echo ok-\$KEEP")
        assertEquals(0, next.exitCode)
        assertTrue("a respawn is announced: ${next.output}", next.output.startsWith("[shell restarted]"))
        assertTrue("the old shell's variables are gone: ${next.output}", next.output.endsWith("ok-"))
        assertTrue(s.isAlive)

        val after = s.exec("echo again")
        assertEquals("again", after.output)
    }

    @Test
    fun respawnedShellStartsInTheHomeDirectoryAgain() {
        val s = session()
        s.exec("cd /")
        assertEquals("/", s.workingDirectory)
        s.exec("exit 0")
        val r = s.exec("pwd")
        assertEquals("/tmp", r.workingDirectory)
        assertTrue(r.output.endsWith("/tmp"))
    }

    @Test
    fun aSilentCommandLongerThanTheQuietPeriodIsNotKilled() {
        val s = session(quietMs = 300L)
        val snapshots = Collections.synchronizedList(mutableListOf<String>())
        val exitCode = s.stream(
            "sleep 1; echo done",
            onLine = { snapshots += it },
            onNeedInput = { null }
        )
        assertEquals(0, exitCode)
        assertTrue("the quiet period shows a notice: $snapshots", snapshots.any { it.contains("still running") })
        val last = snapshots.last()
        assertEquals("the notice is transient, the final transcript has only the output", "done", last)
        assertTrue(s.isAlive)
        assertEquals("next", s.exec("echo next").output)
    }

    @Test
    fun interruptWithNothingRunningIsANoOp() {
        val s = session()
        s.interrupt()
        val r = s.exec("echo fine")
        assertEquals("fine", r.output)
        assertEquals(0, r.exitCode)
    }

    @Test
    fun interruptKillsTheForegroundChildAndKeepsTheShell() {
        assumeTrue("needs /proc", File("/proc/self/stat").exists())
        val s = session()
        s.exec("KEEP=yes")
        val bgFile = tempFile("hg2gui-bg")
        val fgFile = tempFile("hg2gui-fg")
        s.exec("sleep 60 & echo \$! > '${bgFile.absolutePath}'")
        val bgPid = waitForPid(bgFile, 5_000L)
        assertTrue("background job started", bgPid != null)
        try {
            var exitCode = Int.MIN_VALUE
            val worker = thread {
                exitCode = s.stream(
                    "sh -c 'echo \$\$ > ${fgFile.absolutePath}; exec sleep 60'",
                    onLine = {},
                    onNeedInput = { null }
                )
            }
            val fgPid = waitForPid(fgFile, 5_000L)
            assertTrue("foreground child started", fgPid != null)

            val interruptedAt = System.currentTimeMillis()
            s.interrupt()
            worker.join(10_000L)
            val elapsed = System.currentTimeMillis() - interruptedAt

            assertFalse("interrupt() must end the in-flight stream", worker.isAlive)
            assertEquals(130, exitCode)
            assertTrue("the child was signalled rather than the shell discarded after its grace: ${elapsed}ms", elapsed < 3_500L)
            assertTrue("the foreground sleep must be gone", waitUntilGone(fgPid!!, 2_000L))
            assertTrue("an earlier command's background job is left alone", File("/proc/$bgPid").exists())
            assertEquals("the shell itself (and its environment) survived", "yes", s.exec("echo \$KEEP").output)
        } finally {
            s.exec("kill $bgPid")
        }
    }

    @Test
    fun theTimeoutCapCancelsTheCommandButNotTheShell() {
        val s = session()
        s.exec("KEEP=yes")
        val snapshots = Collections.synchronizedList(mutableListOf<String>())
        val startedAt = System.currentTimeMillis()
        val exitCode = s.stream(
            "sleep 60",
            onLine = { snapshots += it },
            onNeedInput = { null },
            timeoutMs = 500L
        )
        assertEquals(124, exitCode)
        assertTrue("returns promptly: ${System.currentTimeMillis() - startedAt}ms", System.currentTimeMillis() - startedAt < 6_000L)
        assertTrue(snapshots.last().contains("[timed out after 500ms]"))
        assertEquals("yes", s.exec("echo \$KEEP").output)
    }

    @Test
    fun decliningAPromptCancelsTheCommandAndTheNextOneRuns() {
        // Was decliningToAnswerTearsDownTheSessionInsteadOfDesyncingIt (ShellSessionTest): the intent
        // is the same - a declined prompt must end that command and must not leave the abandoned
        // `read` to swallow the next command as its answer - but the session is no longer dead
        // afterwards: the next command runs (in a fresh shell, since a builtin `read` leaves no
        // child to signal) instead of answering "shell is not running" forever.
        val s = session(quietMs = 400L)
        var promptOffered = false
        val snapshots = Collections.synchronizedList(mutableListOf<String>())
        val exitCode = s.stream(
            "printf 'Continue? [y/N] '; read ans; echo \"got:\$ans\"",
            onLine = { snapshots += it },
            onNeedInput = { promptOffered = true; null }
        )
        assertTrue("expected the stall to still be offered even with no answer available", promptOffered)
        assertEquals(130, exitCode)
        assertTrue(snapshots.last().contains("Continue?"))
        assertTrue(snapshots.last().contains("[cancelled"))

        val next = s.exec("echo next")
        assertEquals(0, next.exitCode)
        assertTrue("the next command runs: ${next.output}", next.output.endsWith("next"))
        assertFalse("the old prompt did not swallow it as an answer: ${next.output}", next.output.contains("got:"))
        assertTrue(s.isAlive)
    }

    @Test
    fun aSecondConcurrentCommandIsRefusedInsteadOfCorruptingTheFirst() {
        val s = session()
        val first = thread { s.stream("sleep 1; echo first", onLine = {}, onNeedInput = { null }) }
        Thread.sleep(300L)
        val refusedOutput = StringBuilder()
        val refused = s.stream("echo second", onLine = { refusedOutput.append(it) }, onNeedInput = { null })
        assertEquals(-1, refused)
        assertTrue(refusedOutput.contains("still running"))
        first.join(10_000L)
        assertEquals("third", s.exec("echo third").output)
    }

    @Test
    fun parentOfReadsThePpidFromAProcStatLine() {
        assertEquals(45, ShellProcessTree.parentOf("123 (sleep) S 45 123 123 0 -1 4194304"))
        assertEquals(7, ShellProcessTree.parentOf("123 (a b) c) R 7 1 1 0"))
        assertEquals(null, ShellProcessTree.parentOf("garbage"))
    }

    @Test
    fun descendantsWalksTheProcessTreeAndExcludesTheRoot() {
        val root = java.nio.file.Files.createTempDirectory("hg2gui-proc").toFile()
        try {
            // 10 -> 11 -> 12, 10 -> 13, and an unrelated 20 -> 21
            val tree = mapOf(10 to 1, 11 to 10, 12 to 11, 13 to 10, 20 to 1, 21 to 20)
            for ((pid, parent) in tree) {
                val dir = File(root, pid.toString()).also { it.mkdirs() }
                File(dir, "stat").writeText("$pid (cmd) S $parent $pid $pid 0 -1")
            }
            File(root, "self").mkdirs()
            assertEquals(setOf(11, 12, 13), ShellProcessTree.descendants(10, root))
            assertEquals(emptySet<Int>(), ShellProcessTree.descendants(12, root))
            assertEquals(emptySet<Int>(), ShellProcessTree.descendants(1, root))
        } finally {
            root.deleteRecursively()
        }
    }
}
