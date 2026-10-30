package com.hereliesaz.hg2gui.managers

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * [VfsSandbox] is what the MCP server's vfs.* tools run on: pinned to one root, root-absolute
 * paths only, containment by canonical path. Plain java.io, so it runs on the JVM with temp dirs.
 */
class VfsSandboxTest {
    private lateinit var base: File
    private lateinit var root: File
    private lateinit var outside: File
    private lateinit var sandbox: VfsSandbox

    @Before
    fun setUp() {
        base = Files.createTempDirectory("vfs-sandbox-test").toFile().canonicalFile
        root = File(base, "home").apply { mkdirs() }
        outside = File(base, "outside").apply { mkdirs() }
        File(outside, "secret.txt").writeText("top secret")
        sandbox = VfsSandbox(root)
    }

    @After
    fun tearDown() {
        base.deleteRecursively()
    }

    @Test
    fun relativeAndAbsolutePathsBothMeanRootAbsolute() {
        File(root, "notes.txt").writeText("hi")
        assertEquals("hi", sandbox.readText("/notes.txt"))
        assertEquals("hi", sandbox.readText("notes.txt"))
        assertEquals(root, sandbox.resolve("/"))
        assertEquals(root, sandbox.resolve(""))
    }

    @Test
    fun dotDotCannotEscapeTheRoot() {
        assertNull(sandbox.resolve("/../outside/secret.txt"))
        assertNull(sandbox.readText("../outside/secret.txt"))
        assertNull(sandbox.resolve("/a/../../outside"))
        assertFalse(sandbox.writeText("/../outside/pwned.txt", "x"))
        assertFalse(File(outside, "pwned.txt").exists())
        // A sibling whose name merely starts with the root's name is outside too.
        File(base, "home-evil").mkdirs()
        assertNull(sandbox.resolve("/../home-evil"))
    }

    @Test
    fun symlinkOutOfTheRootIsRefused() {
        val link = File(root, "escape")
        try {
            Files.createSymbolicLink(link.toPath(), outside.toPath())
        } catch (e: Exception) {
            assumeTrue("symlinks unavailable here: ${e.message}", false)
        }
        assertNull(sandbox.resolve("/escape"))
        assertNull(sandbox.readText("/escape/secret.txt"))
        assertFalse(sandbox.writeText("/escape/pwned.txt", "x"))
        assertFalse(File(outside, "pwned.txt").exists())
    }

    @Test
    fun pinnedRootIgnoresEverythingElse() {
        // A second sandbox over another directory is entirely independent: the root is a constructor
        // argument, with no shared mutable state to flip (VfsManager's global root can be switched
        // to device storage by the Files UI; this type has no such switch).
        val other = VfsSandbox(outside)
        assertEquals("top secret", other.readText("/secret.txt"))
        assertNull(sandbox.readText("/secret.txt"))
    }

    @Test
    fun pathOfReportsRootRelativePaths() {
        val dir = File(root, "Downloads").apply { mkdirs() }
        val file = File(dir, "photo.png").apply { writeText("x") }
        assertEquals("/", sandbox.pathOf(root))
        assertEquals("/Downloads", sandbox.pathOf(dir))
        assertEquals("/Downloads/photo.png", sandbox.pathOf(file))
        // Something outside the root is reported as "/" rather than leaking its real location.
        assertEquals("/", sandbox.pathOf(File(outside, "secret.txt")))
    }

    @Test
    fun listChildrenHidesTrash_foldersFirst() {
        File(root, ".trash").mkdirs()
        File(root, "b.txt").writeText("")
        File(root, "A").mkdirs()
        File(root, "a.txt").writeText("")
        assertEquals(listOf("A", "a.txt", "b.txt"), sandbox.listChildren(root).map { it.name })
    }

    @Test
    fun writeReadMkdirMoveCopyDelete() {
        assertTrue(sandbox.mkdir("/proj/src"))
        assertTrue(sandbox.writeText("/proj/src/a.txt", "one"))
        assertTrue(sandbox.copy("/proj/src/a.txt", "/proj/b.txt"))
        assertEquals("one", sandbox.readText("/proj/b.txt"))
        assertTrue(sandbox.move("/proj/b.txt", "/proj/c.txt"))
        assertNull(sandbox.readText("/proj/b.txt"))
        assertEquals("one", sandbox.readText("/proj/c.txt"))
        assertTrue(sandbox.delete("/proj"))
        assertFalse(File(root, "proj").exists())
    }

    @Test
    fun theRootItselfCannotBeDeletedOrMoved() {
        File(root, "keep.txt").writeText("x")
        assertFalse(sandbox.delete("/"))
        assertFalse(sandbox.move("/", "/elsewhere"))
        assertTrue(File(root, "keep.txt").exists())
    }

    @Test
    fun copyOntoItselfOrIntoItselfIsRefused() {
        sandbox.writeText("/f.txt", "data")
        sandbox.mkdir("/d")
        assertFalse(sandbox.copy("/f.txt", "/f.txt"))
        assertFalse(sandbox.copy("/d", "/d/inner"))
        assertEquals("data", sandbox.readText("/f.txt"))
    }

    @Test
    fun readTextRefusesDirectoriesAndMissingFiles() {
        sandbox.mkdir("/d")
        assertNull(sandbox.readText("/d"))
        assertNull(sandbox.readText("/missing.txt"))
        assertNotNull(sandbox.resolve("/missing.txt"))
    }
}
