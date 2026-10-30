package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PrefixRuntimeConfig is the single writer of apt.conf and the script-wrapper profile; both
 * DistroManager (after a bootstrap) and TermuxRuntimeRepair (every launch) call it.
 */
class PrefixRuntimeConfigTest {

    private val nativeDir = "/data/app/~~x==/com.hereliesaz.hg2gui-y==/lib/arm64"

    @Test
    fun aptConf_pointsAtTheLauncherBinariesAndTheGivenPrefix() {
        val text = PrefixRuntimeConfig.aptConfText("/data/user/0/app/files/usr", nativeDir)
        assertTrue(text.contains("Dir \"/data/user/0/app/files/usr/\";"))
        assertTrue(text.contains("Dir::Bin::dpkg \"$nativeDir/libhg2gui_dpkg.so\";"))
        assertTrue(text.contains("Dir::Bin::apt-key \"$nativeDir/libhg2gui_apt_key.so\";"))
        assertTrue(text.contains("Acquire::gpgv::Command \"/data/user/0/app/files/usr/bin/gpgv\";"))
        assertTrue(text.contains("Acquire::https::CaInfo \"/data/user/0/app/files/usr/etc/tls/cert.pem\";"))
    }

    @Test
    fun writeAptConf_writesIntoTheRootButNamesTheLogicalPrefix() {
        withHg2TempDir { dir ->
            val staging = File(dir, "usr.staging")
            val finalPrefix = File(dir, "usr")
            PrefixRuntimeConfig.writeAptConf(staging, finalPrefix, nativeDir)

            val written = File(staging, "etc/apt/apt.conf").readText()
            assertTrue(written.contains("Dir \"${finalPrefix.absolutePath}/\";"))
            assertFalse(written.contains("usr.staging"))
            assertTrue(File(staging, "var/cache/apt/archives/partial").isDirectory)
            assertTrue(File(staging, "var/lib/apt/lists/partial").isDirectory)
        }
    }

    @Test
    fun everyWriterProducesIdenticalApt_conf() {
        withHg2TempDir { dir ->
            val prefix = File(dir, "usr")
            PrefixRuntimeConfig.writeAptConf(prefix, prefix, nativeDir) // bootstrap path
            val first = File(prefix, "etc/apt/apt.conf").readText()
            PrefixRuntimeConfig.writeAptConf(prefix, prefix, nativeDir) // per-launch repair path
            assertEquals(first, File(prefix, "etc/apt/apt.conf").readText())
        }
    }

    private fun scriptTree(dir: File): Pair<File, File> {
        val prefix = File(dir, "usr")
        val home = File(dir, "home")
        prefix.write("bin/pkg", "#!/data/user/0/app/files/usr/bin/bash\necho pkg\n")
        prefix.write("bin/apt-key", "#!/usr/bin/env sh\nexit 0\n")
        prefix.write("bin/pyscript", "#!/usr/bin/python3\nprint(1)\n")
        prefix.write("bin/plain", "echo no shebang\n")
        prefix.write("bin/x;rm -rf ~", "#!/bin/sh\n")
        prefix.write("bin/-rf", "#!/bin/sh\n")
        prefix.writeBytesAt("bin/elf", byteArrayOf(0x7f, 'E'.code.toByte(), 'L'.code.toByte(), 'F'.code.toByte(), 0))
        Files.createSymbolicLink(File(prefix, "bin/link").toPath(), File(prefix, "bin/pkg").toPath())
        return prefix to home
    }

