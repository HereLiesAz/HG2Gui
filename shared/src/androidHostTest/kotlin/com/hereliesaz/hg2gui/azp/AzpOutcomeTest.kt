package com.hereliesaz.hg2gui.azp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure pieces behind the typed download/install outcomes - see AzpOutcome.kt. */
class AzpOutcomeTest {

    @Test
    fun httpFailureMessages_nameTheCause() {
        assertTrue(describeDownloadHttpFailure(402).contains("paid", ignoreCase = true))
        assertTrue(describeDownloadHttpFailure(404).contains("404"))
        assertTrue(describeDownloadHttpFailure(302).contains("redirect", ignoreCase = true))
        assertTrue(describeDownloadHttpFailure(503).contains("503"))
        assertTrue(describeDownloadHttpFailure(418).contains("418"))
    }

    @Test
    fun digestProblem_nullWhenEverythingMatches() {
        val extracted = mapOf("a.txt" to "AA", "manifest.json" to "x", "signature.json" to "y")
        val declared = mapOf("a.txt" to "aa")
        assertNull(describeDigestProblem(extracted, declared))
        assertTrue(filesMatchDigests(extracted, declared))
    }

    @Test
    fun digestProblem_namesTheMismatchedFile() {
        val message = describeDigestProblem(mapOf("skills/x/SKILL.md" to "11"), mapOf("skills/x/SKILL.md" to "22"))
        assertNotNull(message)
        assertTrue(message!!.contains("skills/x/SKILL.md"))
        assertTrue(message.contains("digest"))
    }

    @Test
    fun digestProblem_namesAnUnlistedPayloadFile() {
        val message = describeDigestProblem(mapOf("evil.sh" to "11"), emptyMap())
        assertTrue(message!!.contains("evil.sh"))
        assertTrue(message.contains("not listed"))
    }

    @Test
    fun digestProblem_namesADeclaredButMissingFile() {
        val message = describeDigestProblem(emptyMap(), mapOf("gone.txt" to "11"))
        assertTrue(message!!.contains("gone.txt"))
        assertTrue(message.contains("missing"))
    }

    @Test
    fun digestProblem_isDeterministicAcrossInsertionOrder() {
        val a = describeDigestProblem(linkedMapOf("b" to "1", "a" to "1"), emptyMap())
        val b = describeDigestProblem(linkedMapOf("a" to "1", "b" to "1"), emptyMap())
        assertEquals(a, b)
        assertTrue(a!!.contains("\"a\""))
    }

    @Test
    fun classifyInstallError_mapsExceptionTypes() {
        assertEquals(
            AzpFailureKind.ARCHIVE_UNREADABLE,
            classifyInstallError(java.util.zip.ZipException("bad header")).kind
        )
        assertEquals(AzpFailureKind.STORAGE, classifyInstallError(java.io.IOException("disk full")).kind)
        assertEquals(AzpFailureKind.MANIFEST_INVALID, classifyInstallError(IllegalArgumentException("x")).kind)
        assertEquals(AzpFailureKind.STORAGE, classifyInstallError(IllegalStateException("x")).kind)
    }

    @Test
    fun classifyInstallError_messageCarriesTheDetail() {
        assertTrue(classifyInstallError(java.io.IOException("disk full")).message.contains("disk full"))
    }

    @Test
    fun outcomes_areDistinguishableWithoutParsingText() {
        val failed: AzpInstallOutcome = AzpInstallOutcome.Failed(AzpFailure(AzpFailureKind.SIGNATURE_INVALID, "bad"))
        val installed: AzpInstallOutcome =
            AzpInstallOutcome.Installed(AzpInstallResult("skill", listOf("s"), AzpTrust.TRUSTED))
        assertFalse(failed is AzpInstallOutcome.Installed)
        assertEquals(AzpFailureKind.SIGNATURE_INVALID, (failed as AzpInstallOutcome.Failed).failure.kind)
        assertEquals(AzpTrust.TRUSTED, (installed as AzpInstallOutcome.Installed).result.trust)
    }
}
