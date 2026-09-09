package com.hereliesaz.hg2gui.terminal

import android.content.Context

/**
 * Coordinates per-package rollback journals as one logical package-manager transaction.
 *
 * Journals stay live until the complete install/remove plan succeeds. A failure rolls every
 * mutation back in reverse order. Process death leaves those journals on disk; the next package
 * operation recovers them through [PackageTransactionJournal.recoverAbandoned].
 */
class PackageTransactionGroup(private val context: Context) {
    private val journals = mutableListOf<PackageTransactionJournal>()
    private var finished = false

    fun begin(packageName: String): PackageTransactionJournal {
        check(!finished) { "Package transaction group is already finished" }
        return PackageTransactionJournal.begin(context, packageName).also(journals::add)
    }

    fun commit() {
        if (finished) return
        journals.forEach(PackageTransactionJournal::commit)
        finished = true
    }

    fun rollback(): List<String> {
        if (finished) return emptyList()
        val failures = mutableListOf<String>()
        journals.asReversed().forEach { journal ->
            journal.rollback().exceptionOrNull()?.let { failures += it.message ?: it::class.java.simpleName }
        }
        finished = true
        return failures
    }
}
