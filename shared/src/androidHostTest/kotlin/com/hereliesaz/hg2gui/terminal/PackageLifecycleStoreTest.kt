package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageLifecycleStoreTest {
    private fun pkg(manager: String = "pkg", name: String = "example", isolated: Boolean = false) =
        PackageLifecycleStore.InstalledPackage(manager, "label", name, "1.0", listOf(name), false, isolated)

    @Test
    fun updateCommand_perManager() {
        // M-4: `pkg install <installed name>` is a no-op, so a Termux update must be an upgrade.
        val cases = mapOf(
            "pkg" to "pkg upgrade 'example'",
            "pip" to "python -m pip install --upgrade 'example'",
            "pipx" to "pipx upgrade 'example'",
            "npm" to "npm update -g 'example'",
            "gem" to "gem update 'example'"
        )
        for ((manager, expected) in cases) assertEquals(manager, expected, PackageLifecycleStore.updateCommand(pkg(manager)))
        assertFalse(PackageLifecycleStore.updateCommand(pkg("pkg")).startsWith("pkg install"))
    }

    @Test
    fun updateCommand_quotesHostileNames() {
        assertEquals("pkg upgrade 'a'\\''b'", PackageLifecycleStore.updateCommand(pkg(name = "a'b")))
    }

    // -- M-3: Reset must never leave the package's state directories ------------------------------

    private class Dirs(val root: File = Files.createTempDirectory("hg2gui-reset").toFile()) {
        val home = File(root, "home").apply { mkdirs() }
        val prefix = File(root, "usr").apply { mkdirs() }
        fun cleanup() { root.deleteRecursively() }
    }

    private fun withDirs(block: (Dirs) -> Unit) {
        val dirs = Dirs()
        try { block(dirs) } finally { dirs.cleanup() }
    }

    @Test
    fun isSafeStateName_rejectsTraversalAndSeparators() {
        val bad = listOf(".", "..", "", "a", "a/b", "../x", "x\\y", "a\u0000b", "sh", "env", "test", "true")
        for (name in bad) assertFalse("'$name'", PackageLifecycleStore.isSafeStateName(name))
        val good = listOf("git", "python3", "node-gyp", "ripgrep", ".git", "a.b")
        for (name in good) assertTrue("'$name'", PackageLifecycleStore.isSafeStateName(name))
    }

    @Test
    fun resetTargets_neverIncludeAParentDirectory_whateverTheMetadataSays() = withDirs { d ->
        // npm bin keys / pip console scripts / package names come from package metadata.
        val hostile = listOf("..", ".", "@scope/..", "pkg/..", "../../..", "x/../..")
        File(d.home, ".cache/real").mkdirs()
        val targets = PackageLifecycleStore.resetTargets(d.home, d.prefix, hostile + "real", emptySet())
        for (target in targets) {
            val canonical = target.canonicalPath
            assertTrue("$canonical must be strictly inside a state directory", canonical != d.home.canonicalPath)
            assertFalse(canonical == File(d.prefix, "var").canonicalPath)
            assertTrue(canonical.startsWith(d.home.canonicalPath + "/") || canonical.startsWith(d.prefix.canonicalPath + "/var/"))
        }
        assertTrue(targets.any { it.name == "real" })
        assertTrue(targets.none { it.name == ".." || it.name == "." })
    }

    @Test
    fun resetTargets_dropsTrackedPathsOutsideHomeAndTheVarDirs() = withDirs { d ->
        val outside = File(d.root, "elsewhere").apply { mkdirs() }
        val inside = File(d.home, "proj/out.o").apply { parentFile.mkdirs(); writeText("x") }
        val tracked = setOf(
            outside.absolutePath, d.home.absolutePath, d.prefix.absolutePath, File(d.prefix, "var").absolutePath,
            File(d.prefix, "var/cache").absolutePath, File(d.home, "../escape").absolutePath, inside.absolutePath
        )
        val targets = PackageLifecycleStore.resetTargets(d.home, d.prefix, emptyList(), tracked)
        assertEquals(setOf(inside.absolutePath), targets.map { it.absolutePath }.toSet())
    }

    @Test
    fun isResettablePath_table() = withDirs { d ->
        val cache = File(d.prefix, "var/cache/x").apply { mkdirs() }
        val cases = listOf(
            File(d.home, ".cache/git") to true,
            File(d.home, "a/b/c") to true,
            cache to true,
            File(d.prefix, "var/log/git") to true,
            d.home to false,
            File(d.home, ".cache/..") to false,
            File(d.home, ".config/../..") to false,
            File(d.prefix, "var") to false,
            File(d.prefix, "var/cache") to false,
            File(d.prefix, "var/lib/dpkg") to false,
            File(d.prefix, "bin/bash") to false,
            File(d.home, ".ssh/id_ed25519") to false,
            File(d.home, ".gnupg") to false,
            File(d.root, "other") to false
        )
        for ((file, expected) in cases) {
            assertEquals(file.path, expected, PackageLifecycleStore.isResettablePath(d.home, d.prefix, file))
        }
    }

    @Test
    fun isResettablePath_followsSymlinksOutOfTheStateDir() = withDirs { d ->
        val outside = File(d.root, "sdcard").apply { mkdirs() }
        File(outside, "photo.jpg").writeText("precious")
        val link = File(d.home, ".cache/evil")
        link.parentFile.mkdirs()
        Files.createSymbolicLink(link.toPath(), outside.toPath())
        assertFalse(PackageLifecycleStore.isResettablePath(d.home, d.prefix, link))
        assertFalse(PackageLifecycleStore.isDirectChildOf(File(d.home, ".cache"), link))
    }

    @Test
    fun deleteTreeNoFollow_removesALinkWithoutTouchingItsTarget() = withDirs { d ->
        val outside = File(d.root, "sdcard").apply { mkdirs() }
        val keep = File(outside, "photo.jpg").apply { writeText("precious") }
        val state = File(d.home, ".cache/pkg").apply { mkdirs() }
        File(state, "file.bin").writeText("cache")
        Files.createSymbolicLink(File(state, "link").toPath(), outside.toPath())

        assertTrue(PackageLifecycleStore.deleteTreeNoFollow(state))
        assertFalse(state.exists())
        assertTrue("a symlink inside the tree must not be followed", keep.isFile)
    }

    @Test
    fun deleteTreeNoFollow_removesNestedDirectoriesAndFiles() = withDirs { d ->
        val root = File(d.home, ".cache/deep/a/b").apply { mkdirs() }
        File(root, "x").writeText("1")
        assertTrue(PackageLifecycleStore.deleteTreeNoFollow(File(d.home, ".cache/deep")))
        assertFalse(File(d.home, ".cache/deep").exists())
        assertTrue(File(d.home, ".cache").isDirectory)
    }

    @Test
    fun conventionalStatePaths_pairEachCandidateWithItsBase() = withDirs { d ->
        val pairs = PackageLifecycleStore.conventionalStatePaths(d.home, d.prefix, listOf("Git", "git", "a/b/tool"))
        assertEquals(setOf("git", "tool"), pairs.map { it.second.name }.toSet())
        assertEquals(14, pairs.size)
        assertTrue(pairs.all { (base, file) -> file.parentFile == base })
    }

    // -- m-15: tracking stays bounded ---------------------------------------------------------

    @Test
    fun topLevelOnly_keepsOnlyTheRootsOfWhatWasCreated() {
        assertEquals(
            listOf("a", "x/y"),
            PackageLifecycleStore.topLevelOnly(listOf("a/b/c", "a", "a/b", "x/y", "x/y/z", "ab")).filter { it != "ab" }
        )
        assertEquals(listOf("ab", "a"), PackageLifecycleStore.topLevelOnly(listOf("a", "ab")).reversed())
        assertEquals(emptyList<String>(), PackageLifecycleStore.topLevelOnly(emptyList()))
    }

    @Test
    fun capTracked_neverGrowsPastTheLimit_andKeepsWhatWasThere() {
        val existing = (1..PackageLifecycleStore.MAX_TRACKED_PATHS - 2).map { "/h/old$it" }.toSet()
        val added = (1..10).map { "/h/new$it" }.toSet()
        val merged = PackageLifecycleStore.capTracked(existing, added)
        assertEquals(PackageLifecycleStore.MAX_TRACKED_PATHS, merged.size)
        assertTrue(merged.containsAll(existing))
        val full = PackageLifecycleStore.capTracked(merged, setOf("/h/more"))
        assertEquals(merged, full)
        assertEquals(setOf("/h/a"), PackageLifecycleStore.capTracked(emptySet(), setOf("/h/a")))
    }

    @Test
    fun shouldTrack_skipsBaseUtilitiesAndBroadDirectories() = withDirs { d ->
        val work = File(d.home, "proj").apply { mkdirs() }
        assertTrue(PackageLifecycleStore.shouldTrack(pkg(name = "ripgrep"), work.path, d.home, d.prefix))
        for (name in listOf("coreutils", "grep", "findutils", "bash", "tar")) {
            assertFalse(name, PackageLifecycleStore.shouldTrack(pkg(name = name), work.path, d.home, d.prefix))
        }
        // Same package name under another manager is not the base utility.
        assertTrue(PackageLifecycleStore.shouldTrack(pkg(manager = "npm", name = "grep"), work.path, d.home, d.prefix))
        assertFalse(PackageLifecycleStore.shouldTrack(pkg(name = "ripgrep"), d.home.path, d.home, d.prefix))
        assertFalse(PackageLifecycleStore.shouldTrack(pkg(name = "ripgrep"), File(d.prefix, "bin").apply { mkdirs() }.path, d.home, d.prefix))
        assertFalse(PackageLifecycleStore.shouldTrack(pkg(name = "ripgrep"), "/", d.home, d.prefix))
    }
}
