package com.hereliesaz.hg2gui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Channel naming and version/identity/signature decisions of the in-app updater: see UpdatePolicy.kt. */
class AppUpdatePolicyTest {
    private val digest = "sha256:" + "ab".repeat(32)

    private fun asset(name: String, url: String = "https://example.test/$name", digest: String? = null) =
        ReleaseAsset(name, url, digest)

    // --- channel: release builds only (M-9) ---------------------------------------------------

    @Test
    fun releaseTag_isTheReleaseChannelNotTheDebugOne() {
        assertEquals("latest-v0.7", updateReleaseTag("0.7"))
        assertFalse(updateReleaseTag(AppUpdateChecker.releaseSeries("0.7.474.745")).contains("debug"))
    }

    @Test
    fun debugApksAreNeverOffered() {
        val assets = listOf(
            asset("hg2gui-playstore-debug-0.7.474.9000.apk"),
            asset("hg2gui-playstore-debug-latest.apk"),
        )
        assertNull(newestUpdate(assets, installedCode = 1))
    }

    @Test
    fun onlyReleaseApksMatchTheNamePattern() {
        assertTrue(UPDATE_APK_NAME.matches("hg2gui-playstore-release-0.7.474.745.apk"))
        assertFalse(UPDATE_APK_NAME.matches("hg2gui-playstore-debug-0.7.474.745.apk"))
        // The fixed "-latest" alias the pipeline also uploads has no version to compare.
        assertFalse(UPDATE_APK_NAME.matches("hg2gui-playstore-release-latest.apk"))
        assertFalse(UPDATE_APK_NAME.matches("hg2gui-playstore-release-0.7.474.745.apk.sha256"))
        assertFalse(UPDATE_APK_NAME.matches("other-playstore-release-0.7.474.745.apk"))
    }

    // --- version selection -------------------------------------------------------------------

    @Test
    fun picksTheHighestBuildNumberNewerThanInstalled() {
        val assets = listOf(
            asset("hg2gui-playstore-release-0.7.470.700.apk"),
            asset("hg2gui-playstore-release-0.7.474.745.apk", digest = digest),
            asset("hg2gui-playstore-release-0.7.471.9.apk"),
            asset("hg2gui-playstore-release-latest.apk"),
        )
        val update = newestUpdate(assets, installedCode = 600)!!
        assertEquals("0.7.474.745", update.versionName)
        assertEquals(745L, update.versionCode)
        assertEquals("https://example.test/hg2gui-playstore-release-0.7.474.745.apk", update.downloadUrl)
        assertEquals("ab".repeat(32), update.sha256)
    }

    @Test
    fun nothingNewerMeansNoUpdate() {
        val assets = listOf(asset("hg2gui-playstore-release-0.7.474.745.apk"))
        assertNull(newestUpdate(assets, installedCode = 745))
        assertNull(newestUpdate(assets, installedCode = 746))
        assertNull(newestUpdate(emptyList(), installedCode = 1))
    }

    @Test
    fun assetsWithoutAUrlAreSkipped() {
        val assets = listOf(
            asset("hg2gui-playstore-release-0.7.474.745.apk", url = ""),
            asset("hg2gui-playstore-release-0.7.apk"),
        )
        // "0.7" has build number 7 (last dot-separated field): still a valid candidate, but only if newer.
        assertEquals(7L, newestUpdate(assets, installedCode = 1)?.versionCode)
        assertNull(newestUpdate(assets, installedCode = 7))
    }

    @Test
    fun digestIsOnlyTrustedWhenWellFormed() {
        assertEquals("ab".repeat(32), sha256FromDigest(digest))
        assertEquals("ab".repeat(32), sha256FromDigest("SHA256:" + "ab".repeat(32)))
        assertNull(sha256FromDigest(null))
        assertNull(sha256FromDigest(""))
        assertNull(sha256FromDigest("sha1:" + "ab".repeat(20)))
        assertNull(sha256FromDigest("sha256:abcd"))
    }

    // --- archive identity (kept from the original verifier) ----------------------------------

    @Test
    fun identity_acceptsASameAppNewerAdvertisedBuild() {
        assertNull(archiveIdentityError("com.x", "com.x", archiveCode = 745, advertisedCode = 745, installedCode = 744))
    }

    @Test
    fun identity_rejectsWrongPackageMismatchedCodeAndNonUpgrade() {
        assertEquals(
            "Update APK belongs to com.evil, not com.x",
            archiveIdentityError("com.evil", "com.x", 745, 745, 744)
        )
        assertEquals(
            "Update APK version code 900 does not match release 745",
            archiveIdentityError("com.x", "com.x", 900, 745, 744)
        )
        assertEquals(
            "Downloaded APK is not newer than the installed app",
            archiveIdentityError("com.x", "com.x", 744, 744, 744)
        )
        assertEquals(
            "Downloaded APK is not newer than the installed app",
            archiveIdentityError("com.x", "com.x", 700, 700, 744)
        )
    }

    // --- signer comparison -------------------------------------------------------------------

    @Test
    fun signers_mustMatchExactly() {
        assertTrue(signersAcceptable(setOf("A"), setOf("A")))
        assertFalse(signersAcceptable(setOf("A"), setOf("B")))
        assertFalse(signersAcceptable(setOf("A"), setOf("A", "B")))
        assertFalse(signersAcceptable(setOf("A", "B"), setOf("A")))
    }

    @Test
    fun signers_unsignedArchiveIsAlwaysRejected() {
        assertFalse(signersAcceptable(setOf("A"), emptySet()))
        assertFalse(signersAcceptable(emptySet(), emptySet()))
    }

    @Test
    fun signers_unknownInstalledSetLeavesItToThePlatform() {
        assertTrue(signersAcceptable(emptySet(), setOf("A")))
    }

    // --- debuggable -----------------------------------------------------------------------------

    @Test
    fun debuggableFlagIsDetected() {
        val flagDebuggable = 1 shl 1 // android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
        assertTrue(isDebuggable(flagDebuggable))
        assertTrue(isDebuggable(flagDebuggable or 0x1))
        assertFalse(isDebuggable(0))
        assertFalse(isDebuggable(1 shl 0))
    }
}
