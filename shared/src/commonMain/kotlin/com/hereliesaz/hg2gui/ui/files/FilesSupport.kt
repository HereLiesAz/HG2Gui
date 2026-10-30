package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.contrastingText
import com.hereliesaz.hg2gui.ui.minTouchSize
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType

/*
 * Pieces shared by the Files screen, the Storage screen and the two pickers: the trash-retention
 * wording (pure, unit-tested), the type styles those screens read from the Azphalt scale instead
 * of literal sp values, and the three small composables (row action, error banner, status label)
 * that used to be copy-pasted per screen.
 */

/**
 * How long a trashed item survives before it is deleted for good. Mirrors
 * `VfsManager.TRASH_RETENTION_MILLIS` (androidMain, 30 days), which commonMain cannot reach; keep
 * the two in step - this number is what the user is told.
 */
internal const val TRASH_RETENTION_DAYS = 30
private const val MS_PER_DAY = 24L * 60 * 60 * 1000

/** "1 day" / "n days". */
internal fun daysLabel(days: Int): String = if (days == 1) "1 day" else "$days days"

/**
 * The trash row's retention line: "deleted 5 days ago - purged in 25 days". Whole days, rounded
 * down for the age and therefore up for the time left, so the row never promises more time than
 * the item has. Falls back to the plain rule when either timestamp is unknown (0).
 */
internal fun trashAgeDescription(deletedAtMillis: Long, nowMillis: Long): String {
    if (deletedAtMillis <= 0L || nowMillis <= 0L) {
        return "purged ${daysLabel(TRASH_RETENTION_DAYS)} after it was deleted"
    }
    val ageDays = ((nowMillis - deletedAtMillis).coerceAtLeast(0L) / MS_PER_DAY).toInt()
    val remaining = (TRASH_RETENTION_DAYS - ageDays).coerceAtLeast(0)
    val deleted = when (ageDays) {
        0 -> "deleted today"
        1 -> "deleted 1 day ago"
        else -> "deleted $ageDays days ago"
    }
    val purge = if (remaining == 0) "purging now" else "purged in ${daysLabel(remaining)}"
    return "$deleted - $purge"
}

/** The sentence every "move to trash" confirmation ends with, so the 30-day rule is never a surprise. */
internal fun trashRetentionNote(): String =
    "Trashed items are deleted for good after ${daysLabel(TRASH_RETENTION_DAYS)}."

/** Confirm text for moving one entry to the trash (Files and Storage share it). */
internal fun trashConfirmMessage(name: String, isDirectory: Boolean): String {
    val what = if (isDirectory) "$name and everything inside it" else name
    return "This moves $what to the trash. ${trashRetentionNote()}"
}

/** Confirm text for the multi-select delete. */
internal fun trashBatchConfirmMessage(): String =
    "This moves everything selected, including the contents of any selected folders, to the " +
        "trash. ${trashRetentionNote()}"

/**
 * Moves keyboard focus to [requester]. A no-op instead of a crash when the field it was attached
 * to has already left the composition (the prompt was cancelled in the same frame it opened).
 */
internal fun requestFocusSafely(requester: FocusRequester) {
    try {
        requester.requestFocus()
    } catch (ignored: IllegalStateException) {
        // The target is gone; there is nothing to focus.
    }
}

/** Real-case name text (file and folder names are literals, not labels). */
@Composable
internal fun nameTextStyle(): TextStyle = LocalAzphaltType.current.chip.copy(letterSpacing = 0.02.em)

/** Sentence-case secondary text: sizes, ages, paths. 11sp, the floor of the scale. */
@Composable
internal fun captionTextStyle(): TextStyle =
    LocalAzphaltType.current.endCap.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.02.em)

/** Upper-case micro labels: section captions, status words. 11sp, the floor of the scale. */
@Composable
internal fun labelTextStyle(): TextStyle = LocalAzphaltType.current.endCap

/**
 * A text-only row action (RENAME, SHARE, TRASH, ...): at least 48 x 48dp as a hit box, spoken as
 * [description] instead of the bare word so a screen-reader user hears which row it belongs to.
 */
@Composable
internal fun RowAction(
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val min = minTouchSize()
    Box(
        modifier
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = min, minHeight = min)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, style = labelTextStyle(), maxLines = 1)
    }
}

/**
 * The transient failure banner both Files and Storage show. A polite live region (a screen reader
 * announces the failure without moving focus), at least 48dp tall, tap to dismiss.
 */
@Composable
internal fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val bg = Azphalt.hues[ERROR_HUE]
    val fg = bg.contrastingText()
    Row(
        modifier
            .fillMaxWidth()
            .liveRegionPolite()
            .heightIn(min = minTouchSize())
            .clip(AzphaltSurface.note)
            .background(bg)
            .clickable(onClickLabel = "Dismiss", role = Role.Button, onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            message, color = fg, style = captionTextStyle().copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.weight(1f)
        )
        Text("DISMISS", color = fg, style = labelTextStyle())
    }
}

private const val ERROR_HUE = 6

/** A muted one-line status word on the page (LOADING..., NOTHING HERE, ...), contrast-safe. */
@Composable
internal fun StatusLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = statusColor(StatusKind.Muted), style = labelTextStyle(), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = modifier)
}
