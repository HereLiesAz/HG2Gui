package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintainerScriptStoreTest {

    private class Fixture(val root: File) {
        val prefix = File(root, "usr").apply { mkdirs() }
        val info = File(prefix, "var/lib/dpkg/info").apply { mkdirs() }

        /** A fake "libhg2gui_maintscript.so" in a per-install directory. */
        fun launcher(installDir: String): File =
            root.write("app/$installDir/lib/libhg2gui_maintscript.so", "ELF").also { it.setExecutable(true) }

        fun layout(launcher: File) = MaintainerScriptStore.Layout(info, launcher, prefix)
    }

    private fun target(link: File): String = Files.readSymbolicLink(link.toPath()).toString()

    @Test
    fun migrateInstalled_convertsAPlainScriptIntoBodyPlusTrampolineLink() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val launcher = fx.launcher("v1")
            val script = fx.info.write("pkg.postinst", "#!/data/data/com.termux/files/usr/bin/sh\necho hi\n")

            val changed = MaintainerScriptStore.migrateInstalled(fx.layout(launcher))

            assertEquals(1, changed)
            assertTrue(Files.isSymbolicLink(script.toPath()))
            assertEquals(launcher.absolutePath, target(script))
            val body = File(fx.info, "pkg.postinst.hg2body").readText()
            assertEquals("#!${fx.prefix.absolutePath}/bin/sh\necho hi\n", body)
        }
    }

    @Test
    fun migrateInstalled_healsLinksLeftDanglingByAnApkUpdate() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val oldLauncher = fx.launcher("v1")
            val scripts = fx.root.write("stage/postinst", "#!/bin/sh\necho hi\n").parentFile
            MaintainerScriptStore.restore(fx.layout(oldLauncher), "pkg", scripts)
            val entry = File(fx.info, "pkg.postinst")
            assertEquals(oldLauncher.absolutePath, target(entry))

            // APK update: Android assigns a new nativeLibraryDir and removes the old one.
            File(dir, "app/v1").deleteRecursively()
            val newLauncher = fx.launcher("v2")
            assertFalse("link dangles before healing", entry.exists())

            val changed = MaintainerScriptStore.migrateInstalled(fx.layout(newLauncher))

            assertEquals(1, changed)
            assertEquals(newLauncher.absolutePath, target(entry))
            assertTrue("link resolves after healing", entry.exists())
            assertTrue(File(fx.info, "pkg.postinst.hg2body").isFile)
        }
    }

    @Test
    fun migrateInstalled_isANoOpWhenLinksAlreadyPointAtTheCurrentLauncher() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val launcher = fx.launcher("v1")
            val scripts = fx.root.write("stage/prerm", "#!/bin/sh\nexit 0\n").parentFile
            MaintainerScriptStore.restore(fx.layout(launcher), "pkg", scripts)

            assertEquals(0, MaintainerScriptStore.migrateInstalled(fx.layout(launcher)))
        }
    }

    @Test
    fun migrateInstalled_leavesForeignSymlinksAlone() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val launcher = fx.launcher("v1")
            val elsewhere = fx.root.write("elsewhere", "x")
            val link = File(fx.info, "pkg.postinst")
            Files.createSymbolicLink(link.toPath(), elsewhere.toPath())

            assertEquals(0, MaintainerScriptStore.migrateInstalled(fx.layout(launcher)))
            assertEquals(elsewhere.absolutePath, target(link))
        }
    }

    @Test
    fun stageThenRestore_roundTripsScriptBodiesAndClearsTheInfoEntries() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val layout = fx.layout(fx.launcher("v1"))
            val original = fx.root.write("orig/postinst", "#!/bin/sh\necho one\n").parentFile
            MaintainerScriptStore.restore(layout, "pkg", original)
            assertTrue(MaintainerScriptStore.hasEntry(layout, "pkg", "postinst"))

            val staged = File(dir, "staged")
            MaintainerScriptStore.stage(layout, "pkg", staged)
            assertEquals("#!/bin/sh\necho one\n", File(staged, "postinst").readText())
            assertFalse(MaintainerScriptStore.hasEntry(layout, "pkg", "postinst"))

            MaintainerScriptStore.restore(layout, "pkg", staged)
            assertTrue(MaintainerScriptStore.hasEntry(layout, "pkg", "postinst"))
            MaintainerScriptStore.removeEntries(layout, "pkg")
            assertFalse(MaintainerScriptStore.hasEntry(layout, "pkg", "postinst"))
        }
    }

    @Test
    fun restore_keepsNonUtf8BytesWhileRewritingThePrefix() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val layout = fx.layout(fx.launcher("v1"))
            val bytes = "#!/bin/sh\nP=/data/data/com.termux/files/usr\n# na".toByteArray() +
                byteArrayOf(0xEF.toByte(), 0x76.toByte()) + "\n".toByteArray()
            val scripts = fx.root.writeBytesAt("orig/postinst", bytes).parentFile

            MaintainerScriptStore.restore(layout, "pkg", scripts)

            val expected = "#!/bin/sh\nP=${fx.prefix.absolutePath}\n# na".toByteArray() +
                byteArrayOf(0xEF.toByte(), 0x76.toByte()) + "\n".toByteArray()
            assertArrayEquals(expected, File(fx.info, "pkg.postinst.hg2body").readBytes())
        }
    }

    @Test
    fun restore_refusesANonShellInterpreter() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            val layout = fx.layout(fx.launcher("v1"))
            val scripts = fx.root.write("orig/postinst", "#!/usr/bin/python3\nprint(1)\n").parentFile
            assertThrows(IllegalArgumentException::class.java) {
                MaintainerScriptStore.restore(layout, "pkg", scripts)
            }
        }
    }
}
