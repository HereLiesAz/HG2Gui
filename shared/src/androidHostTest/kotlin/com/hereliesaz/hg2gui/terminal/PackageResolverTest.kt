package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Dependency planning and `pkg` argument handling, run against a fake version comparator instead of
 * the dpkg binary.
 */
class PackageResolverTest {

    /** Dotted-numeric comparator good enough for the fixtures: "1.10" > "1.9". */
    private fun compare(candidate: String, operator: String, wanted: String): Boolean {
        fun parts(v: String) = v.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(wanted)
        var cmp = 0
        for (i in 0 until maxOf(a.size, b.size)) {
            cmp = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (cmp != 0) break
        }
        return when (operator) {
            "<<" -> cmp < 0
            "<=" -> cmp <= 0
            "=" -> cmp == 0
            ">=" -> cmp >= 0
            ">>" -> cmp > 0
            else -> false
        }
    }

    private val resolver = PackageResolver(::compare)

    private fun index(text: String): Map<String, Hg2PackageManager.PackageRecord> =
        DebControl.parsePackages(text.trimIndent()).associateBy { it.name }

    private fun installed(text: String): Map<String, Hg2PackageManager.Installed> =
        DebControl.parseInstalled(text.trimIndent())

    private val repo = index(
        """
        Package: app
        Version: 2.0
        Architecture: aarch64
        Filename: pool/app.deb
        SHA256: aa
        Depends: lib (>= 1.5), shell | busybox

        Package: lib
        Version: 1.6
        Architecture: aarch64
        Filename: pool/lib.deb
        SHA256: bb

        Package: busybox
        Version: 1.0
        Architecture: aarch64
        Filename: pool/busybox.deb
        SHA256: cc
        Provides: shell

        Package: other
        Version: 1.0
        Architecture: aarch64
        Filename: pool/other.deb
        SHA256: dd
        Conflicts: busybox
        """
    )

    @Test
    fun plansDependenciesBeforeDependents_andResolvesAProviderForAnAlternative() {
        val plan = resolver.resolve(listOf("app"), repo, emptyMap(), forceRequested = false)
        assertEquals(listOf("lib", "busybox", "app"), plan.map { it.name })
    }

    @Test
    fun installedVersionSatisfyingTheRelationIsNotPlannedAgain() {
        val plan = resolver.resolve(
            listOf("app"),
            repo,
            installed("Package: lib\nStatus: install ok installed\nVersion: 1.9\n\nPackage: busybox\nStatus: install ok installed\nVersion: 1.0\n"),
            forceRequested = false
        )
        assertEquals(listOf("app"), plan.map { it.name })
    }

    @Test
    fun anInstalledDependencyThatIsTooOldIsReplannedAtTheRepositoryVersion() {
        val plan = resolver.resolve(
            listOf("app"),
            repo,
            installed("Package: lib\nStatus: install ok installed\nVersion: 1.2\n\nPackage: busybox\nStatus: install ok installed\nVersion: 1.0\n"),
            forceRequested = false
        )
        assertEquals(listOf("lib", "app"), plan.map { it.name })
    }

