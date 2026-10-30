package com.hereliesaz.hg2gui.ui

import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalUiLogicTest {

    @Test
    fun confirmPromptTitleIsTheQuestionAndBodyKeepsEveryLine() {
        val parsed = ConfirmPrompts.parse("Run as root? su -c 'a\nb\nc' [y/N]")
        assertEquals(ConfirmPromptKind.Root, parsed.kind)
        assertEquals("Run as root?", parsed.title)
        assertTrue(parsed.body.contains("a\nb\nc"))
        assertTrue(parsed.badge != null)
    }

    @Test
    fun adbAndGeneralPromptsAreClassified() {
        assertEquals(ConfirmPromptKind.Adb, ConfirmPrompts.kindOf("Run through ADB shell? ls [y/N]"))
        assertEquals(ConfirmPromptKind.General, ConfirmPrompts.kindOf("Delete restore point 3 for git? [y/N]"))
        assertNull(ConfirmPrompts.parse("Reset git? This deletes data. [y/N]").badge)
    }

    @Test
    fun promptWithoutAQuestionFallsBackToAGenericTitle() {
        val parsed = ConfirmPrompts.parse("Continue (y/n)")
        assertEquals("Confirm", parsed.title)
        assertEquals("Continue (y/n)", parsed.body)
    }

    @Test
    fun generalDetailKeepsTheTailOfALongTranscript() {
        val transcript = (1..40).joinToString("\n") { "line $it" } + "\nProceed? [y/N]"
        val detail = ConfirmPrompts.detailOf(transcript)
        assertTrue(detail.endsWith("Proceed? [y/N]"))
        assertEquals(12, detail.lines().size)
    }

    @Test
    fun sessionBadgePriority() {
        assertEquals(SessionActivity.WaitingForInput, sessionActivityOf(running = true, waitingForInput = true, finishedWhileAway = true))
        assertEquals(SessionActivity.Running, sessionActivityOf(running = true, waitingForInput = false, finishedWhileAway = true))
        assertEquals(SessionActivity.Finished, sessionActivityOf(running = false, waitingForInput = false, finishedWhileAway = true))
        assertEquals(SessionActivity.Idle, sessionActivityOf(running = false, waitingForInput = false, finishedWhileAway = false))
    }

    @Test
    fun wipeLedgerNeverLowersAndPrunes() {
        val ledger = OutputWipeLedger()
        assertEquals(0, ledger.shownLines(1))
        ledger.record(1, 5)
        ledger.record(1, 3)
        ledger.record(2, 4)
        assertEquals(5, ledger.shownLines(1))
        ledger.retainOnly(setOf(2L))
        assertEquals(0, ledger.shownLines(1))
        assertEquals(4, ledger.shownLines(2))
    }

    @Test
    fun progressFillReadsOnEveryGround() {
        Azphalt.grounds.forEach { ground ->
            val fill = progressFillOn(ground.page)
            assertTrue(contrastRatio(fill, ground.page) >= AzphaltContrast.NON_TEXT, "fill on ${ground.name}")
        }
        assertEquals(Azphalt.Ink, progressFillOn(Azphalt.grounds.first { it.name == "Mustard" }.page))
    }

    @Test
    fun portraitPhoneNeverUsesTheSideLayout() {
        val plan = planWorkspace(width = 360.dp, height = 120.dp, estimatedPillRows = 11, hasBuffer = true)
        assertIs<WorkspacePlan.Stacked>(plan)
        assertTrue(plan.commandHeight >= 72.dp)
    }

    @Test
    fun tallStackIsCappedSoTheBufferSurvives() {
        val plan = planWorkspace(width = 360.dp, height = 460.dp, estimatedPillRows = 11, hasBuffer = true)
        assertIs<WorkspacePlan.Stacked>(plan)
        assertTrue(plan.commandHeight <= 460.dp * 0.6f)
        assertTrue(plan.bufferHeight >= 96.dp)
    }

    @Test
    fun landscapePhoneUsesTheSideLayoutWithUsableColumns() {
        val plan = planWorkspace(width = 720.dp, height = 170.dp, estimatedPillRows = 6, hasBuffer = true)
        assertIs<WorkspacePlan.Side>(plan)
        assertTrue(plan.commandWidth >= 240.dp)
        assertTrue(720.dp - plan.commandWidth - 8.dp >= 280.dp)
    }

    @Test
    fun noBufferMeansCommandOnly() {
        assertEquals(WorkspacePlan.CommandOnly, planWorkspace(360.dp, 400.dp, 5, hasBuffer = false))
    }
}
