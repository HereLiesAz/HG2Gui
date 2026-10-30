package com.hereliesaz.hg2gui.azp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which trust levels may reach the AI system prompt, and the trusted-key comparison. */
class AzpTrustTest {

    @Test
    fun onlyTrustedReachesTheAiPrompt() {
        for (trust in AzpTrust.entries) {
            assertEquals(trust.name, trust == AzpTrust.TRUSTED, trust.reachesAiPrompt)
        }
    }

    @Test
    fun selfSignedAndUnsignedNeverReachThePrompt() {
        assertFalse(AzpTrust.VALID.reachesAiPrompt)
        assertFalse(AzpTrust.UNSIGNED.reachesAiPrompt)
        assertFalse(AzpTrust.UNVERIFIABLE.reachesAiPrompt)
        assertFalse(AzpTrust.UNCHECKED.reachesAiPrompt)
        assertFalse(AzpTrust.INVALID.reachesAiPrompt)
        assertTrue(AzpTrust.TRUSTED.reachesAiPrompt)
    }

    @Test
    fun storedTrustParsing_unknownOrMissingIsUncheckedNeverTrusted() {
        assertEquals(AzpTrust.UNCHECKED, AzpTrust.fromStored(null))
        assertEquals(AzpTrust.UNCHECKED, AzpTrust.fromStored(""))
        assertEquals(AzpTrust.UNCHECKED, AzpTrust.fromStored("trusted")) // case matters
        assertEquals(AzpTrust.UNCHECKED, AzpTrust.fromStored("SUPER_TRUSTED"))
        assertEquals(AzpTrust.TRUSTED, AzpTrust.fromStored("TRUSTED"))
        assertEquals(AzpTrust.VALID, AzpTrust.fromStored("VALID"))
    }

    @Test
    fun everyTrustRoundTripsThroughItsStoredName() {
        for (trust in AzpTrust.entries) assertEquals(trust, AzpTrust.fromStored(trust.name))
    }

    // ---- n-4: trusted key comparison -----------------------------------------------------

    private val key = "MCowBQYDK2VwAyEAabcdefghijklmnopqrstuvwxyz0123456789+/ABCDE="

    @Test
    fun trustedKey_exactMatch() {
        assertTrue(AzpSignatureVerifier.isTrustedKey(key, listOf(key)))
    }

    @Test
    fun trustedKey_ignoresWhitespaceAndLineWrapping() {
        val wrapped = key.chunked(16).joinToString("\r\n") + "\n"
        assertTrue(AzpSignatureVerifier.isTrustedKey(key, listOf(wrapped)))
        assertTrue(AzpSignatureVerifier.isTrustedKey(" $key ", listOf(key)))
        assertTrue(AzpSignatureVerifier.isTrustedKey(wrapped, listOf(key)))
    }

    @Test
    fun trustedKey_ignoresPaddingAndUrlSafeAlphabet() {
        val unpadded = key.trimEnd('=')
        val urlSafe = key.replace('+', '-').replace('/', '_')
        assertTrue(AzpSignatureVerifier.isTrustedKey(unpadded, listOf(key)))
        assertTrue(AzpSignatureVerifier.isTrustedKey(key, listOf(urlSafe)))
    }

    @Test
    fun trustedKey_differentKeyIsNotTrusted() {
        val other = key.replace("abcdef", "abcdeg")
        assertFalse(AzpSignatureVerifier.isTrustedKey(other, listOf(key)))
        assertFalse(AzpSignatureVerifier.isTrustedKey(key, emptyList()))
    }

    @Test
    fun trustedKey_caseIsSignificantBecauseBase64IsCaseSensitive() {
        assertFalse(AzpSignatureVerifier.isTrustedKey(key.lowercase(), listOf(key)))
    }

    @Test
    fun trustedKey_blankNeverMatches() {
        assertFalse(AzpSignatureVerifier.isTrustedKey("", listOf("")))
        assertFalse(AzpSignatureVerifier.isTrustedKey("  \n", listOf(" ", "")))
        assertFalse(AzpSignatureVerifier.isTrustedKey("==", listOf("=")))
    }
}
