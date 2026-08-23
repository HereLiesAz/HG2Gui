package com.hereliesaz.hg2gui.ui.menu

import kotlin.random.Random

// Lottery tickets, not a percentage - see StackEntrance.roll(). Named rather than left as bare
// enum-constructor literals so the "reads as the app's own vs. seasoning" intent survives a
// glance at the declaration below, not just the doc comment above it.
private const val WEIGHT_HOUSE = 4
private const val WEIGHT_COMMON = 3
private const val WEIGHT_OCCASIONAL = 2
private const val WEIGHT_RARE = 1

/**
 * How a stack of pills arrives. Only arrivals vary - every stack still LEAVES by the sweep
 * left, so the departure stays learnable. Resolved once per stack and handed down: a pill
 * that picked for itself could disagree with the row beneath it.
 */
enum class StackEntrance(val weight: Int) {
    Slide(WEIGHT_HOUSE), // the house arrival; deliberately the most common
    Unfold(WEIGHT_COMMON),
    Drop(WEIGHT_COMMON),
    Cascade(WEIGHT_OCCASIONAL),
    Deal(WEIGHT_OCCASIONAL),
    Telescope(WEIGHT_OCCASIONAL),
    Split(WEIGHT_RARE),
    Rally(WEIGHT_RARE),
    Extend(WEIGHT_OCCASIONAL),
    Unroll(WEIGHT_OCCASIONAL),
    Tumble(WEIGHT_OCCASIONAL);

    companion object {
        private var last: StackEntrance? = null

        // Unroll and Tumble are a root-stack-only flourish - a wide, staggered wave reads fine
        // sweeping across a whole screen of rows, but a child band is only ever a handful of
        // pills tall, where that same overlap reads as indistinguishable from Cascade itself.
        // Cascade already covers "the child hinge" for a band; these two are the ROOT borrowing
        // it back, not something a band needs a version of too.
        private val CHILD_BAND_POOL = entries.filterNot { it == Unroll || it == Tumble }

        /** Excludes whatever ran last, so the variation is actually perceptible. Shared across
         *  root and child-band rolls on purpose - a child band opening still "spends" a turn, so
         *  the root doesn't keep re-rolling the same handful of variants around it. */
        fun roll(random: Random = Random.Default): StackEntrance = rollFrom(entries, random)

        /** Same lottery, restricted to the entrances a child band can actually use - see
         *  [CHILD_BAND_POOL]'s own doc comment. */
        fun rollForChildBand(random: Random = Random.Default): StackEntrance = rollFrom(CHILD_BAND_POOL, random)

        private fun rollFrom(candidates: List<StackEntrance>, random: Random): StackEntrance {
            val pool = candidates.filter { it != last }
            var n = random.nextInt(pool.sumOf { it.weight })
            for (e in pool) {
                if (n < e.weight) {
                    last = e
                    return e
                }
                n -= e.weight
            }
            return pool.last().also { last = it }
        }
    }
}

private const val ROW_CAP = 6

/** Per-row interval, halved past six rows so a tall stack never runs long. */
fun stackInterval(base: Int, rowCount: Int): Int =
    if (rowCount > ROW_CAP) base / 2 else base
