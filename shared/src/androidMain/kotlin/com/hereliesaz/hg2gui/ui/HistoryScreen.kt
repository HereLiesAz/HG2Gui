package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.managers.CommandHistoryEntry
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush

private const val SECONDS_PER_MINUTE = 60
private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
private const val MILLIS_PER_SECOND = 1000

/**
 * W5 (docs/HG2Gui Termux Coverage.dc.html): "no cross-session scrollback/history search" - every
 * tab's own up/down-arrow recall (SessionUiState.commandHistory) is in-memory and per-session;
 * this searches CommandHistoryStore's persistent, cross-session log instead. Selecting a result
 * hands the command text back to the caller (dropped into the active tab's input, same as arrow
 * recall already does) rather than re-running it directly - the user should see what they're
 * about to run before it runs, same as recalling it with the up arrow already works today.
 *
 * The log can hold secrets typed on a command line, so the screen is also where it is erased:
 * each row has its own DELETE and the header has CLEAR (behind a [ConfirmDialog]).
 * [loading] is true until the first read returns, so a slow read shows "Loading" rather than
 * flashing "No commands recorded yet." before the list appears.
 */
@Suppress("LongParameterList") // Every param is a genuinely distinct value the caller must
// supply - the search box, the results list, and the back pill below all need their own slice
// of it, so bundling would just move the same count into a wrapper class.
@Composable
fun HistoryScreen(
    entries: List<CommandHistoryEntry>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
    fullscreen: Boolean,
    nowMillis: Long,
    modifier: Modifier = Modifier,
    /** True while the log is still being read; suppresses the empty-state message. */
    loading: Boolean = false,
    /** Removes one entry from the persistent log. */
    onDelete: (CommandHistoryEntry) -> Unit = {},
    /** Erases the entire log (asked to confirm first). */
    onClear: () -> Unit = {}
) {
    var confirmClear by remember { mutableStateOf(false) }
    val muted = statusColor(StatusKind.Muted)

    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("‹ Back", onClick = onBack, contentDescription = "Back to settings")
            if (entries.isNotEmpty() || query.isNotBlank()) {
                AzphaltChip(
                    "Clear history", onClick = { confirmClear = true },
                    tint = statusColor(StatusKind.Error),
                    contentDescription = "Clear all command history"
                )
            }
        }

        AzphaltHeading("History", Modifier.padding(horizontal = 20.dp, vertical = 6.dp))

        HistorySearchBox(query, onQueryChange, resultCount = entries.size)

        if (loading) {
            Text(
                "Loading history…",
                color = muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).liveRegionPolite()
            )
        } else if (entries.isEmpty()) {
            Text(
                if (query.isBlank()) "No commands recorded yet." else "No matches for “$query”.",
                color = muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
            // Index in the key: the same command can be logged twice in one millisecond, and a
            // duplicate LazyColumn key crashes.
            itemsIndexed(entries, key = { index, it -> "$index:${it.timestampMs}:${it.command}" }) { _, entry ->
                HistoryRow(entry, nowMillis, muted, onClick = { onSelect(entry.command) }, onDelete = { onDelete(entry) })
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear history?",
            message = "Erases every recorded command from every tab. Commands you already ran are " +
                "not affected, but they can no longer be searched or recalled from here.",
            confirmLabel = "CLEAR",
            onConfirm = { confirmClear = false; onClear() },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun HistorySearchBox(query: String, onQueryChange: (String) -> Unit, resultCount: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink.copy(alpha = .10f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (query.isEmpty()) {
            Text(
                "SEARCH $resultCount COMMANDS", color = statusColor(StatusKind.Muted),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = Azphalt.currentGround.onPage, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Azphalt.currentGround.onPage),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search command history" }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: CommandHistoryEntry,
    nowMillis: Long,
    muted: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClickLabel = "Put in the command line", onClick = onClick)
                .padding(vertical = 10.dp)
        ) {
            Text(
                entry.command, color = Azphalt.currentGround.onPage,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${entry.sessionLabel} · ${relativeTime(nowMillis - entry.timestampMs)}",
                color = muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        AzphaltChip(
            "Delete", onClick = onDelete,
            contentDescription = "Delete ${entry.command} from history"
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.currentGround.onPage.copy(alpha = .12f)))
}

private fun relativeTime(agoMs: Long): String {
    val seconds = (agoMs / MILLIS_PER_SECOND).coerceAtLeast(0)
    val minutes = seconds / SECONDS_PER_MINUTE
    val hours = minutes / MINUTES_PER_HOUR
    val days = hours / HOURS_PER_DAY
    return when {
        seconds < SECONDS_PER_MINUTE -> "just now"
        minutes < MINUTES_PER_HOUR -> "${minutes}m ago"
        hours < HOURS_PER_DAY -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
