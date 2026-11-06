package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.ai.AiIntentAnalyzer
import com.hereliesaz.hg2gui.ai.AiIntentKind
import com.hereliesaz.hg2gui.ai.AiProvider
import com.hereliesaz.hg2gui.managers.AiSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** `aicli` is an engine verb: planned, policy-gated and classified like the other install commands. */
class AiCliRoutingTest {
    private fun steps(line: String): List<LinePlanner.Step> =
        (LinePlanner.plan(line, emptySet()) { null } as LinePlanner.Plan.Steps).steps

    @Test
    fun aicliIsRoutedToTheEngineNotTheShell() {
        val step = steps("aicli install claude").single()
        assertEquals(LinePlanner.StepKind.ENGINE, step.kind)
        assertEquals("aicli", step.verb)
        assertEquals(listOf("aicli", "install", "claude"), step.words)
    }

    @Test
    fun aicliChainsWithTheToolItInstalls() {
        val planned = steps("aicli install gemini && gemini --version")
        assertEquals(listOf(LinePlanner.StepKind.ENGINE, LinePlanner.StepKind.SHELL), planned.map { it.kind })
    }

    @Test
    fun interactiveMayInstall() {
        for (sub in listOf(null, "list", "install", "remove", "nonsense")) {
            assertNull(sub, ExecutionPolicy.INTERACTIVE.aiCliDenial(sub))
        }
    }

    @Test
    fun headlessCallersMayOnlyList() {
        for (policy in listOf(ExecutionPolicy.api(appInForeground = true), ExecutionPolicy.MCP, ExecutionPolicy.INSTALLER)) {
            assertNull(policy.origin.name, policy.aiCliDenial(null))
            assertNull(policy.origin.name, policy.aiCliDenial("list"))
            for (sub in listOf("install", "remove", "nonsense")) {
                val denial = policy.aiCliDenial(sub)
                assertNotNull("${policy.origin} $sub", denial)
                assertTrue(denial!!.contains(policy.label))
            }
        }
    }

    @Test
    fun anAiSuggestedAicliInstallIsClassifiedAsAPackageMutation() {
        assertEquals(AiIntentKind.PACKAGE_MUTATION, AiIntentAnalyzer.analyze("aicli install claude").kind)
        assertEquals(AiIntentKind.PACKAGE_MUTATION, AiIntentAnalyzer.analyze("aicli remove codex").kind)
        assertEquals(AiIntentKind.COMMAND, AiIntentAnalyzer.analyze("aicli list").kind)
    }

    @Test
    fun theAnthropicKeyKeepsItsOriginalStorageName_andOthersAreSeparate() {
        assertEquals("api_key", AiSettings.keyName(AiProvider.ANTHROPIC))
        val others = AiProvider.entries.filter { it != AiProvider.ANTHROPIC }.map { AiSettings.keyName(it) }
        assertEquals(others.size, others.toSet().size)
        assertTrue(others.none { it == "api_key" })
    }
}
