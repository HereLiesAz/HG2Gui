package com.hereliesaz.hg2gui.terminal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsolationSeedPlannerTest {
    private val pkg = PackageLifecycleStore.InstalledPackage("pkg", "Termux / pkg", "demo", "1.0", listOf("demo"), false, true)

    @Test
    fun manifestListsOnlyExistingPrefixFiles_andDropsNewlineAndDotDotPaths() {
        withHg2TempDir { dir ->
            val prefix = File(dir, "usr")
            val real = prefix.write("bin/demo", "x")
            prefix.write("var/lib/dpkg/status", "Package: demo\nStatus: install ok installed\nVersion: 1.0\n\n")
            prefix.write(
                "var/lib/dpkg/info/demo.list",
                listOf(real.absolutePath, "${prefix.absolutePath}/bin/missing", "${prefix.absolutePath}/bin/../../escape", "/etc/passwd").joinToString("\n") + "\n"
            )
            File(dir, "escape").writeText("outside")
            val plan = IsolationSeedPlanner.prepare(prefix, pkg, File(dir, "base"))
            assertEquals("dependency-closure-manifest", plan.strategy)
            assertEquals(listOf(real.absolutePath), plan.manifest!!.readLines())
            assertTrue(plan.fileCount == 1)
        }
    }

    @Test
    fun nonPkgManagersFallBackToWholePrefixCopy() {
        withHg2TempDir { dir ->
            val plan = IsolationSeedPlanner.prepare(File(dir, "usr"), pkg.copy(manager = "pip"), File(dir, "base"))
            assertEquals("installed-prefix-copy", plan.strategy)
        }
    }
}
