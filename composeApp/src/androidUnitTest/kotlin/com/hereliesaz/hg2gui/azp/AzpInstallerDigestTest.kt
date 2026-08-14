package com.hereliesaz.hg2gui.azp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [filesMatchDigests] is the integrity check [AzpInstaller.install] runs before ever trusting a
 * package's payload - see the class doc on AzpInstaller.kt for why this matters specifically for
 * `skill` packages (SKILL.md content lands directly in the AI system prompt). These cases are the
 * ones a real attack would actually try.
 */
class AzpInstallerDigestTest {

    private val digestA = "a".repeat(64)
    private val digestB = "b".repeat(64)

    @Test
    fun matchingDigests_pass() {
        val extracted = mapOf("skills/foo/SKILL.md" to digestA)
        val declared = mapOf("skills/foo/SKILL.md" to digestA)
        assertTrue(filesMatchDigests(extracted, declared))
    }

    @Test
    fun tamperedPayload_fails() {
        // Same declared digest as a real install, but the bytes on disk hash differently - a
        // SKILL.md swapped after signing, which is exactly the attack this check exists for.
        val extracted = mapOf("skills/foo/SKILL.md" to digestB)
        val declared = mapOf("skills/foo/SKILL.md" to digestA)
        assertFalse(filesMatchDigests(extracted, declared))
    }

    @Test
    fun unlistedPayload_fails() {
        // A file present in the archive that the manifest never hashed at all - nothing vouches
        // for it, so it must be rejected even though no *declared* digest was violated.
        val extracted = mapOf(
            "skills/foo/SKILL.md" to digestA,
            "skills/foo/scripts/evil.sh" to digestB,
        )
        val declared = mapOf("skills/foo/SKILL.md" to digestA)
        assertFalse(filesMatchDigests(extracted, declared))
    }

    @Test
    fun manifestAndSignature_areExempt() {
        // manifest.json and signature.json are never in manifest.files (the manifest doesn't
        // digest itself, and the signature is the detached signature, not a signed payload file)
        // - both must be ignored by this check, exactly like @azphalt/azp's verifyAzp.
        val extracted = mapOf(
            "manifest.json" to digestA,
            "signature.json" to digestB,
            "skills/foo/SKILL.md" to digestA,
        )
        val declared = mapOf("skills/foo/SKILL.md" to digestA)
        assertTrue(filesMatchDigests(extracted, declared))
    }

    @Test
    fun caseInsensitiveHexComparison_stillMatches() {
        // manifest.files digests are lowercase hex by convention (package-format.md), but the
        // check itself shouldn't be sensitive to case - it compares digests, not string literals.
        val extracted = mapOf("assets/x.cube" to digestA.uppercase())
        val declared = mapOf("assets/x.cube" to digestA)
        assertTrue(filesMatchDigests(extracted, declared))
    }

    @Test
    fun emptyPackage_passesVacuously() {
        assertTrue(filesMatchDigests(emptyMap(), emptyMap()))
    }

    @Test
    fun missingDeclaredFile_fails() {
        // manifest.files vouches for a file that was deleted from the archive before signing (or
        // stripped afterward) - the old one-directional check only ever walked what's present,
        // so it never noticed a declared file's simple absence.
        val extracted = mapOf("skills/foo/SKILL.md" to digestA)
        val declared = mapOf(
            "skills/foo/SKILL.md" to digestA,
            "skills/foo/scripts/setup.sh" to digestB,
        )
        assertFalse(filesMatchDigests(extracted, declared))
    }
}
