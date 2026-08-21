package com.hereliesaz.hg2gui.ui.menu

import kotlin.random.Random

/**
 * How a stack of pills arrives. Only arrivals vary - every stack still LEAVES by the sweep
 * left, so the departure stays learnable. Resolved once per stack and handed down: a pill
 * that picked for itself could disagree with the row beneath it.
 */
enum class StackEntrance(val weight: Int) {
    Slide(4), // the house arrival; deliberately the most common
    Unfold(3),
    Drop(3),
    Cascade(2),
    Deal(2),
    Telescope(2),
    Split(1),
    Rally(1);

    companion object {
        private var last: StackEntrance? = null

        /** Excludes whatever ran last, so the variation is actually perceptible. */
        fun roll(random: Random = Random.Default): StackEntrance {
            val pool = entries.filter { it != last }
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

/** Per-row interval, halved past six rows so a tall stack never runs long. */
fun stackInterval(base: Int, rowCount: Int): Int =
    if (rowCount > 6) base / 2 else base
