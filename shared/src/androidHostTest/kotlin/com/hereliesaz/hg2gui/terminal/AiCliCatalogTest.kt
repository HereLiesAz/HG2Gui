package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCliCatalogTest {
    @Test
    fun everyEntryPassesTheNameGrammar_andIdsAreUnique() {
        AiCliCatalog.ALL.forEach { assertTrue(it.id, AiCliCatalog.isValid(it)) }
        assertEquals(AiCliCatalog.ALL.size, AiCliCatalog.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun theRequestedToolsAreListed() {
        for (id in listOf("claude", "codex", "gemini", "ollama")) assertNotNull(id, AiCliCatalog.find(id))
        assertNull(AiCliCatalog.find("nonsense"))
        assertNull(AiCliCatalog.find("claude; rm -rf ~"))
    }

    @Test
    fun grammarRejectsAnythingThatCouldCarryShellSyntax() {
        val base = AiCliCatalog.find("gemini")!!
        for (bad in listOf("a b", "a;b", "a\$(b)", "a`b`", "a&&b", "-g", "A", "")) {
            assertFalse(bad, AiCliCatalog.isValid(base.copy(packageName = bad)))
            assertFalse(bad, AiCliCatalog.isValid(base.copy(id = bad)))
            assertFalse(bad, AiCliCatalog.isValid(base.copy(runtime = listOf(bad))))
        }
        assertFalse(AiCliCatalog.isValid(base.copy(version = "1.0")))
        assertFalse(AiCliCatalog.isValid(base.copy(version = "1.0.0; id")))
        assertTrue(AiCliCatalog.isValid(base.copy(packageName = "@scope/name", version = "1.2.3")))
    }

    @Test
    fun pkgTools_installThroughTheJournaledPackageManager() {
        val plan = AiCliCatalog.installPlan(AiCliCatalog.find("codex")!!)
        assertEquals(listOf<AiCliCatalog.Step>(AiCliCatalog.Step.Pkg("install", listOf("codex"))), plan)
        assertEquals(
            listOf<AiCliCatalog.Step>(AiCliCatalog.Step.Pkg("install", listOf("ollama"))),
            AiCliCatalog.installPlan(AiCliCatalog.find("ollama")!!)
        )
    }

    @Test
    fun npmTools_installTheNodeRuntimeFirst_thenTheTool() {
        val plan = AiCliCatalog.installPlan(AiCliCatalog.find("gemini")!!)
        assertEquals(
            listOf(
                AiCliCatalog.Step.Pkg("install", listOf("nodejs-lts")),
                AiCliCatalog.Step.Shell("npm install -g --no-audit --no-fund @google/gemini-cli")
            ),
            plan
        )
    }

    @Test
    fun claudeCode_isPinnedToTheLastJavaScriptOnlyRelease() {
        val cli = AiCliCatalog.find("claude")!!
        assertEquals("2.1.112", cli.version)
        val shell = AiCliCatalog.installPlan(cli).last() as AiCliCatalog.Step.Shell
        assertEquals("npm install -g --no-audit --no-fund @anthropic-ai/claude-code@2.1.112", shell.line)
        assertTrue("the note must say why it is pinned", cli.note.contains("2.1.112"))
    }

    @Test
    fun removal_leavesTheSharedRuntimeAlone() {
        assertEquals(
            listOf<AiCliCatalog.Step>(AiCliCatalog.Step.Shell("npm uninstall -g @google/gemini-cli")),
            AiCliCatalog.removePlan(AiCliCatalog.find("gemini")!!)
        )
        assertEquals(
            listOf<AiCliCatalog.Step>(AiCliCatalog.Step.Pkg("remove", listOf("codex"))),
            AiCliCatalog.removePlan(AiCliCatalog.find("codex")!!)
        )
    }

    @Test
    fun noPlanEverMentionsAKey() {
        // Installation only: each tool asks for its own sign-in; HG2Gui hands none of its stored keys over.
        for (cli in AiCliCatalog.ALL) {
            val text = (AiCliCatalog.installPlan(cli) + AiCliCatalog.removePlan(cli)).joinToString(" ")
            assertFalse(cli.id, text.contains("KEY", ignoreCase = true))
            assertFalse(cli.id, text.contains("TOKEN", ignoreCase = true))
        }
    }

    @Test
    fun listing_marksInstalledToolsAndNamesEveryId() {
        val text = AiCliCatalog.listing { it.id == "codex" }
        for (cli in AiCliCatalog.ALL) assertTrue(cli.id, text.contains(cli.id))
        val codex = text.lines().first { it.trimStart().startsWith("codex") }
        assertTrue(codex, codex.contains("installed") && !codex.contains("not installed"))
        val ollama = text.lines().first { it.trimStart().startsWith("ollama") }
        assertTrue(ollama, ollama.contains("not installed"))
    }
}
