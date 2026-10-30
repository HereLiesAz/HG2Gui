package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * PATH-level Disable enforcement (audit M-5) and the direct-dpkg guard. The pure planning and the
 * directory sync run everywhere; the tests that execute the generated scripts use the host's
 * /bin/sh and skip cleanly (JUnit assumption) where there is none.
 */
class DisableShimsTest {

    private fun pkg(
        name: String,
        binaries: List<String>,
        disabled: Boolean,
        manager: String = "pkg"
    ) = PackageLifecycleStore.InstalledPackage(manager, "label", name, "1.0", binaries, disabled, false)

    private val launcher = "/data/app/x/lib/arm64/libhg2gui_dpkg.so"

    // -- which binaries get a shim ---------------------------------------------------------------

    @Test
    fun onlyBinariesOfDisabledPackagesGetShims_andDpkgIsAlwaysGuarded() {
        val shims = DisableShims.desiredShims(
            listOf(
                pkg("libfoo", listOf("foo", "foo-config"), disabled = true),
                pkg("coreutils", listOf("ls", "env"), disabled = false),
                pkg("bash", listOf("bash", "sh"), disabled = false)
            ),
            launcher
        )
        assertEquals(setOf("dpkg", "foo", "foo-config"), shims.keys)
        assertTrue(shims.getValue("foo").contains("foo is disabled (hg2package enable pkg libfoo)"))
        assertTrue(shims.getValue("foo").contains("exit 126"))
        assertTrue(shims.getValue("dpkg").contains("_hg2gui_dpkg_guard"))
    }

    @Test
    fun aBinaryAlsoOwnedByAnEnabledPackageIsNeverShadowed() {
        val shims = DisableShims.desiredShims(
            listOf(
                pkg("python", listOf("python3", "pip"), disabled = true),
                pkg("pip", listOf("pip"), disabled = false, manager = "pip")
            ),
            launcher
        )
        assertFalse("pip is still provided by an enabled package", "pip" in shims)
        assertTrue("python3" in shims)
    }

    @Test
    fun coreToolsAreShimmedOnlyWhenTheirOwnerIsExplicitlyDisabled() {
        val enabled = DisableShims.desiredShims(listOf(pkg("coreutils", listOf("ls", "env", "cat"), disabled = false)), launcher)
        assertEquals(setOf("dpkg"), enabled.keys)
        val disabled = DisableShims.desiredShims(listOf(pkg("coreutils", listOf("ls", "env", "cat"), disabled = true)), launcher)
        assertEquals(setOf("dpkg", "ls", "env", "cat"), disabled.keys)
    }

    @Test
    fun aDisabledDpkgPackageReplacesTheGuardWithTheDisabledShim() {
        val shims = DisableShims.desiredShims(listOf(pkg("dpkg", listOf("dpkg", "dpkg-deb"), disabled = true)), launcher)
        assertTrue(shims.getValue("dpkg").contains("dpkg is disabled (hg2package enable pkg dpkg)"))
        assertFalse(shims.getValue("dpkg").contains("exec "))
    }

    @Test
    fun unsafeBinaryNamesGetNoShim_andHostileMessagesStayOnOneQuotedLine() {
        val shims = DisableShims.desiredShims(
            listOf(
                pkg(
                    "evil'pkg\nexit 0",
                    listOf("ok", "..", "a b", "x;rm -rf ~", "-rf", "dir/inner", "["),
                    disabled = true
                )
            ),
            launcher
        )
        assertEquals(setOf("dpkg", "ok", "inner"), shims.keys)
        val lines = shims.getValue("ok").lines().filter { it.isNotEmpty() }
        assertEquals(4, lines.size) // shebang, marker, printf, exit
        assertTrue(lines[2].startsWith("printf '%s\\n' 'ok is disabled (hg2package enable pkg evil'\\''pkg exit 0)' >&2"))
        assertEquals("exit 126", lines[3])
    }

