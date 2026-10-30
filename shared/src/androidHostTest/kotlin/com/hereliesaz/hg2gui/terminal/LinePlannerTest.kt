package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TerminalEngine's routing decisions (M-5, B-2), tested through the pure planner with fabricated owners. */
class LinePlannerTest {
    private fun pkg(name: String, vararg binaries: String, disabled: Boolean = false, isolated: Boolean = false) =
        PackageLifecycleStore.InstalledPackage("pkg", "Termux / pkg", name, "1.0", binaries.toList(), disabled, isolated)

    private val builtins = setOf("wifi", "call", "calc", "vfs", "edit")
    private val installed = listOf(
        pkg("offtool", "offtool", "off", disabled = true),
        pkg("boxed", "boxed", isolated = true),
        pkg("plain", "plain"),
        pkg("coreutils", "ls", "cat"),
        pkg("apt", "apt", "apt-get", "pkg")
    )

    private fun plan(line: String) = LinePlanner.plan(line, builtins) { name -> installed.firstOrNull { name in it.binaries } }

    private fun refusal(line: String): LinePlanner.Plan.Refuse = plan(line) as LinePlanner.Plan.Refuse

    private fun steps(line: String): List<LinePlanner.Step> = (plan(line) as LinePlanner.Plan.Steps).steps

    // -- Disable -------------------------------------------------------------------------------

    @Test
    fun disabledPackage_blocksEverySpellingOfTheCommand() {
        val lines = listOf(
            "offtool", "offtool --x", "/data/x/usr/bin/offtool", "cd x && offtool", "true; offtool", "false || offtool",
            "ls | offtool", "env offtool", "env FOO=1 offtool", "time offtool", "sudo offtool", "nohup offtool &",
            "bash -c offtool", "sh -c 'cd x && offtool'", "echo \$(offtool)", "echo `offtool`",
            "find . -exec offtool {} \\;", "xargs offtool", "FOO=1 offtool", "eval offtool", "if true; then offtool; fi",
            "(offtool)", "off"
        )
        for (line in lines) {
            val r = refusal(line)
            assertEquals(line, ExecutionPolicy.DENIED_EXIT_CODE, r.exitCode)
            assertTrue(line, r.message.contains("disabled in HG2Gui"))
        }
    }

    @Test
    fun disabledPackage_doesNotBlockLinesThatOnlyMentionItsName() {
        val lines = listOf("echo offtool", "ls offtool", "cat 'offtool'", "grep offtool file", "man offtool", "command -v offtool", "echo hi # offtool")
        for (line in lines) assertTrue(line, plan(line) !is LinePlanner.Plan.Refuse)
    }

    // -- Isolation -----------------------------------------------------------------------------

    @Test
    fun isolatedPackage_runsAsItsOwnCommandOrAListStep() {
        val single = steps("boxed --flag")
        assertEquals(LinePlanner.StepKind.OWNED, single.single().kind)
        assertTrue(single.single().owner!!.isolated)

        val list = steps("cd x && boxed run")
        assertEquals(listOf(LinePlanner.StepKind.SHELL, LinePlanner.StepKind.OWNED), list.map { it.kind })
        assertEquals(CommandLine.Connector.AND, list[1].connector)

        // Assignment prefix and benign wrappers still run inside the sandbox as one line.
        assertEquals(LinePlanner.StepKind.OWNED, steps("FOO=1 boxed").single().kind)
        assertEquals(LinePlanner.StepKind.OWNED, steps("env FOO=1 boxed").single().kind)
        assertEquals(LinePlanner.StepKind.OWNED, steps("time boxed").single().kind)
    }

    @Test
    fun isolatedPackage_isRefusedWhereItCouldEscapeItsSandbox() {
        val lines = listOf(
            "ls | boxed", "boxed | cat", "bash -c boxed", "echo \$(boxed)", "echo `boxed`", "find . -exec boxed {} \\;",
            "eval boxed", "if true; then boxed; fi", "(boxed)", "boxed &", "cat <(boxed)"
        )
        for (line in lines) {
            val r = refusal(line)
            assertEquals(line, ExecutionPolicy.DENIED_EXIT_CODE, r.exitCode)
            assertTrue(line, r.message.contains("isolated"))
        }
    }

    // -- Package transaction layer -------------------------------------------------------------

