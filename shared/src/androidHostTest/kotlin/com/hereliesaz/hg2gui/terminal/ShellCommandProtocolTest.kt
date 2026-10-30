package com.hereliesaz.hg2gui.terminal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * [ShellCommandProtocol] against real shells: every frame must report a start marker with the
 * shell pid and an end marker with status and cwd, whatever the command text is, and a command
 * that fails to parse must leave the shell usable. Skips when the shell binary is missing.
 */
class ShellCommandProtocolTest {

    private val head = "__HG2GUI_EOC_"
    private val startHead = "__HG2GUI_SOC_"
    private val tail = "abcd1234__"

    private class Frame(val pid: Int?, val body: List<String>, val status: Int, val pwd: String)

    private fun firstExecutable(vararg paths: String): String? = paths.firstOrNull { File(it).canExecute() }

    private fun runFrames(shell: String, family: ShellFamily, commands: List<String>): List<Frame> {
        val process = ProcessBuilder(shell).redirectErrorStream(true).directory(File("/")).start()
        process.outputStream.bufferedWriter().use { writer ->
            commands.forEach { writer.write(ShellCommandProtocol.frame(family, it, startHead, head, tail)) }
        }
        val lines = process.inputStream.bufferedReader().readLines()
        process.waitFor()

        val frames = mutableListOf<Frame>()
        var pid: Int? = null
        var body = mutableListOf<String>()
        for (line in lines) {
            when {
                line.startsWith(startHead + tail) -> {
                    pid = ShellCommandProtocol.parseShellPid(line.removePrefix(startHead + tail))
                    body = mutableListOf()
                }
                line.startsWith(head + tail) -> {
                    val rest = line.removePrefix(head + tail)
                    val split = rest.indexOf(':')
                    frames += Frame(pid, body, rest.substring(0, split).toInt(), rest.substring(split + 1))
                }
                else -> body.add(line)
            }
        }
        return frames
    }

    private fun roundTrip(shell: String, family: ShellFamily) {
        val commands = listOf(
            "echo hello",
            "echo (",
            "echo \"unterminated",
            "if true",
            "echo it's",
            "printf '%s\\n' \"it's \\\"quoted\\\" \\\$HOME\"",
            "cd /usr; pwd",
            "(exit 3)",
            "cat <<EOF\nheredoc line\nEOF",
            "echo trailing backslash \\",
            "echo last"
        )
        val frames = runFrames(shell, family, commands)
        assertEquals("one end marker per command - a parse error must not end the shell", commands.size, frames.size)

        val pids = frames.map { it.pid }.toSet()
        assertEquals("every frame reports the same shell pid", 1, pids.size)
        assertNotNull(pids.single())
        assertTrue(pids.single()!! > 1)

        assertEquals(listOf("hello"), frames[0].body)
        assertEquals(0, frames[0].status)
        for (broken in 1..3) {
            assertNotEquals("command #$broken fails to parse and reports it", 0, frames[broken].status)
        }
        assertNotEquals(0, frames[4].status)
        assertEquals(listOf("it's \"quoted\" \$HOME"), frames[5].body)
        assertEquals(0, frames[5].status)
        assertEquals(listOf("/usr"), frames[6].body)
        assertEquals("/usr", frames[6].pwd)
        assertEquals(3, frames[7].status)
        assertEquals("/usr", frames[7].pwd)
        assertEquals(listOf("heredoc line"), frames[8].body)
        assertEquals(listOf("trailing backslash \\"), frames[9].body)
        assertEquals(listOf("last"), frames[10].body)
        assertEquals(0, frames[10].status)
    }

    @Test
    fun framesRoundTripThroughRealSh() {
        val sh = firstExecutable("/bin/sh", "/usr/bin/sh")
        assumeTrue("no /bin/sh", sh != null)
        roundTrip(sh!!, ShellFamily.SH)
    }

    @Test
    fun framesRoundTripThroughRealBash() {
        val bash = firstExecutable("/bin/bash", "/usr/bin/bash")
        assumeTrue("no bash", bash != null)
        roundTrip(bash!!, ShellFamily.BASH)
    }

    @Test
    fun parseShellPidReadsTheMarkerSuffix() {
        assertEquals(1234, ShellCommandProtocol.parseShellPid(":1234"))
        assertEquals(1234, ShellCommandProtocol.parseShellPid(":1234\r"))
        assertNull(ShellCommandProtocol.parseShellPid(""))
        assertNull(ShellCommandProtocol.parseShellPid("1234"))
        assertNull(ShellCommandProtocol.parseShellPid(":abc"))
        assertNull(ShellCommandProtocol.parseShellPid(":1"))
    }

    @Test
    fun quotingKeepsTextInsideOneWord() {
        assertEquals("'a'\\''b'", ShellCommandProtocol.posixQuote("a'b"))
        assertEquals("'a\\'b\\\\c'", ShellCommandProtocol.fishQuote("a'b\\c"))
    }

    @Test
    fun evalBodyEndsWithExactlyOneNewlineAndDoublesADanglingBackslash() {
        assertEquals("echo hi\n", ShellCommandProtocol.evalBody("echo hi\n\n"))
        assertEquals("echo hi \\\\\n", ShellCommandProtocol.evalBody("echo hi \\"))
        assertEquals("echo hi \\\\\n", ShellCommandProtocol.evalBody("echo hi \\\\"))
    }
}
