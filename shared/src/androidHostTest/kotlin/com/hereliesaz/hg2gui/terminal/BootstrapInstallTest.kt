package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The stage-verify-swap bootstrap install (audit M-6): a failed bootstrap must never delete a
 * pre-existing prefix.
 */
class BootstrapInstallTest {

    private class Fixture(val root: File) {
        val prefix = File(root, "files/usr")
        val staging = File(root, "files/usr.staging")
        val backup = File(root, "files/usr.previous")
        val nativeDir = File(root, "native").apply { mkdirs() }.also {
            File(it, "libbin_bash.so").apply { writeText("ELF-bash"); setExecutable(true) }
        }

        fun zip(vararg entries: Pair<String, ByteArray?>): File {
            val file = File(root, "bootstrap.zip")
            ZipOutputStream(file.outputStream()).use { zip ->
                for ((name, data) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    if (data != null) zip.write(data)
                    zip.closeEntry()
                }
            }
            return file
        }

        fun install(zip: File) = DistroManager.installBootstrapArchive(zip, prefix, staging, backup, nativeDir.absolutePath)

        /** A pre-existing installation with a package the user cares about. */
        fun existingInstall() {
            prefix.write("bin/bash", "old bash")
            prefix.write("var/lib/dpkg/status", "Package: precious\nStatus: install ok installed\n\n")
            prefix.write("lib/libprecious.so", "payload")
        }

        fun assertExistingInstallUntouched() {
            assertEquals("old bash", File(prefix, "bin/bash").readText())
            assertEquals("payload", File(prefix, "lib/libprecious.so").readText())
            assertTrue(File(prefix, "var/lib/dpkg/status").readText().contains("precious"))
        }
    }

    private fun validZip(fx: Fixture, extra: List<Pair<String, ByteArray?>> = emptyList()): File = fx.zip(
        "bin/" to null,
        "bin/bash" to "inert zip content".toByteArray(),
        "bin/pkg" to "#!/data/data/com.termux/files/usr/bin/bash\necho pkg\n".toByteArray(),
        "share/termux-keyring/termux-pacman.gpg" to "key".toByteArray(),
        "var/lib/dpkg/status" to "Package: bash\nStatus: install ok installed\n\n".toByteArray(),
        "SYMLINKS.txt" to
            "/data/data/com.termux/files/usr/bin/bash←./bin/sh\n".toByteArray(Charsets.UTF_8),
        *extra.toTypedArray()
    )

    @Test
    fun freshInstall_landsAtTheFinalPrefixWithPathsThatNameTheFinalPrefixNotTheStagingDir() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.install(validZip(fx))

            assertTrue(File(fx.prefix, "bin/bash").canExecute())
            assertFalse(fx.staging.exists())
            assertFalse(fx.backup.exists())
            val shebang = File(fx.prefix, "bin/pkg").readText()
            assertEquals("#!${fx.prefix.absolutePath}/bin/bash\necho pkg\n", shebang)
            assertFalse("no staging path may leak into content", shebang.contains("usr.staging"))

            val aptConf = File(fx.prefix, "etc/apt/apt.conf").readText()
            assertTrue(aptConf.contains("Dir \"${fx.prefix.absolutePath}/\";"))
            assertFalse(aptConf.contains("usr.staging"))
            assertTrue(aptConf.contains(File(fx.nativeDir, "libhg2gui_dpkg.so").absolutePath))

