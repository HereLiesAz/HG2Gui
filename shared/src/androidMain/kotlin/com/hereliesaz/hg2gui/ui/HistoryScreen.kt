package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "‹ BACK", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        Eyebrow("History")

        HistorySearchBox(query, onQueryChange, resultCount = entries.size)

        if (entries.isEmpty()) {
            Text(
                if (query.isBlank()) "No commands recorded yet." else "No matches for “$query”.",
                color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
            items(entries, key = { "${it.timestampMs}:${it.command}" }) { entry ->
                HistoryRow(entry, nowMillis, onClick = { onSelect(entry.command) })
            }
        }
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (query.isEmpty()) {
            Text(
                "SEARCH $resultCount COMMANDS", color = Azphalt.currentGround.onPage.copy(alpha = .45f),
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(color = Azphalt.currentGround.onPage, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(Azphalt.currentGround.onPage),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun HistoryRow(entry: CommandHistoryEntry, nowMillis: Long, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            entry.command, color = Azphalt.currentGround.onPage,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold
        )
        Text(
            "${entry.sessionLabel} · ${relativeTime(nowMillis - entry.timestampMs)}",
            color = Azphalt.currentGround.onPage.copy(alpha = .55f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
    Box(Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .12f)))
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