    @Test
    fun packageManagerCommands_areRoutedToTheEngineEvenInsideLists() {
        val single = steps("pkg install git")
        assertEquals(LinePlanner.StepKind.ENGINE, single.single().kind)
        assertEquals("pkg", single.single().verb)

        val list = steps("apt update && apt install -y git")
        assertEquals(listOf(LinePlanner.StepKind.ENGINE, LinePlanner.StepKind.ENGINE), list.map { it.kind })
        assertEquals(listOf("apt", "apt"), list.map { it.verb })

        val mixed = steps("cd x ; pkg update ; ls")
        assertEquals(listOf(LinePlanner.StepKind.SHELL, LinePlanner.StepKind.ENGINE, LinePlanner.StepKind.OWNED), mixed.map { it.kind })
    }

    @Test
    fun mutatingPackageManagerCommands_areRefusedWhenTheEngineCannotJournalThem() {
        val lines = listOf(
            "sudo apt install foo", "env apt-get install foo", "time pkg install foo", "echo y | apt install foo",
            "bash -c 'apt install foo'", "echo \$(apt install foo)", "if true; then apt install foo; fi",
            "apt install foo &", "(pkg install foo)", "FOO=1 apt install foo", "xargs apt install", "true; /usr/bin/apt install foo",
            "apt autoremove | cat", "nohup pkg upgrade"
        )
        for (line in lines) {
            val r = refusal(line)
            assertEquals(line, 2, r.exitCode)
            assertTrue(line, r.message.contains("Nothing was run"))
        }
    }

    @Test
    fun readOnlyPackageManagerUseInPipesAndWrappersStillWorks() {
        val lines = listOf("pkg list-installed | grep git", "apt search foo | head", "time pkg search foo", "sudo apt show foo", "apt-get -s install foo | cat")
        for (line in lines) assertTrue(line, plan(line) !is LinePlanner.Plan.Refuse)
    }

    // -- Ordinary lines are untouched ---------------------------------------------------------

    @Test
    fun ordinaryShellLines_goToTheShellExactlyAsTyped() {
        val lines = listOf(
            "echo hi", "grep x file | sort | uniq -c", "cd x && make && make install", "echo a > b 2>&1", "echo 'a && b'",
            "for i in 1 2 3; do echo \$i; done", "x=1", "echo \$(date)", "# just a comment"
        )
        for (line in lines) {
            val p = plan(line)
            assertTrue(line, p is LinePlanner.Plan.Shell)
            assertEquals(line, (p as LinePlanner.Plan.Shell).line)
        }
    }

    @Test
    fun builtinsAndEngineVerbs_areOnlyRecognisedAsTheCommandWord() {
        assertEquals(LinePlanner.StepKind.BUILTIN, steps("calc 1+2").single().kind)
        assertEquals(LinePlanner.StepKind.BUILTIN, steps("vfs ls").single().kind)
        assertEquals(LinePlanner.StepKind.ENGINE, steps("vfs mount /x").single().kind)
        assertEquals(LinePlanner.StepKind.ENGINE, steps("hg2auth status").single().kind)
        assertEquals(LinePlanner.StepKind.ENGINE, steps("bootstrap").single().kind)
        assertEquals(LinePlanner.StepKind.ENGINE, steps("download http://x/y").single().kind)
        assertEquals(LinePlanner.StepKind.ENGINE, steps("remote scan").single().kind)
        // Not the command word: plain arguments, or behind a pipe, so they stay shell text.
        assertTrue(plan("echo calc") is LinePlanner.Plan.Shell)
        assertTrue(plan("echo hi | calc") is LinePlanner.Plan.Shell)
    }

    @Test
    fun unterminatedQuote_isRefusedInsteadOfReachingTheShell() {
        val r = refusal("echo \"abc")
        assertEquals(2, r.exitCode)
        assertTrue(r.message.contains("Unterminated"))
        assertTrue(plan("cat <<EOF\nit's") !is LinePlanner.Plan.Refuse)
    }

    @Test
    fun ownedNonIsolatedPackage_isRoutedForTracking() {
        val s = steps("plain --x").single()
        assertEquals(LinePlanner.StepKind.OWNED, s.kind)
        assertEquals("plain", s.owner!!.name)
    }

    @Test
    fun dynamicCommandWords_areNotGuessedAt() {
        assertTrue(plan("\$TOOL install foo") is LinePlanner.Plan.Shell)
        // The name appears only as an argument of `which`, so nothing runs it.
        assertTrue(plan("\$(which offtool)") is LinePlanner.Plan.Shell)
    }
}
