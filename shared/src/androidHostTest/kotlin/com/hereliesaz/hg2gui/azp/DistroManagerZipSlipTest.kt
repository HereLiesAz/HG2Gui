package com.hereliesaz.hg2gui.azp

import com.hereliesaz.hg2gui.terminal.DistroManager
import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DistroManagerZipSlipTest {
    private val root = Files.createTempDirectory("prefix").toFile().resolve("usr").apply { mkdirs() }

    @Test
    fun allowsNestedPath() {
        assertEquals(File(root.canonicalFile, "bin/sh"), DistroManager.resolveContained(root, "bin/sh"))
    }

    @Test
    fun rejectsParentTraversal() {
        assertThrows(IOException::class.java) { DistroManager.resolveContained(root, "../../shared_prefs/x") }
    }

    @Test
    fun rejectsSiblingWithSharedPrefix() {
        assertThrows(IOException::class.java) { DistroManager.resolveContained(root, "../usr2/x") }
    }
}
