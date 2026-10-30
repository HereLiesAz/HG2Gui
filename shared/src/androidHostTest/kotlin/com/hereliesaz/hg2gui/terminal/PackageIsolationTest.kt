package com.hereliesaz.hg2gui.terminal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageIsolationTest {

    private fun spec(dir: File, procFiles: List<String> = PackageIsolation.PROC_ALLOWLIST) = PackageIsolation.LaunchSpec(
        proot = File("/data/app/lib/libhg2gui_proot.so"),
        root = File(dir, "package-isolation/pkg_demo/root"),
        liveLog = PackageIsolation.liveLogFile(File(dir, "package-isolation/pkg_demo")),
        hostPrefix = "/data/user/0/app/files/usr",
        hostHome = "/data/user/0/app/files/home",
        hostBash = "/data/user/0/app/files/usr/bin/bash",
        original = "demo --flag 'quoted arg'",
        bindSystem = true,
        bindApex = true,
        procFiles = procFiles,
        suPaths = listOf("/system/bin/su")
    )

    @Test
    fun theGuestNeverGetsARawProcBind() {
        withHg2TempDir { dir ->
            val command = PackageIsolation.buildCommand(spec(dir))
            assertFalse("raw /proc must not be bound", Regex("-b '?/proc'?(\\s|$)").containsMatchIn(command))
            assertTrue(command.contains("-b '/proc/cpuinfo'"))
            assertTrue(PackageIsolation.PROC_ALLOWLIST.none { it.removePrefix("/proc/").contains('/') })
        }
    }

    @Test
    fun theWatcherLogLivesOutsideTheGuestRoot_andIsNotBoundIntoTheGuest() {
        withHg2TempDir { dir ->
            val s = spec(dir)
            assertFalse(s.liveLog.absolutePath.startsWith(s.root.absolutePath + "/"))
            val command = PackageIsolation.buildCommand(s)
            assertFalse("no guest bind may expose the log", command.contains("-b ${s.liveLog.parentFile.absolutePath}"))
            assertFalse(command.contains(".hg2gui/audit/latest.log"))
        }
    }

    @Test
    fun theCommandRunsTheWatcherOnTheHostSide() {
        withHg2TempDir { dir ->
            val command = PackageIsolation.buildCommand(spec(dir))
            assertTrue(command.startsWith("'/data/user/0/app/files/usr/bin/bash' -c "))
            assertTrue(command.contains("libhg2gui_proot.so"))
            assertTrue(command.contains("-b '/system'"))
            assertTrue(command.contains(".hg2gui/denied:/system/bin/su"))
        }
    }

    @Test
    fun watcherWorksEndToEndWithAFakeProot_andRowsAreSanitised() {
        val bash = File("/bin/bash")
        org.junit.Assume.assumeTrue(bash.canExecute() && File("/usr/bin/env").canExecute())
        withHg2TempDir { dir ->
            val fakeProot = File(dir, "proot").apply {
                // Skips PRoot's own options, then execs the guest command line.
                writeText("#!/bin/bash\nwhile [ \"\$1\" = -r ] || [ \"\$1\" = -b ] || [ \"\$1\" = -w ]; do shift 2; done\nexec \"\$@\"\n")
                setExecutable(true)
            }
            val prefix = File(dir, "usr").also { File(it, "bin").mkdirs() }
            java.nio.file.Files.createSymbolicLink(File(prefix, "bin/bash").toPath(), bash.toPath())
            val base = File(dir, "pkg")
            val s = PackageIsolation.LaunchSpec(
                proot = fakeProot, root = File(base, "root"), liveLog = PackageIsolation.liveLogFile(base),
                hostPrefix = prefix.absolutePath, hostHome = dir.absolutePath, hostBash = File(prefix, "bin/bash").absolutePath,
                original = "echo hello; printf 'x\\033[31mred\\n' > /dev/null; exit 3",
                bindSystem = false, bindApex = false, procFiles = emptyList(), suPaths = emptyList(),
                guestEnv = "/usr/bin/env", sampleInterval = "0.01"
            )
            val process = ProcessBuilder("/bin/bash", "-c", PackageIsolation.buildCommand(s)).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            assertEquals(3, process.waitFor())
            assertTrue(output, output.contains("hello"))
            val rows = PackageIsolation.readObservedTelemetry(s.liveLog)
            assertTrue(rows.toString(), rows.any { it.startsWith("root-process:") })
            assertTrue(rows.none { row -> row.any { it.isISOControl() } })
        }
    }

    @Test
    fun telemetryRowsAreStrippedOfControlCharactersAndCapped() {
        withHg2TempDir { dir ->
            val log = File(dir, "live.log")
            log.writeText("file-write:1:/a\u001b[2Jb\n\n" + "x".repeat(5000) + "\n")
            val rows = PackageIsolation.readObservedTelemetry(log)
            assertEquals("file-write:1:/a?[2Jb", rows[0])
            assertEquals(1024, rows[1].length)
            assertEquals(emptyList<String>(), PackageIsolation.readObservedTelemetry(File(dir, "missing")))
        }
    }

    @Test
    fun auditFormat_labelsRuntimeRowsAsSampledAndBestEffort() {
        val text = PackageIsolation.formatAudit(PackageIsolation.Audit(listOf("a"), emptyList(), emptyList(), listOf("process:1")))
        assertTrue(text, text.contains("best-effort, not tamper-proof"))
        assertTrue(text.contains("host-diffed"))
    }

    @Test
    fun seedCommand_createsTheWatcherLogOutsideTheRoot() {
        withHg2TempDir { dir ->
            val base = File(dir, "package-isolation/pkg_demo")
            val command = PackageIsolation.buildSeedCommand(base, File(dir, "usr"), File(dir, "home"), "1.0", null)
            assertTrue(command.contains(": > '${PackageIsolation.liveLogFile(base).absolutePath}'"))
            assertFalse(command.contains(".hg2gui/audit"))
            assertTrue(command.startsWith("rm -rf '${File(base, "root").absolutePath}'"))
        }
    }
}