    @Test
    fun installingAnAlreadyInstalledPackageIsANoOpWithoutForce() {
        val plan = resolver.resolve(
            listOf("lib"),
            repo,
            installed("Package: lib\nStatus: install ok installed\nVersion: 1.2\n"),
            forceRequested = false
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun forceRequestedReplansTheNamedInstalledPackageButNotItsInstalledDependencies() {
        val plan = resolver.resolve(
            listOf("app"),
            repo,
            installed(
                "Package: app\nStatus: install ok installed\nVersion: 1.0\n\n" +
                    "Package: lib\nStatus: install ok installed\nVersion: 1.6\n\n" +
                    "Package: busybox\nStatus: install ok installed\nVersion: 1.0\n"
            ),
            forceRequested = true
        )
        assertEquals(listOf("app"), plan.map { it.name })
        assertEquals("2.0", plan.single().version)
    }

    @Test
    fun forceRequestedWorksThroughAProvidedName() {
        val plan = resolver.resolve(
            listOf("shell"),
            repo,
            installed("Package: busybox\nStatus: install ok installed\nVersion: 0.9\n"),
            forceRequested = true
        )
        assertEquals(listOf("busybox"), plan.map { it.name })
    }

    @Test
    fun refusesToInstallOverAnInstalledConflict() {
        val error = assertThrows(IllegalStateException::class.java) {
            resolver.resolve(
                listOf("other"),
                repo,
                installed("Package: busybox\nStatus: install ok installed\nVersion: 1.0\n"),
                forceRequested = false
            )
        }
        assertTrue(error.message, error.message!!.contains("conflicts with installed busybox"))
    }

    @Test
    fun refusesTwoPlannedPackagesThatConflict() {
        val error = assertThrows(IllegalStateException::class.java) {
            resolver.resolve(listOf("busybox", "other"), repo, emptyMap(), forceRequested = false)
        }
        assertTrue(error.message, error.message!!.contains("conflicts"))
    }

    @Test
    fun dependencyCyclesResolveInsteadOfRecursingForever() {
        val cyclic = index(
            """
            Package: a
            Version: 1
            Architecture: all
            Filename: a.deb
            Depends: b

            Package: b
            Version: 1
            Architecture: all
            Filename: b.deb
            Depends: a
            """
        )
        val plan = resolver.resolve(listOf("a"), cyclic, emptyMap(), forceRequested = false)
        assertEquals(setOf("a", "b"), plan.map { it.name }.toSet())
    }

    @Test
    fun unknownPackageAndUnsatisfiableDependencyAreErrors() {
        assertThrows(IllegalStateException::class.java) {
            resolver.resolve(listOf("nope"), repo, emptyMap(), forceRequested = false)
        }
        val broken = index("Package: x\nVersion: 1\nArchitecture: all\nFilename: x.deb\nDepends: missing\n")
        assertThrows(IllegalStateException::class.java) {
            resolver.resolve(listOf("x"), broken, emptyMap(), forceRequested = false)
        }
    }

    @Test
    fun versionedRelationsAreCheckedAgainstTheCandidate() {
        val tooOld = index(
            """
            Package: app
            Version: 1
            Architecture: all
            Filename: app.deb
            Depends: lib (>= 3.0)

            Package: lib
            Version: 1.6
            Architecture: all
            Filename: lib.deb
            """
        )
        assertThrows(IllegalStateException::class.java) {
            resolver.resolve(listOf("app"), tooOld, emptyMap(), forceRequested = false)
        }
    }

    // --- control-file parsing -------------------------------------------------------------

    @Test
    fun parseRelation_stripsArchitectureAndProfileRestrictions() {
        val relation = DebControl.parseRelation("libfoo:any (>= 1.2) [arm64] <!nocheck>")!!
        assertEquals("libfoo", relation.name)
        assertEquals(">=", relation.operator)
        assertEquals("1.2", relation.version)
    }

    @Test
    fun parseInstalled_ignoresHalfInstalledPackages() {
        val parsed = installed(
            "Package: ok\nStatus: install ok installed\nVersion: 1\n\n" +
                "Package: half\nStatus: install ok half-configured\nVersion: 1\n"
        )
        assertEquals(setOf("ok"), parsed.keys)
    }

    // --- pkg argument handling (M-4) ---------------------------------------------------------

    @Test
    fun pkgArgs_reinstallIsAnInstallOnlyForceFlag() {
        val parsed = PkgArgs.parse("install", listOf("--reinstall", "git", "-y"))
        assertEquals(listOf("git"), parsed.packages)
        assertTrue(parsed.reinstall)
        assertFalse(PkgArgs.parse("install", listOf("git")).reinstall)
        assertThrows(IllegalStateException::class.java) { PkgArgs.parse("remove", listOf("--reinstall", "git")) }
    }

    @Test
    fun pkgArgs_unknownOptionsAreRejectedNotSilentlyDropped() {
        // `--dry-run` used to be dropped, turning a simulation into a real install.
        val error = assertThrows(IllegalStateException::class.java) {
            PkgArgs.parse("install", listOf("--dry-run", "git"))
        }
        assertTrue(error.message, error.message!!.contains("--dry-run"))
    }

    @Test
    fun pkgArgs_upgradeAcceptsNamesAndNoNames() {
        assertEquals(emptyList<String>(), PkgArgs.parse("upgrade", listOf("-y")).packages)
        assertEquals(listOf("git", "curl"), PkgArgs.parse("upgrade", listOf("git", "curl")).packages)
    }
}
