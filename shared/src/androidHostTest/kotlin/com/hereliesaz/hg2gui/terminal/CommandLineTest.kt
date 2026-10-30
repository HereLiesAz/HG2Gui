package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandLineTest {

    @Test
    fun words_table() {
        val cases = listOf(
            "ls -la" to listOf("ls", "-la"),
            "  a   b  " to listOf("a", "b"),
            "echo \"a b\" 'c d'" to listOf("echo", "a b", "c d"),
            "a\"b c\"d" to listOf("ab cd"),
            "f\\ g h" to listOf("f g", "h"),
            "echo \"say \\\"hi\\\"\"" to listOf("echo", "say \"hi\""),
            "echo ''" to listOf("echo", ""),
            "" to emptyList()
        )
        for ((input, expected) in cases) assertEquals(input, expected, CommandLine.words(input))
    }

    @Test
    fun words_unterminatedQuoteIsNullNotGuessed() {
        assertNull(CommandLine.words("echo \"abc"))
        assertNull(CommandLine.words("echo 'abc"))
    }

    @Test
    fun tail_returnsTheRemainderVerbatim_notReJoinedWords() {
        // M-11: the approved text must be the text that runs, quotes included.
        val line = "hg2auth root shell sh -c \"cd /data/x && rm -rf *\""
        assertEquals("sh -c \"cd /data/x && rm -rf *\"", CommandLine.tail(line, 3))
        // The old words().drop(3).joinToString(" ") lost the quotes and so the meaning.
        assertEquals("sh -c cd /data/x && rm -rf *", CommandLine.words(line)!!.drop(3).joinToString(" "))
    }

    @Test
    fun tail_table() {
        val cases = listOf(
            Triple("hg2auth adb shell pm list packages", 3, "pm list packages"),
            Triple("hg2auth   adb   shell    ls  'a  b'", 3, "ls  'a  b'"),
            Triple("a \"b c\" d", 2, "d"),
            Triple("a b", 5, ""),
            Triple("  a b  ", 0, "a b")
        )
        for ((line, skip, expected) in cases) assertEquals(line, expected, CommandLine.tail(line, skip))
        assertNull(CommandLine.tail("a \"b c", 2))
    }

    private fun names(line: String) = CommandLine.parse(line).invocations.map { it.name }

    @Test
    fun parse_findsEveryCommandPosition() {
        val cases = listOf(
            "ls -la" to listOf("ls"),
            "cd x && tool" to listOf("cd", "tool"),
            "a || b ; c" to listOf("a", "b", "c"),
            "a | tool" to listOf("a", "tool"),
            "env FOO=1 tool arg" to listOf("tool"),
            "time tool" to listOf("tool"),
            "sudo -u root tool" to listOf("tool"),
            "nohup tool &" to listOf("tool"),
            "timeout 5 tool" to listOf("tool"),
            "xargs -n 1 tool" to listOf("tool"),
            "FOO=1 BAR=2 tool" to listOf("tool"),
            "bash -c 'tool x; other'" to listOf("bash", "tool", "other"),
            "sh -lc \"cd /x && tool\"" to listOf("sh", "cd", "tool"),
            "eval tool x" to listOf("eval", "tool"),
            "echo \$(tool a)" to listOf("tool", "echo"),
            "echo `tool a`" to listOf("tool", "echo"),
            "cat <(tool a)" to listOf("tool", "cat"),
            "find . -exec tool {} \\;" to listOf("find", "tool"),
            "/data/x/usr/bin/tool --v" to listOf("tool"),
            "(cd x && tool)" to listOf("cd", "tool"),
            "if true; then tool; fi" to listOf("true", "tool"),
            "ls 2>&1 | grep tool" to listOf("ls", "grep"),
            "echo hi > out.txt" to listOf("echo"),
            "echo \"\${x:-\$(tool)}\"" to listOf("tool", "echo")
        )
        for ((line, expected) in cases) assertEquals(line, expected.sorted(), names(line).sorted())
    }

    @Test
    fun parse_commandLookupsAndQuotedWordsAreNotCommands() {
        assertEquals(emptyList<String>(), names("command -v tool").filter { it == "tool" })
        assertEquals(listOf("echo"), names("echo tool"))
        assertEquals(listOf("echo"), names("echo 'a && tool'"))
        assertEquals(listOf("echo"), names("echo hi # && tool"))
        assertEquals(listOf("echo"), names("echo a\\;tool"))
    }

    @Test
    fun parse_marksHowACommandWasReached() {
        fun kinds(line: String) = CommandLine.parse(line).invocations.associate { it.name to it.kind }
        assertEquals(CommandLine.Kind.DIRECT, kinds("tool")["tool"])
        assertEquals(CommandLine.Kind.DIRECT, kinds("FOO=1 tool")["tool"])
        assertEquals(CommandLine.Kind.WRAPPED, kinds("env tool")["tool"])
        assertEquals(CommandLine.Kind.NESTED, kinds("bash -c tool")["tool"])
        assertEquals(CommandLine.Kind.NESTED, kinds("echo \$(tool)")["tool"])
    }

    @Test
    fun parse_dynamicCommandWordIsFlagged() {
        val inv = CommandLine.parse("\$TOOL install x").invocations.single()
        assertTrue(inv.dynamic)
        assertTrue(CommandLine.parse("t*l --x").invocations.single().dynamic)
        assertFalse(CommandLine.parse("tool").invocations.single().dynamic)
    }

    @Test
    fun parse_splitsTopLevelListsWithConnectors() {
        val p = CommandLine.parse("pkg update && pkg upgrade || echo no ; ls")
        assertFalse(p.complex)
        assertEquals(
            listOf(
                CommandLine.Connector.NONE to "pkg update",
                CommandLine.Connector.AND to "pkg upgrade",
                CommandLine.Connector.OR to "echo no",
                CommandLine.Connector.SEQ to "ls"
            ),
            p.segments.map { it.connector to it.text }
        )
    }

    @Test
    fun parse_pipelineStaysOneSegmentWithTwoStages() {
        val p = CommandLine.parse("ls | grep x && echo ok")
        assertEquals(2, p.segments.size)
        assertEquals(2, p.segments[0].stages.size)
    }

    @Test
    fun parse_operatorsInsideQuotesDoNotSplit() {
        assertEquals(1, CommandLine.parse("echo 'a && b ; c'").segments.size)
        assertEquals(1, CommandLine.parse("echo \"a || b\"").segments.size)
        assertEquals(1, CommandLine.parse("echo \$(a && b)").segments.size)
    }

    @Test
    fun parse_controlStructuresBackgroundAndHeredocsAreComplex() {
        val complex = listOf(
            "if true; then tool; fi", "for f in a b; do tool; done", "while true; do x; done",
            "tool &", "(cd x; tool)", "{ a; b; }", "cat <<EOF", "case x in a) tool;; esac",
            "a &&", "&& a", "f() { tool; }"
        )
        for (line in complex) assertTrue(line, CommandLine.parse(line).complex)
        assertFalse(CommandLine.parse("a && b | c").complex)
    }

    @Test
    fun parse_unterminatedQuoteOrSubstitutionIsReported() {
        assertTrue(CommandLine.parse("echo \"abc").unterminated)
        assertTrue(CommandLine.parse("echo 'abc").unterminated)
        assertTrue(CommandLine.parse("echo \$(abc").unterminated)
        assertTrue(CommandLine.parse("echo `abc").unterminated)
        assertFalse(CommandLine.parse("echo hi # it's a comment").unterminated)
        assertFalse(CommandLine.parse("echo \$'it\\'s'").unterminated)
    }
}
