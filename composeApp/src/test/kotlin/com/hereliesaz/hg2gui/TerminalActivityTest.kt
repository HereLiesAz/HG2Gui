package com.hereliesaz.hg2gui

import androidx.biometric.BiometricPrompt
import com.hereliesaz.hg2gui.azp.AzpTrust
import com.hereliesaz.hg2gui.azp.ScriptInstaller
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalActivityTest {

    @Test
    fun bootstrapDownloadFraction_parsesAMatchingLine() {
        assertEquals(0.25f, bootstrapDownloadFraction("Downloaded: 10MB / 40MB"))
    }

    @Test
    fun bootstrapDownloadFraction_coercesAnOvershootDownloadToOne() {
        // A chunk can land right after the total was already reported reached - the fraction
        // still has to read as "done", not a value over 100%.
        assertEquals(1f, bootstrapDownloadFraction("Downloaded: 41MB / 40MB"))
    }

    @Test
    fun bootstrapDownloadFraction_returnsNullForAnUnknownTotal() {
        // The server didn't report Content-Length - see DistroManager.bootstrap's own "?" case.
        assertNull(bootstrapDownloadFraction("Downloaded: 10MB / ?"))
    }

    @Test
    fun bootstrapDownloadFraction_returnsNullForUnrelatedOutput() {
        assertNull(bootstrapDownloadFraction("Starting bootstrap process..."))
        assertNull(bootstrapDownloadFraction("Bootstrap successful! You can now use 'apt', 'pkg', and real coreutils."))
    }

    @Test
    fun appendGuideCommand_intoAnEmptyCompositionBecomesTheToken() {
        assertEquals(GuideAppend(listOf("chown"), ""), appendGuideCommand(emptyList(), "", "chown"))
        assertEquals(GuideAppend(listOf("chown"), ""), appendGuideCommand(emptyList(), "   ", "  chown "))
    }

    @Test
    fun appendGuideCommand_afterTypedTextKeepsTheTextAndSeparatesWithASpace() {
        assertEquals(GuideAppend(emptyList(), "ls -l chown"), appendGuideCommand(emptyList(), "ls -l ", "chown"))
    }

    @Test
    fun appendGuideCommand_afterPillTokensGoesIntoTheTypedText() {
        assertEquals(GuideAppend(listOf("git", "log"), "chown"), appendGuideCommand(listOf("git", "log"), "", "chown"))
        assertEquals(GuideAppend(listOf("git"), "log --oneline chown"), appendGuideCommand(listOf("git"), "log --oneline", "chown"))
    }

    @Test
    fun appendGuideCommand_ignoresABlankCommand() {
        assertEquals(GuideAppend(listOf("a"), "b"), appendGuideCommand(listOf("a"), "b", "  "))
    }

    @Test
    fun describeScriptResult_isSilentOnlyOnSuccess() {
        assertNull(describeScriptResult("tool", ScriptInstaller.Result.Installed("tool")))
        assertNotNull(describeScriptResult("tool", ScriptInstaller.Result.BootstrapMissing))
        assertNotNull(describeScriptResult("tool", ScriptInstaller.Result.Rejected))
    }

    @Test
    fun describeScriptResult_namesTheDependencyProblem() {
        val invalid = describeScriptResult("tool", ScriptInstaller.Result.InvalidDependency("-rf"))!!
        assertTrue(invalid.contains("-rf"))
        val declined = describeScriptResult("tool", ScriptInstaller.Result.DependenciesNotConfirmed(listOf("git", "curl")))!!
        assertTrue(declined.contains("git, curl"))
    }

    @Test
    fun describeScriptResult_quotesTheLastLineOfAFailedDependencyInstall() {
        val message = describeScriptResult("tool", ScriptInstaller.Result.DependencyFailed("fetching...\nE: Unable to locate package foo\n\n"))!!
        assertTrue(message.endsWith("E: Unable to locate package foo"))
        assertEquals(
            "tool was saved, but installing its dependencies failed.",
            describeScriptResult("tool", ScriptInstaller.Result.DependencyFailed("   \n"))
        )
    }

    @Test
    fun describeUnverifiedSkill_onlyTrustedIsSilent() {
        assertNull(describeUnverifiedSkill("s", AzpTrust.TRUSTED))
        for (trust in AzpTrust.entries.filter { it != AzpTrust.TRUSTED }) {
            assertNotNull(trust.name, describeUnverifiedSkill("s", trust))
        }
    }

    @Test
    fun isUserDismissal_coversCancelButNotRealErrors() {
        assertTrue(isUserDismissal(BiometricPrompt.ERROR_NEGATIVE_BUTTON))
        assertTrue(isUserDismissal(BiometricPrompt.ERROR_USER_CANCELED))
        assertTrue(isUserDismissal(BiometricPrompt.ERROR_CANCELED))
        assertFalse(isUserDismissal(BiometricPrompt.ERROR_NO_BIOMETRICS))
        assertFalse(isUserDismissal(BiometricPrompt.ERROR_LOCKOUT))
    }
}