    @Test
    fun theMessageNamesTheAlphabeticallyFirstOwnerWhenSeveralAreDisabled() {
        val shims = DisableShims.desiredShims(
            listOf(
                pkg("zeta", listOf("tool"), disabled = true),
                pkg("alpha", listOf("tool"), disabled = true)
            ),
            launcher
        )
        assertTrue(shims.getValue("tool").contains("enable pkg alpha"))
    }

    // -- PATH ------------------------------------------------------------------------------------

    @Test
    fun theShimDirectoryIsASiblingOfThePrefix_notInsideAnyPackageOwnedTree() {
        val prefix = File("/data/user/0/app/files/usr")
        val dir = DisableShims.directoryFor(prefix)
        assertEquals(File("/data/user/0/app/files/hg2gui-shims"), dir)
        assertFalse(dir.path.startsWith(prefix.path + "/"))
        assertFalse(dir.path.startsWith("/data/user/0/app/files/home"))
    }

    @Test
    fun pathGetsTheShimDirectoryFirst_onlyForAShellEnvironment() {
        val prefix = File("/data/user/0/app/files/usr")
        val shims = File("/data/user/0/app/files/hg2gui-shims")
        assertEquals(
            "/data/user/0/app/files/hg2gui-shims:/data/user/0/app/files/usr/bin",
            DisableShims.pathWithShims("/data/user/0/app/files/usr/bin", prefix, shims)
        )
        assertEquals(
            "/data/user/0/app/files/hg2gui-shims:/data/user/0/app/files/usr/bin:/system/bin",
            DisableShims.pathWithShims("/data/user/0/app/files/usr/bin:/system/bin", prefix, shims)
        )
        // The engine's own dpkg/maintainer-script environments and the app's inherited PATH are untouched.
        assertEquals("/system/bin:/system/xbin", DisableShims.pathWithShims("/system/bin:/system/xbin", prefix, shims))
        assertEquals("/other/bin:/data/user/0/app/files/usr/bin", DisableShims.pathWithShims("/other/bin:/data/user/0/app/files/usr/bin", prefix, shims))
        assertEquals(null, DisableShims.pathWithShims(null, prefix, shims))
        val once = DisableShims.pathWithShims("/data/user/0/app/files/usr/bin", prefix, shims)
        assertEquals(once, DisableShims.pathWithShims(once, prefix, shims))
    }

    // -- directory sync --------------------------------------------------------------------------

    @Test
    fun sync_writesExecutableShims_removesStaleOnes_andLeavesForeignFilesAlone() = withHg2TempDir { root ->
        val dir = File(root, DisableShims.DIRECTORY_NAME)
        val foo = DisableShims.disabledScript("foo", pkg("libfoo", listOf("foo"), true))
        val dpkg = DisableShims.dpkgGuardScript(launcher)
        val both = mapOf("foo" to foo, "dpkg" to dpkg)

        val before = DisableShims.generation()
        assertTrue(DisableShims.syncDirectory(dir, both))
        assertTrue(DisableShims.generation() > before)
        assertEquals(setOf("foo", "dpkg"), dir.list().orEmpty().toSet())
        assertTrue(File(dir, "foo").canExecute())
        assertEquals(foo, File(dir, "foo").readText())

        assertFalse("nothing changed: nothing rewritten", DisableShims.syncDirectory(dir, both))

        // Package enabled again: the foo shim goes, dpkg's stays, a file that is not ours is untouched.
        File(dir, "mine.txt").writeText("not a shim\nsecond line\n")
        assertTrue(DisableShims.syncDirectory(dir, mapOf("dpkg" to dpkg)))
        assertEquals(setOf("dpkg", "mine.txt"), dir.list().orEmpty().toSet())
    }

    @Test
    fun sync_rewritesAShimWhoseContentChanged_andCleansTempLeftovers() = withHg2TempDir { root ->
        val dir = File(root, DisableShims.DIRECTORY_NAME)
        DisableShims.syncDirectory(dir, mapOf("dpkg" to DisableShims.dpkgGuardScript("/old/lib/libhg2gui_dpkg.so")))
        File(dir, "x~tmp").writeText("partial")
        assertTrue(DisableShims.syncDirectory(dir, mapOf("dpkg" to DisableShims.dpkgGuardScript("/new/lib/libhg2gui_dpkg.so"))))
        val text = File(dir, "dpkg").readText()
        assertTrue(text.contains("/new/lib/libhg2gui_dpkg.so"))
        assertFalse(text.contains("/old/lib"))
        assertEquals(listOf("dpkg"), dir.list().orEmpty().toList())
        assertTrue(File(dir, "dpkg").canExecute())
    }

