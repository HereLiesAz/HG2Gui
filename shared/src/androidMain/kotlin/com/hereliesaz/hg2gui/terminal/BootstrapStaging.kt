package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Stage-verify-swap installation of a bootstrap prefix.
 *
 * A bootstrap is extracted into a sibling `usr.staging` directory, checked, and only then renamed
 * over `usr`. The previous prefix (if any) is renamed to `usr.previous` first and deleted only
 * after the new one is in place, so a failure at ANY step - offline, SHA mismatch, full storage,
 * process death - can never leave the user without their existing packages. This replaces the old
 * "delete the whole prefix on any failure" behaviour, which turned a flaky network into total loss
 * of every installed package and the dpkg database.
 *
 * All of this lives next to `usr` under `filesDir` so each step is a same-filesystem, atomic
 * directory rename.
 */
object BootstrapStaging {
    private val SWAP_LOCK = Any()

    fun stagingDir(context: Context): File = File(context.filesDir, "usr.staging")
    fun backupDir(context: Context): File = File(context.filesDir, "usr.previous")

    /**
     * Finishes the one step a crash can leave half-done: the old prefix was moved to [backup] but
     * the staged one never became [prefix]. Restores the old prefix so the user is back where they
     * started. A no-op (false) whenever [prefix] exists or there is no [backup]. Cheap enough for
     * a hot path: two `exists()` checks.
     */
    fun recoverInterruptedSwap(prefix: File, backup: File): Boolean = synchronized(SWAP_LOCK) {
        if (prefix.exists() || !backup.exists()) return@synchronized false
        try {
            move(backup, prefix)
            true
        } catch (_: IOException) {
            false
        }
    }

    /**
     * Throws unless the staged tree looks like a working prefix: a `bin/` directory, an
     * executable `bin/bash` (the same bar [DistroManager.isInstalled] applies to a live install)
     * and the archive's own `var/lib/dpkg/status`.
     * [requireExecutableBash] exists for host tests, where there is no exec-exempt native library
     * to link against.
     */
    fun verify(staged: File, requireExecutableBash: Boolean = true) {
        if (!File(staged, "bin").isDirectory) throw IOException("Bootstrap archive produced no bin/ directory")
        val bash = File(staged, "bin/bash")
        if (!bash.exists()) throw IOException("Bootstrap archive produced no bin/bash")
        if (requireExecutableBash && !bash.canExecute()) throw IOException("Staged bin/bash is not executable")
        // bin/bash alone proves nothing: the exec-exempt links are created from the APK's manifest
        // even when the archive itself yielded nothing. dpkg's status file only comes from the
        // archive, so requiring it stops an empty/garbled extraction replacing a working prefix.
        if (!File(staged, "var/lib/dpkg/status").isFile) throw IOException("Bootstrap archive produced no dpkg database")
    }

    /**
     * Atomically makes [staging] the new [prefix]. If [prefix] exists it is first renamed to
     * [backup]; if installing the staged tree then fails, the old prefix is renamed back. The
     * backup is deleted only after success. Any failure leaves [prefix] exactly as it was before
     * the call (or, if the process dies mid-way, recoverable via [recoverInterruptedSwap]).
     */
    fun swapIntoPlace(prefix: File, staging: File, backup: File) = synchronized(SWAP_LOCK) {
        if (!staging.isDirectory) throw IOException("No staged bootstrap at ${staging.path}")
        // A backup next to a MISSING prefix is the recovery source from an interrupted swap, not
        // garbage: put it back before doing anything else.
        if (!prefix.exists() && backup.exists()) move(backup, prefix)
        if (backup.exists() && !FileTrees.deleteRecursivelyNoFollow(backup)) {
            throw IOException("Cannot clear stale ${backup.path}")
        }
        val hadPrefix = prefix.exists()
        if (hadPrefix) move(prefix, backup)
        try {
            move(staging, prefix)
        } catch (t: Throwable) {
            if (hadPrefix) {
                // The staged tree never landed; put the original back exactly where it was.
                runCatching { move(backup, prefix) }
            }
            throw t
        }
        if (hadPrefix) FileTrees.deleteRecursivelyNoFollow(backup)
    }

    private fun move(from: File, to: File) {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: AtomicMoveNotSupportedException) {
            throw IOException("Atomic directory rename unavailable for ${from.path} -> ${to.path}", e)
        }
    }
}