            val shLink = Files.readSymbolicLink(File(fx.prefix, "bin/sh").toPath()).toString()
            assertEquals("${fx.prefix.absolutePath}/bin/bash", shLink)
            val keyLink = Files.readSymbolicLink(File(fx.prefix, "etc/apt/trusted.gpg.d/termux-pacman.gpg").toPath()).toString()
            assertEquals("${fx.prefix.absolutePath}/share/termux-keyring/termux-pacman.gpg", keyLink)
        }
    }

    @Test
    fun aCorruptArchive_leavesAnExistingPrefixExactlyAsItWas() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.existingInstall()
            val corrupt = File(dir, "corrupt.zip").apply { writeBytes(ByteArray(2000) { (it * 7).toByte() }) }

            assertThrows(Exception::class.java) { fx.install(corrupt) }

            fx.assertExistingInstallUntouched()
            assertFalse(fx.staging.exists())
            assertFalse(fx.backup.exists())
        }
    }

    @Test
    fun anArchiveThatVerifiesBadly_leavesAnExistingPrefixUntouched() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.existingInstall()
            val noBash = fx.zip("bin/" to null, "bin/pkg" to "#!/bin/sh\n".toByteArray())

            assertThrows(IOException::class.java) { fx.install(noBash) }

            fx.assertExistingInstallUntouched()
            assertFalse(fx.staging.exists())
        }
    }

    @Test
    fun aPathTraversalEntry_abortsWithoutTouchingTheLivePrefix() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.existingInstall()
            val evil = validZip(fx, listOf("../escaped.txt" to "x".toByteArray()))

            assertThrows(IOException::class.java) { fx.install(evil) }

            fx.assertExistingInstallUntouched()
            assertFalse(File(dir, "files/escaped.txt").exists())
        }
    }

    @Test
    fun aSuccessfulInstallOverAnExistingPrefix_replacesItAndDropsTheBackup() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.existingInstall()
            fx.install(validZip(fx))

            assertTrue(File(fx.prefix, "var/lib/dpkg/status").readText().contains("Package: bash"))
            assertFalse(File(fx.prefix, "lib/libprecious.so").exists())
            assertFalse(fx.backup.exists())
            assertFalse(fx.staging.exists())
        }
    }

    @Test
    fun aStaleStagingDirectoryFromACrashedRun_isClearedFirst() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.staging.write("leftover/garbage", "x")
            fx.install(validZip(fx))
            assertFalse(File(fx.prefix, "leftover").exists())
        }
    }

    @Test
    fun nonUtf8ScriptsComeThroughByteForByte() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val script = "#!/data/data/com.termux/files/usr/bin/bash\n# na".toByteArray() + byteArrayOf(0xEF.toByte()) + "\n".toByteArray()
            fx.install(validZip(fx, listOf("bin/latin1" to script)))
            val expected = "#!${fx.prefix.absolutePath}/bin/bash\n# na".toByteArray() + byteArrayOf(0xEF.toByte()) + "\n".toByteArray()
            assertArrayEquals(expected, File(fx.prefix, "bin/latin1").readBytes())
        }
    }

    // --- BootstrapStaging itself ----------------------------------------------------------

    @Test
    fun swapIntoPlace_withAMissingStagingDir_failsAndKeepsThePrefix() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.existingInstall()
            assertThrows(IOException::class.java) { BootstrapStaging.swapIntoPlace(fx.prefix, fx.staging, fx.backup) }
            fx.assertExistingInstallUntouched()
        }
    }

    @Test
    fun recoverInterruptedSwap_putsThePreviousPrefixBackWhenTheNewOneNeverLanded() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            // The state a crash leaves: old prefix moved aside, nothing moved in.
            fx.backup.write("bin/bash", "old bash")

            assertTrue(BootstrapStaging.recoverInterruptedSwap(fx.prefix, fx.backup))

            assertEquals("old bash", File(fx.prefix, "bin/bash").readText())
            assertFalse(fx.backup.exists())
            assertFalse("a no-op once the prefix exists", BootstrapStaging.recoverInterruptedSwap(fx.prefix, fx.backup))
        }
    }

    @Test
    fun swapIntoPlace_restoresAnInterruptedPreviousPrefixBeforeAnythingElse() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.backup.write("bin/bash", "old bash")
            fx.staging.write("bin/bash", "new bash")

            BootstrapStaging.swapIntoPlace(fx.prefix, fx.staging, fx.backup)

            assertEquals("new bash", File(fx.prefix, "bin/bash").readText())
            assertFalse("the interrupted backup was consumed, not left to be mistaken for garbage", fx.backup.exists())
        }
    }

    @Test
    fun verify_requiresBinAndBash() {
        withHg2TempDir { dir ->
            val staged = File(dir, "s").apply { mkdirs() }
            assertThrows(IOException::class.java) { BootstrapStaging.verify(staged) }
            File(staged, "bin").mkdirs()
            assertThrows(IOException::class.java) { BootstrapStaging.verify(staged) }
            staged.write("bin/bash", "x")
            BootstrapStaging.verify(staged, requireExecutableBash = false)
        }
    }
}
