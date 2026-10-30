package com.hereliesaz.hg2gui.mcp

/**
 * Collects what a `TerminalEngine.run` flow emits into one result string for the two headless
 * callers (the MCP `shell.exec` tool and the signature API's EXECUTE).
 *
 * The flow has two shapes. Commands that run in the shell emit the WHOLE terminal transcript each
 * time (every emission supersedes the previous one - appending them duplicates every earlier line),
 * while the package manager, downloader and builtins emit one new line or block per emission
 * (appending is right and keeping only the last one drops everything before it). Neither shape is
 * labelled, so [add] tells them apart per emission: a chunk that starts with the previous chunk is
 * a longer snapshot of the same transcript and replaces what was collected; anything else is new
 * text and is appended on a new line. [markFinalSnapshot] is the unambiguous signal: the shell path
 * reports its styled transcript exactly once, after its last text emission, so when a caller sees
 * that callback the last chunk IS the complete output and wins outright.
 *
 * Output is capped at [maxChars] (the head is kept, matching how both callers truncated before).
 * Pure Kotlin so it is unit-tested without Android - see CommandOutputAccumulatorTest.
 */
class CommandOutputAccumulator(private val maxChars: Int) {
    private var merged = ""
    private var last = ""
    private var finalSnapshot = false

    /** Feeds one emission of the engine's output flow. */
    fun add(chunk: String) {
        when {
            last.isNotEmpty() && chunk.startsWith(last) -> merged = chunk
            merged.isEmpty() -> merged = chunk
            merged.length < maxChars -> merged = merged + "\n" + chunk
        }
        last = chunk
        if (merged.length > maxChars) merged = merged.take(maxChars)
    }

    /** The shell path finished: the last emission is the complete transcript, not a delta. */
    fun markFinalSnapshot() {
        finalSnapshot = true
    }

    /** The collected output, at most [maxChars] long. */
    fun text(): String = (if (finalSnapshot) last else merged).take(maxChars)
}
