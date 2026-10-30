package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPOutputStream
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxRepositoryClientTest {

    private class Fixture(val root: File) {
        val prefix = File(root, "usr").apply { mkdirs() }
        val home = File(root, "home").apply { mkdirs() }
        val trust = File(root, "trust")
        val state = File(prefix, "var/lib/hg2pkg").apply { mkdirs() }
        val keyrings = File(prefix, "share/termux-keyring").apply { mkdirs() }

        fun client() = TermuxRepositoryClient(prefix, home, trust, OkHttpClient())

        fun sha(file: File) = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

        fun gz(name: String, text: String): File {
            val file = File(root, name)
            GZIPOutputStream(file.outputStream()).use { it.write(text.toByteArray()) }
            return file
        }

        fun index(text: String = "Package: a\n\n") =
            TermuxRepositoryClient.IndexResult("https://mirror.example/termux-main", gz("Packages.gz", text), "")
    }

    // --- cache authenticity ---------------------------------------------------------------

    @Test
    fun init_discardsAPackagesCacheThatHasNoAuthenticityMarker() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val packages = fx.state.write("Packages", "Package: legacy\n\n")
            fx.client()
            assertFalse(packages.exists())
        }
    }

    @Test
    fun init_discardsAPackagesCacheWhoseDigestNoLongerMatchesTheMarker() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.client().installAuthenticatedIndex("aarch64", fx.index("Package: a\n\n"))
            File(fx.state, "Packages").appendText("Package: injected\n\n")
            fx.client()
            assertFalse(File(fx.state, "Packages").exists())
            assertFalse(File(fx.state, "Packages.authenticated").exists())
        }
    }

    @Test
    fun installAuthenticatedIndex_promotesAndMarksInOneStep_thatASecondClientAccepts() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.client().installAuthenticatedIndex("aarch64", fx.index("Package: a\nVersion: 1\n\n"))

            val packages = File(fx.state, "Packages")
            assertEquals("Package: a\nVersion: 1\n\n", packages.readText())
            val marker = File(fx.state, "Packages.authenticated").readLines()
            assertEquals(listOf("aarch64", "https://mirror.example/termux-main", fx.sha(packages)), marker)

            fx.client() // a second tab constructing a client must keep the fresh index
            assertTrue(packages.isFile)
            assertFalse(File(fx.state, "Packages.tmp").exists())
        }
    }

    @Test
    fun constructingClientsConcurrentlyWithAPromote_neverDeletesTheFreshIndex() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val indexResult = fx.index("Package: a\nVersion: 1\n\n")
            val writer = fx.client()
            val failure = AtomicReference<Throwable?>(null)
            val pool = Executors.newFixedThreadPool(4)
            val start = CountDownLatch(1)
            val done = CountDownLatch(4)
            repeat(3) {
                pool.execute {
                    try {
                        start.await()
                        repeat(300) { fx.client() }
                    } catch (t: Throwable) {
                        failure.compareAndSet(null, t)
                    } finally {
                        done.countDown()
                    }
                }
            }
            pool.execute {
                try {
                    start.await()
                    repeat(300) { writer.installAuthenticatedIndex("aarch64", indexResult) }
                } catch (t: Throwable) {
                    failure.compareAndSet(null, t)
                } finally {
                    done.countDown()
                }
            }
            start.countDown()
            assertTrue(done.await(60, TimeUnit.SECONDS))
            pool.shutdown()
            assertNull("no promote/marker write may fail under concurrent construction: ${failure.get()}", failure.get())
            fx.client()
            assertTrue("the last promoted index survives", File(fx.state, "Packages").isFile)
        }
    }

    // --- trust-root provenance ------------------------------------------------------------

    @Test
    fun repositoryKeyrings_trustsOnlyPinnedOrProvenanceRecordedFiles() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.keyrings.write("planted.gpg", "attacker key")
            assertEquals(
                "a file merely sitting in the prefix is not a trust root",
                emptyList<String>(),
                fx.client().repositoryKeyrings().map { it.name }
            )

            // termux-keyring was installed through an authenticated index: its files become trusted.
            fx.keyrings.write("rotated.gpg", "new key")
            fx.client().recordKeyringProvenance()
            // ...but a file added afterwards is not.
            fx.keyrings.write("later.gpg", "late key")

            val names = fx.client().repositoryKeyrings().map { it.name }
            assertEquals(listOf("planted.gpg", "rotated.gpg"), names)
        }
    }

    @Test
    fun repositoryKeyrings_neverUsesThePacmanKeyring() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.keyrings.write("termux-pacman.gpg", "pacman key")
            fx.client().recordKeyringProvenance()
            assertEquals(emptyList<String>(), fx.client().repositoryKeyrings().map { it.name })
        }
    }

    @Test
    fun aKeyringThatChangesAfterItWasRecorded_isNoLongerTrusted() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val key = fx.keyrings.write("k.gpg", "good")
            fx.client().recordKeyringProvenance()
            key.writeText("tampered")
            assertEquals(emptyList<String>(), fx.client().repositoryKeyrings().map { it.name })
        }
    }

    @Test
    fun pinnedKeyringDigests_areWellFormedAndDistinct() {
        val pinned = TermuxRepositoryClient.PINNED_KEYRING_SHA256
        assertEquals(9, pinned.size)
        assertTrue(pinned.all { Regex("[0-9a-f]{64}").matches(it) })
    }

    // --- package URLs ---------------------------------------------------------------------

    @Test
    fun packageUrls_refusePlainHttpAndPassHttpsThrough() {
        withHg2TempDir { dir ->
            val client = Fixture(dir).client()
            assertThrows(IllegalStateException::class.java) { client.packageUrls("http://evil.example/x.deb", null) }
            assertEquals(listOf("https://cdn.example/x.deb"), client.packageUrls("https://cdn.example/x.deb", null))
        }
    }

    @Test
    fun packageUrls_tryThePreferredMirrorFirst() {
        withHg2TempDir { dir ->
            val client = Fixture(dir).client()
            val preferred = TermuxRepositoryClient.MAIN_MIRRORS.last()
            val urls = client.packageUrls("/pool/main/a/a.deb", preferred)
            assertEquals("$preferred/pool/main/a/a.deb", urls.first())
            assertEquals(TermuxRepositoryClient.MAIN_MIRRORS.size, urls.size)
        }
    }

    // --- freshness (Valid-Until handling) -----------------------------------------------------

    private val aug16 = parseReleaseDate("Sun, 16 Aug 2026 12:00:00 UTC")!!
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun withoutValidUntil_aFrozenOldSnapshotIsRejectedPastTheMaxAge() {
        val release = "Date: Sun, 16 Aug 2026 12:00:00 UTC\nSHA256:\n abc 1 main/x"
        assertThrows(IllegalStateException::class.java) {
            checkReleaseFreshness(release, aug16 + 200 * day, null, maxAgeWithoutValidUntilMillis = 180 * day)
        }
        // inside the window it passes, and without a configured bound behaviour is unchanged
        assertEquals(aug16, checkReleaseFreshness(release, aug16 + 100 * day, null, maxAgeWithoutValidUntilMillis = 180 * day))
        assertEquals(aug16, checkReleaseFreshness(release, aug16 + 400 * day, null))
    }

    @Test
    fun aPresentValidUntil_replacesTheMaxAgeBound() {
        val release = "Date: Sun, 16 Aug 2026 12:00:00 UTC\nValid-Until: Sun, 16 Aug 2027 12:00:00 UTC\nSHA256:\n abc 1 main/x"
        assertEquals(aug16, checkReleaseFreshness(release, aug16 + 300 * day, null, maxAgeWithoutValidUntilMillis = 180 * day))
    }

    @Test
    fun theReplayFloor_isBeforeThePinnedBootstrapAndAfterTheEpoch() {
        val floor = TermuxRepositoryClient.FLOOR_RELEASE_DATE_MILLIS
        assertTrue(floor < aug16)
        assertEquals(parseReleaseDate("Mon, 01 Jun 2026 00:00:00 UTC"), floor)
    }
}