    @Test
    fun wrappers_coverOnlySafeShellScripts_andIncludeTheDpkgAndAptFunctions() {
        withHg2TempDir { dir ->
            val (prefix, home) = scriptTree(dir)
            PrefixRuntimeConfig.writeScriptWrappers(prefix, home, nativeDir)
            val text = File(home, PrefixRuntimeConfig.WRAPPER_SCRIPT_NAME).readText()

            assertTrue(text.contains("apt-key() ( source \"\$PREFIX/bin/apt-key\" \"\$@\" )"))
            assertTrue(text.contains("pkg() ( source \"\$PREFIX/bin/pkg\" \"\$@\" )"))
            for (excluded in listOf("pyscript", "plain", "elf", "link", "x;rm", "-rf")) {
                assertFalse("$excluded must not be wrapped", text.contains("$excluded()") || text.contains("bin/$excluded\""))
            }
            assertTrue(text.contains("dpkg() {"))
            assertTrue(text.contains("'$nativeDir/libhg2gui_dpkg.so' \"\$@\""))
            assertTrue(text.contains("apt() {"))
            assertTrue(text.contains(PrefixRuntimeConfig.NEEDS_APT_UPDATE))
        }
    }

    @Test
    fun wrappers_areSorted_andProfileLineIsAddedOnceWithoutTouchingUserContent() {
        withHg2TempDir { dir ->
            val (prefix, home) = scriptTree(dir)
            home.mkdirs()
            File(home, ".bash_profile").writeText("export FOO=1") // no trailing newline
            PrefixRuntimeConfig.writeScriptWrappers(prefix, home, nativeDir)
            PrefixRuntimeConfig.writeScriptWrappers(prefix, home, nativeDir)

            val profile = File(home, ".bash_profile").readText()
            assertTrue(profile.startsWith("export FOO=1\n"))
            assertEquals(1, profile.lines().count { it.contains("script-wrappers") })

            val text = File(home, PrefixRuntimeConfig.WRAPPER_SCRIPT_NAME).readText()
            assertTrue(text.indexOf("apt-key()") < text.indexOf("pkg()"))
        }
    }

    @Test
    fun wrappers_surviveAnApkUpdateByRegeneratingTheLauncherPath() {
        withHg2TempDir { dir ->
            val (prefix, home) = scriptTree(dir)
            PrefixRuntimeConfig.writeScriptWrappers(prefix, home, "/data/app/old/lib")
            PrefixRuntimeConfig.writeScriptWrappers(prefix, home, "/data/app/new/lib")
            val text = File(home, PrefixRuntimeConfig.WRAPPER_SCRIPT_NAME).readText()
            assertTrue(text.contains("/data/app/new/lib/libhg2gui_dpkg.so"))
            assertFalse(text.contains("/data/app/old/lib"))
        }
    }

    @Test
    fun wrappers_putTheDisableShimDirectoryFirstOnPath_andGuardDpkg() {
        withHg2TempDir { dir ->
            val (prefix, home) = scriptTree(dir)
            PrefixRuntimeConfig.writeScriptWrappers(prefix, home, nativeDir)
            val text = File(home, PrefixRuntimeConfig.WRAPPER_SCRIPT_NAME).readText()
            assertTrue(text.contains("'${File(dir, DisableShims.DIRECTORY_NAME).absolutePath}'"))
            assertTrue(text.contains("_hg2gui_dpkg_guard \"\$@\" || return \$?"))
            assertTrue(text.indexOf("_hg2gui_dpkg_guard() {") < text.indexOf("dpkg() {"))
        }
    }

    @Test
    fun shebangAndNameRules() {
        assertTrue(PrefixRuntimeConfig.isShellShebang("#!/usr/bin/env -S bash"))
        assertFalse(PrefixRuntimeConfig.isShellShebang("#!/usr/bin/env python3"))
        assertTrue(PrefixRuntimeConfig.isSafeScriptName("termux-setup-package-manager"))
        assertTrue(PrefixRuntimeConfig.isSafeScriptName("g++-wrapper"))
        assertFalse(PrefixRuntimeConfig.isSafeScriptName("-rf"))
        assertFalse(PrefixRuntimeConfig.isSafeScriptName(".."))
        assertFalse(PrefixRuntimeConfig.isSafeScriptName("a b"))
        assertFalse(PrefixRuntimeConfig.isSafeScriptName("a".repeat(65)))
    }
}