    // -- the generated scripts, executed ---------------------------------------------------------

    private class Sandbox(val root: File, val shims: File, val launcher: File)

    /** Real shims in a temp dir, with the on-device interpreter path swapped for the host's /bin/sh. */
    private fun withSandbox(packages: List<PackageLifecycleStore.InstalledPackage>, block: (Sandbox) -> Unit) {
        assumeTrue("/bin/sh is not available", File("/bin/sh").canExecute())
        withHg2TempDir { root ->
            val stub = File(root, "launcher").apply {
                writeText("#!/bin/sh\necho \"LAUNCHER \$*\"\n")
                setExecutable(true, false)
            }
            val dir = File(root, DisableShims.DIRECTORY_NAME)
            DisableShims.syncDirectory(dir, DisableShims.desiredShims(packages, stub.absolutePath))
            dir.listFiles().orEmpty().forEach { file ->
                file.writeText(file.readText().replaceFirst("#!/system/bin/sh", "#!/bin/sh"))
                file.setExecutable(true, false)
            }
            block(Sandbox(root, dir, stub))
        }
    }

    private class Ran(val code: Int, val out: String, val err: String)

    private fun sh(box: Sandbox, script: String, vararg args: String): Ran {
        val builder = ProcessBuilder(listOf("/bin/sh", "-c", script, "sh") + args)
        builder.environment()["PATH"] = box.shims.absolutePath + ":/usr/bin:/bin"
        val process = builder.start()
        assumeTrue("shell did not finish", process.waitFor(20, TimeUnit.SECONDS))
        return Ran(process.exitValue(), process.inputStream.bufferedReader().readText(), process.errorStream.bufferedReader().readText())
    }

    @Test
    fun aDisabledBinaryIsRefusedWhateverWayTheShellFindsIt() = withSandbox(listOf(pkg("libfoo", listOf("foo"), disabled = true))) { box ->
        val direct = sh(box, "foo")
        assertEquals(126, direct.code)
        assertEquals("foo is disabled (hg2package enable pkg libfoo)\n", direct.err)
        assertEquals("", direct.out)

        assertEquals(126, sh(box, "tool=foo; \$tool").code)
        assertEquals(126, sh(box, "\$(command -v foo)").code)
        assertEquals(126, sh(box, "run() { foo; }; run").code)
        assertEquals(126, sh(box, "/bin/sh -c foo").code)
        assertEquals(126, sh(box, "env foo").code)
        assertEquals(0, sh(box, "true").code)
    }

    @Test
    fun aBinaryThatIsNotDisabledIsNotShadowed() = withSandbox(listOf(pkg("libfoo", listOf("foo"), disabled = true))) { box ->
        assertFalse(File(box.shims, "ls").exists())
        assertEquals(0, sh(box, "ls /").code)
        assertEquals(0, sh(box, "env true").code)
    }

