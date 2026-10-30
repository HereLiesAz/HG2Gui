package com.hereliesaz.hg2gui.terminal

import android.content.Context
import java.io.File

/**
 * Coordinates per-package rollback journals as one logical package-manager transaction.
 *
 * Journals stay live until the complete install/remove plan succeeds. A failure rolls every
 * mutation back in reverse order. Process death leaves those journals on disk; the next package
 * operation recovers them through [PackageTransactionJournal.recoverAbandoned].
 *
 * All journals of the plan live in one group directory, and [commit] retires the plan with a single
 * atomic rename of that directory (see [PackageTransactionJournal]) - never a loop of deletes that a
 * crash could interrupt halfway.
 */
class PackageTransactionGroup internal constructor(private val env: PackageTransactionJournal.Env) {
    constructor(context: Context) : this(PackageTransactionJournal.Env.forContext(context))

    private val journals = mutableListOf<PackageTransactionJournal>()
    private var groupDir: File? = null
    private var finished = false

    fun begin(packageName: String): PackageTransactionJournal {
        check(!finished) { "Package transaction group is already finished" }
        val dir = groupDir ?: PackageTransactionJournal.newGroupDir(env).also { groupDir = it }
        return PackageTransactionJournal.begin(env, packageName, dir, journals.size + 1).also(journals::add)
    }

    /**
     * Atomically retires the whole plan. If the rename itself fails the group is left intact and
     * still active, so the caller's failure path can [rollback] it.
     */
    fun commit() {
        if (finished) return
        groupDir?.let(PackageTransactionJournal::commitDirectory)
        journals.forEach(PackageTransactionJournal::markCommitted)
        finished = true
    }

    fun rollback(): List<String> {
        if (finished) return emptyList()
        val failures = mutableListOf<String>()
        journals.asReversed().forEach { journal ->
            journal.rollback().exceptionOrNull()?.let { failures += it.message ?: it::class.java.simpleName }
        }
        finished = true
        // Only a fully rolled-back group may disappear; a failed journal must stay on disk so the
        // next recovery pass can retry it.
        if (failures.isEmpty()) groupDir?.let { FileTrees.deleteRecursivelyNoFollow(it) }
        return failures
    }
}
