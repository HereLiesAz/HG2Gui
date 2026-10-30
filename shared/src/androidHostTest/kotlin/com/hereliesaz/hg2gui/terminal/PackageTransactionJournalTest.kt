package com.hereliesaz.hg2gui.terminal

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PackageTransactionJournalTest {

    private class Fixture(val root: File) {
        val prefix = File(root, "usr").apply { mkdirs() }
        val journalRoot = File(root, "nobackup/hg2-package-transactions")
        val legacyRoot = File(root, "cache/hg2-package-transactions")

        fun env(
            pid: Int = 100,
            token: String = "this-process",
            alive: (Int) -> Boolean = { false },
            space: (File) -> Long = { Long.MAX_VALUE },
            journals: File = journalRoot,
            legacy: List<File> = listOf(legacyRoot)
        ) = PackageTransactionJournal.Env(
            prefix = prefix,
            journalRoot = journals,
            legacyRoots = legacy,
            pid = pid,
            isProcessAlive = alive,
            usableSpace = space,
            processToken = token
        )

        /** An installed package: payload file, dpkg list, and the dpkg status store. */
        fun installPackage(name: String, payload: String) {
            val file = prefix.write("bin/$name", payload)
            prefix.write("var/lib/dpkg/info/$name.list", "${file.absolutePath}\n")
            val status = File(prefix, "var/lib/dpkg/status")
            status.parentFile.mkdirs()
            status.appendText("Package: $name\nStatus: install ok installed\n\n")
        }

        /** Simulates a package manager mutating [name] mid-transaction. */
        fun mutate(name: String) {
            File(prefix, "bin/$name").writeText("MUTATED")
            val extra = prefix.write("bin/$name-extra", "new file")
            File(prefix, "var/lib/dpkg/info/$name.list").appendText("${extra.absolutePath}\n")
            File(prefix, "var/lib/dpkg/status").appendText("Package: $name-mutation\n\n")
        }

        fun payload(name: String) = File(prefix, "bin/$name").readText()
        fun status() = File(prefix, "var/lib/dpkg/status").readText()
        fun activeEntries(root: File = journalRoot) = root.listFiles().orEmpty().map { it.name }
    }

    @Test
    fun rollback_restoresPayloadStatusAndInfo_andRemovesTheJournal() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "original")
            val statusBefore = fx.status()
            val journal = PackageTransactionJournal.begin(fx.env(), "alpha", groupDir = null, index = 1)
            fx.mutate("alpha")

            assertTrue(journal.rollback().isSuccess)

            assertEquals("original", fx.payload("alpha"))
            assertFalse(File(fx.prefix, "bin/alpha-extra").exists())
            assertEquals(statusBefore, fx.status())
            assertEquals(emptyList<String>(), fx.activeEntries())
        }
    }

    @Test
    fun standaloneCommit_leavesNoJournalAndNoCommittedLeftover() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "original")
            val journal = PackageTransactionJournal.begin(fx.env(), "alpha", groupDir = null, index = 1)
            fx.mutate("alpha")
            journal.commit()

            assertEquals("MUTATED", fx.payload("alpha"))
            assertEquals(emptyList<String>(), fx.activeEntries())
        }
    }

    @Test
    fun groupRollback_restoresEveryPackageInReverseOrder() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            fx.installPackage("beta", "beta-original")
            val statusBefore = fx.status()
            val group = PackageTransactionGroup(fx.env())
            group.begin("alpha")
            fx.mutate("alpha")
            group.begin("beta")
            fx.mutate("beta")

            assertEquals(emptyList<String>(), group.rollback())

            assertEquals("alpha-original", fx.payload("alpha"))
            assertEquals("beta-original", fx.payload("beta"))
            assertEquals(statusBefore, fx.status())
            assertEquals(emptyList<String>(), fx.activeEntries())
        }
    }

    @Test
    fun groupCommit_isOneRename_andLeavesNothingToRecover() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            fx.installPackage("beta", "beta-original")
            val group = PackageTransactionGroup(fx.env())
            group.begin("alpha")
            fx.mutate("alpha")
            group.begin("beta")
            fx.mutate("beta")
            group.commit()

            assertEquals(emptyList<String>(), fx.activeEntries())
            // A later recovery must be a no-op: a committed plan is never rolled back.
            assertEquals(emptyList<String>(), PackageTransactionJournal.recoverAbandoned(fx.env(pid = 200, token = "next")))
            assertEquals("MUTATED", fx.payload("alpha"))
            assertEquals("MUTATED", fx.payload("beta"))
        }
    }

    @Test
    fun crashAfterCommitRename_isNotRolledBack_andLeftoverIsCleaned() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            fx.installPackage("beta", "beta-original")
            val env = fx.env()
            val group = PackageTransactionGroup(env)
            group.begin("alpha")
            fx.mutate("alpha")
            group.begin("beta")
            fx.mutate("beta")

            // The atomic commit rename happened, then the process died before the cleanup deleted
            // the renamed directory. (Previously: a crash between per-journal deletes left SOME
            // journals of a completed plan behind, and recovery rolled those back.)
            val active = fx.journalRoot.listFiles()!!.single()
            assertTrue(active.renameTo(File(fx.journalRoot, ".committed-${active.name}")))

            val messages = PackageTransactionJournal.recoverAbandoned(fx.env(pid = 200, token = "next"))

            assertEquals(emptyList<String>(), messages)
            assertEquals("MUTATED", fx.payload("alpha"))
            assertEquals("MUTATED", fx.payload("beta"))
            assertEquals(emptyList<String>(), fx.activeEntries())
        }
    }

    @Test
    fun recoverAbandoned_rollsBackAnUncommittedGroupFromADeadProcess() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            val statusBefore = fx.status()
            val group = PackageTransactionGroup(fx.env(pid = 100, token = "old"))
            group.begin("alpha")
            fx.mutate("alpha")
            // ...process dies here: no commit, no rollback.

            val messages = PackageTransactionJournal.recoverAbandoned(fx.env(pid = 200, token = "new", alive = { false }))

            assertEquals(listOf("Recovered interrupted package transaction for alpha."), messages)
            assertEquals("alpha-original", fx.payload("alpha"))
            assertEquals(statusBefore, fx.status())
            assertEquals(emptyList<String>(), fx.activeEntries())
        }
    }

    @Test
    fun recoverAbandoned_leavesAJournalOwnedByALiveForeignProcessAndReportsFailure() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            val group = PackageTransactionGroup(fx.env(pid = 100, token = "other"))
            group.begin("alpha")
            fx.mutate("alpha")

            val messages = PackageTransactionJournal.recoverAbandoned(fx.env(pid = 200, token = "new", alive = { it == 100 }))

            assertEquals(1, messages.size)
            assertTrue(messages.single(), messages.single().startsWith("Could not recover"))
            assertEquals("MUTATED", fx.payload("alpha"))
            assertEquals(1, fx.activeEntries().size)
        }
    }

    @Test
    fun legacyCacheDirJournals_areMigratedAndStillRecovered() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            // An older build wrote its journal directly under cacheDir, without a group directory.
            val legacyWriter = fx.env(pid = 100, token = "old-build", journals = fx.legacyRoot, legacy = emptyList())
            val journal = PackageTransactionJournal.begin(legacyWriter, "alpha", groupDir = null, index = 1)
            fx.mutate("alpha")
            // Flatten to the legacy layout: <root>/<journal> instead of <root>/<group>/<journal>.
            val group = fx.legacyRoot.listFiles()!!.single()
            val flat = group.listFiles()!!.single { it.isDirectory }
            assertTrue(flat.renameTo(File(fx.legacyRoot, "alpha-123")))
            group.deleteRecursively()
            assertNotEquals(null, journal)

            val env = fx.env(pid = 200, token = "new")
            PackageTransactionJournal.migrateLegacyJournals(env)
            assertEquals(listOf("alpha-123"), fx.activeEntries())
            assertFalse("legacy cache location is emptied", fx.legacyRoot.exists())

            val messages = PackageTransactionJournal.recoverAbandoned(env)
            assertEquals(listOf("Recovered interrupted package transaction for alpha."), messages)
            assertEquals("alpha-original", fx.payload("alpha"))
        }
    }

    @Test
    fun begin_refusesWhenTheVolumeCannotHoldTheSnapshot_andLeavesNothingBehind() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "x".repeat(10_000))
            try {
                PackageTransactionJournal.begin(fx.env(space = { 1024L }), "alpha", groupDir = null, index = 1)
                fail("expected a free-space failure")
            } catch (e: IllegalStateException) {
                assertTrue(e.message, e.message!!.contains("not enough free storage"))
            }
            assertEquals(emptyList<String>(), fx.activeEntries())
            assertEquals("x".repeat(10_000), fx.payload("alpha"))
        }
    }

    @Test
    fun groupBegin_failingOnSpace_leavesNoPartialJournal() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            val group = PackageTransactionGroup(fx.env(space = { 0L }))
            try {
                group.begin("alpha")
                fail("expected a free-space failure")
            } catch (_: IllegalStateException) {
            }
            assertEquals(emptyList<String>(), group.rollback())
            assertEquals(emptyList<String>(), fx.activeEntries())
        }
    }

    @Test
    fun estimateSnapshotBytes_countsPayloadStatusAndInfo() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "12345")
            val info = File(fx.prefix, "var/lib/dpkg/info")
            val estimate = PackageTransactionJournal.estimateSnapshotBytes(
                fx.prefix,
                "alpha",
                File(info, "alpha.list"),
                File(fx.prefix, "var/lib/dpkg/status"),
                info
            )
            val expectedMinimum = 5L + File(fx.prefix, "var/lib/dpkg/status").length() + File(info, "alpha.list").length()
            assertEquals(expectedMinimum, estimate)
        }
    }

    @Test
    fun discardAll_dropsEveryJournalWithoutReplayingIt() {
        withHg2TempDir { dir ->
            val fx = Fixture(dir)
            fx.installPackage("alpha", "alpha-original")
            val group = PackageTransactionGroup(fx.env())
            group.begin("alpha")
            fx.mutate("alpha")

            PackageTransactionJournal.discardAll(fx.env())

            assertEquals(emptyList<String>(), fx.activeEntries())
            assertEquals("MUTATED", fx.payload("alpha"))
        }
    }
}
