package com.hereliesaz.hg2gui.ui.guide

/*
 * Pure helpers behind the Guide reader: the entry-wipe schedule and the spoken/shown wording of a
 * command tap. Kept free of Compose so the numbers and strings can be unit-tested.
 */

/** Gap between consecutive blocks of an entry wiping in. */
internal const val GUIDE_STAGGER_STEP_MS = 60L

/** The latest any block may *start* wiping in, however many blocks the entry has. */
internal const val GUIDE_STAGGER_MAX_MS = 240L

/** Hard ceiling on the whole entry wipe (last start + its own duration). Tapping skips it sooner. */
internal const val GUIDE_WIPE_TOTAL_CAP_MS = 600L

/** How long the "added to input" confirmation stays up. Long enough to read, short enough to clear. */
internal const val GUIDE_CONFIRMATION_MS = 2200L

/** When block number [seq] (0 = the first) starts its wipe, in ms: linear, then flat at the cap. */
internal fun guideWipeDelayMs(seq: Int): Long =
    (seq.coerceAtLeast(0) * GUIDE_STAGGER_STEP_MS).coerceAtMost(GUIDE_STAGGER_MAX_MS)

/** Length of one block's wipe. Wide prose blocks sweep a little longer than the small chips. */
internal fun guideWipeDurationMs(wide: Boolean): Int = if (wide) 300 else 220

/** The transient chip shown after a command is handed to the input. */
internal fun guideAddedMessage(command: String): String = "Added “${command.trim()}” to input"

/** What a screen reader says for a control that adds [command] to the command being composed. */
internal fun guideAddActionLabel(command: String): String = "Add ${command.trim()} to command"
