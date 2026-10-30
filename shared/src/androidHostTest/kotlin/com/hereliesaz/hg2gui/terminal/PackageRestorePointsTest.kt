package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageRestorePointsTest {

    private class Fixture(val root: File) {
        val home = File(root, "home").apply { mkdirs() }
        val prefix = File(root, "usr").apply { mkdirs() }
        val points = File(root, "points/pkg_demo")
        val allow: (File) -> Boolean = {
            PackageRestorePoints.isAllowedTarget(it, isolated = false, isolationBase = null, home = home, prefix = prefix)
        }

        fun create(vararg targets: File, version: String = "1.0") =
            PackageRestorePoints.createPoint(points, targets.toList(), version, isolated = false, isAllowed = allow)

        fun restore(id: String, allowed: (File) -> Boolean = allow) =
            PackageRestorePoints.restorePoint(points, id, "demo", isolated = false, isAllowed = allowed)
    }

    @Test
    fun createThenRestore_bringsBackTheSnapshotAndDropsLaterFiles() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val state = File(fx.home, ".config/demo")
            state.write("settings.json", "v1")
            state.write("sub/data", "data-v1")

            val point = fx.create(state)
            assertEquals(1, point.pathCount)
            assertEquals(1, PackageRestorePoints.listPoints(fx.points).size)

            state.write("settings.json", "v2-corrupt")
            state.write("new-file", "created later")
            File(state, "sub/data").delete()

            val result = fx.restore(point.id)

            assertEquals(emptyList<String>(), result.failed)
            assertEquals(1, result.restoredPaths)
            assertEquals("v1", File(state, "settings.json").readText())
            assertEquals("data-v1", File(state, "sub/data").readText())
            assertFalse(File(state, "new-file").exists())
            assertEquals("no staging/aside leftovers", listOf("demo"), state.parentFile.list()!!.toList())
        }
    }

    @Test
    fun restore_ofATargetThatIsNoLongerAllowed_failsWithoutDeletingIt() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val state = File(fx.home, ".cache/demo")
            state.write("a", "keep me")
            val point = fx.create(state)
            state.write("a", "current")

            val result = fx.restore(point.id) { false }

            assertEquals(listOf(state.canonicalPath), result.failed)
            assertEquals("current", File(state, "a").readText())
        }
    }

    @Test
    fun create_refusesTargetsOutsideTheManagedAreas_andLeavesNoStagingDir() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val outside = File(dir, "elsewhere").also { it.write("secret", "x") }
            assertThrows(IllegalArgumentException::class.java) { fx.create(outside) }
            assertEquals(emptyList<String>(), fx.points.list().orEmpty().toList())
        }
    }

    @Test
    fun create_refusesWhenThereIsNotEnoughFreeSpace() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val state = File(fx.home, ".config/big").also { it.write("blob", "x".repeat(50_000)) }
            val error = assertThrows(IllegalArgumentException::class.java) {
                PackageRestorePoints.createPoint(
                    fx.points, listOf(state), "1", isolated = false, isAllowed = fx.allow, usableSpace = { 0L }
                )
            }
            assertTrue(error.message, error.message!!.contains("free storage"))
            assertEquals(emptyList<String>(), fx.points.list().orEmpty().toList())
        }
    }

    @Test
    fun onlyTheThreeNewestPointsAreKept() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val state = File(fx.home, ".config/demo").also { it.write("a", "1") }
            val ids = (1..5).map { fx.create(state, version = "v$it").also { Thread.sleep(5) }.id }
            val kept = PackageRestorePoints.listPoints(fx.points).map { it.id }
            assertEquals(ids.takeLast(3).reversed(), kept)
        }
    }

    @Test
    fun idsThatCouldEscapeTheRestoreDirectoryAreRejected() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            assertThrows(IllegalArgumentException::class.java) { fx.restore("../../etc") }
            assertFalse(PackageRestorePoints.deletePoint(fx.points, "../x"))
        }
    }

    @Test
    fun delete_removesThePoint() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val state = File(fx.home, ".config/demo").also { it.write("a", "1") }
            val id = fx.create(state).id
            assertTrue(PackageRestorePoints.deletePoint(fx.points, id))
            assertEquals(emptyList<Any>(), PackageRestorePoints.listPoints(fx.points))
        }
    }

    @Test
    fun manifestsWrittenWithAndroidBase64AreStillReadable() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val point = File(fx.points, "abcd-1234").apply { mkdirs() }
            val enc = java.util.Base64.getEncoder()
            File(point, "manifest.tsv").writeText(
                "timestamp\t1700000000000\nversion\t${enc.encodeToString("2.5-é".toByteArray())}\nisolated\tfalse\n" +
                    "entry\t0\t42\t${enc.encodeToString("/h/.config/demo".toByteArray())}\n"
            )
            val listed = PackageRestorePoints.listPoints(fx.points).single()
            assertEquals("2.5-é", listed.packageVersion)
            assertEquals(42L, listed.bytes)
        }
    }

    // --- target guard ------------------------------------------------------------------------

    @Test
    fun isAllowedTarget_acceptsOnlyPathsStrictlyInsideHomeOrPrefixVarAreas() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val a = fx.allow
            assertTrue(a(File(fx.home, ".config/x")))
            assertTrue(a(File(fx.prefix, "var/cache/x")))
            assertTrue(a(File(fx.prefix, "var/log/x")))
            assertTrue(a(File(fx.prefix, "var/tmp/x")))
            assertFalse("home itself", a(fx.home))
            assertFalse("prefix var itself", a(File(fx.prefix, "var")))
            assertFalse(a(File(fx.prefix, "var/lib/dpkg")))
            assertFalse(a(File(fx.prefix, "bin")))
        }
    }

    @Test
    fun isAllowedTarget_canonicalisesDotDotAndSymlinks() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            File(fx.home, ".cache").mkdirs()
            // `home/.cache/..` is home itself - the traversal the audit found in reset (M-3).
            assertFalse(fx.allow(File(fx.home, ".cache/..")))
            assertFalse(fx.allow(File(fx.prefix, "var/cache/../lib")))

            val outside = File(dir, "outside").apply { mkdirs() }
            Files.createSymbolicLink(File(fx.home, "escape").toPath(), outside.toPath())
            assertFalse(fx.allow(File(fx.home, "escape")))
        }
    }

    @Test
    fun isAllowedTarget_inIsolatedModeIsExactlyTheIsolationBase() {
        withHg2TempDir { dir ->
            val base = File(dir, "package-isolation/pkg_demo").apply { mkdirs() }
            fun allowed(f: File) = PackageRestorePoints.isAllowedTarget(f, true, base, File(dir, "home"), File(dir, "usr"))
            assertTrue(allowed(base))
            assertFalse(allowed(File(base, "root")))
            assertFalse(allowed(base.parentFile))
            assertFalse(PackageRestorePoints.isAllowedTarget(base, true, null, File(dir, "home"), File(dir, "usr")))
        }
    }
}