    @Test
    fun theDpkgShimRefusesMutatingActionsAndPassesReadOnlyOnesToTheLauncher() = withSandbox(emptyList()) { box ->
        val readOnly = listOf(
            listOf("-l"), listOf("-L", "bash"), listOf("-S", "/bin/sh"), listOf("-s", "bash"),
            listOf("--print-architecture"), listOf("--get-selections"), listOf("--compare-versions", "1", "lt", "2"),
            listOf("-l", "--", "-i")
        )
        for (args in readOnly) {
            val r = sh(box, "dpkg \"\$@\"", *args.toTypedArray())
            assertEquals(args.toString(), 0, r.code)
            assertEquals(args.toString(), "LAUNCHER ${args.joinToString(" ")}\n", r.out)
        }
        val mutating = listOf(
            listOf("-i", "a.deb"), listOf("--install", "a.deb"), listOf("-r", "x"), listOf("--remove", "x"),
            listOf("-P", "x"), listOf("--purge", "x"), listOf("--configure", "-a"), listOf("--unpack", "a.deb"),
            listOf("--triggers-only", "-a"), listOf("--force-all", "-l"), listOf("-l", "--force-depends"),
            listOf("--set-selections"), listOf("--add-architecture", "arm"), listOf("--command-fd=0"), listOf("-A", "a.deb")
        )
        for (args in mutating) {
            val r = sh(box, "dpkg \"\$@\"", *args.toTypedArray())
            assertEquals(args.toString(), 126, r.code)
            assertEquals(args.toString(), "", r.out)
            assertTrue(args.toString(), r.err.contains("pkg install") && r.err.contains("pkg remove"))
        }
        // Reached through env / sudo-style indirection or a dynamic command word: same shim.
        assertEquals(126, sh(box, "env dpkg -i x.deb").code)
        assertEquals(126, sh(box, "t=dpkg; \$t --purge x").code)
        assertEquals(0, sh(box, "env dpkg -l").code)
    }

    @Test
    fun theWrapperProfileGuardsTheDpkgFunction_andPutsTheShimDirectoryFirstOnce() {
        assumeTrue("/bin/sh is not available", File("/bin/sh").canExecute())
        withHg2TempDir { root ->
            val stub = File(root, "launcher").apply {
                writeText("#!/bin/sh\necho \"LAUNCHER \$*\"\n")
                setExecutable(true, false)
            }
            val shims = File(root, "shims").apply { mkdirs() }
            val profile = File(root, "profile.sh")
            profile.writeText(PrefixRuntimeConfig.wrapperScriptText(emptyList(), stub.absolutePath, shims.absolutePath))
            fun run(script: String, path: String): Ran {
                val builder = ProcessBuilder("/bin/sh", "-c", ". '${profile.absolutePath}'; $script")
                builder.environment()["PATH"] = path
                builder.environment()["PREFIX"] = root.absolutePath
                val process = builder.start()
                assumeTrue("shell did not finish", process.waitFor(20, TimeUnit.SECONDS))
                return Ran(process.exitValue(), process.inputStream.bufferedReader().readText(), process.errorStream.bufferedReader().readText())
            }
            val path = run("echo \"\$PATH\"", "/usr/bin:/bin")
            assertEquals("${shims.absolutePath}:/usr/bin:/bin\n", path.out)
            assertEquals("${shims.absolutePath}:/usr/bin\n", run("echo \"\$PATH\"", "${shims.absolutePath}:/usr/bin").out)

            val list = run("dpkg -l; echo rc=\$?", "/usr/bin:/bin")
            assertEquals("LAUNCHER -l\nrc=0\n", list.out)
            val install = run("dpkg -i x.deb; echo rc=\$?", "/usr/bin:/bin")
            assertEquals("rc=126\n", install.out)
            assertTrue(install.err.contains("pkg install"))
            // The guard's loop variable does not leak into the user's shell.
            assertEquals("unset\n", run("dpkg -l >/dev/null; echo \"\${_hg2gui_a-unset}\"", "/usr/bin:/bin").out)
        }
    }

    @Test
    fun theGuardListCoversTheMutatingActionsTheTaskNames() {
        val text = PrefixRuntimeConfig.dpkgGuardFunction()
        for (flag in listOf("-i", "--install", "-r", "--remove", "-P", "--purge", "--configure", "--unpack", "--force-*")) {
            assertTrue(flag, flag in PrefixRuntimeConfig.DPKG_MUTATING_ARGUMENTS)
            assertTrue(flag, text.contains(flag))
        }
        for (readOnly in listOf("-l", "-L", "-S", "-s", "--print-architecture", "--get-selections")) {
            assertFalse(readOnly, readOnly in PrefixRuntimeConfig.DPKG_MUTATING_ARGUMENTS)
        }
    }
}
