package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/*
 * Storage broken down by content type - real numbers from a recursive walk of the sandbox. The
 * headline "USED OF n GB" / percent figure needs the device's actual capacity though, which this
 * commonMain screen has no way to ask the OS for itself - so it's plumbed in as an optional
 * [StorageStats.totalCapacityBytes] the platform layer fills in (Android does this with
 * `StatFs`); when it's null the header just falls back to the sandbox total alone. Two tabs: the
 * breakdown, and the worst offenders named.
 */

private val CATEGORY_HUES = mapOf(
    "Images" to 0, "Documents" to 2, "Code" to 9, "Archives" to 5, "Other" to 4
)

private enum class StorageTab { BY_TYPE, LARGEST }

@Composable
fun StorageScreen(
    stats: StorageStats?,
    onDelete: (path: String) -> Unit,
    onBack: () -> Unit,
    fullscreen: Boolean,
    onFreeUpSpace: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(StorageTab.BY_TYPE) }

    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip("‹ BACK", onClick = onBack)
            Chip("${stats?.byCategory?.size ?: 0} PLACES", filled = false, clickable = false)
        }

        if (stats == null) {
            Text(
                "COUNTING…", color = Azphalt.Ink.copy(alpha = .4f),
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.1.em,
                modifier = Modifier.padding(start = 20.dp, top = 24.dp)
            )
            return
        }

        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            val capacity = stats.totalCapacityBytes
            if (capacity != null && capacity > 0) {
                Text(
                    "USED OF ${formatFileSize(capacity)}", color = Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
                )
                val percent = ((stats.totalBytes.toDouble() / capacity.toDouble()) * 100).toInt().coerceIn(0, 100)
                Text(
                    "$percent%", color = Azphalt.Ink,
                    fontSize = 44.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black
                )
            } else {
                Text(
                    "USED IN THIS SANDBOX", color = Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em
                )
                Text(
                    formatFileSize(stats.totalBytes), color = Azphalt.Ink,
                    fontSize = 44.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink.copy(alpha = .14f))
            ) {
                val total = stats.totalBytes.coerceAtLeast(1)
                stats.byCategory.filter { it.bytes > 0 }.forEach { cat ->
                    val fraction = (cat.bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .weight(fraction.coerceAtLeast(0.001f))
                            .background(Azphalt.hues[CATEGORY_HUES[cat.label] ?: 3])
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Chip("BY TYPE", filled = tab == StorageTab.BY_TYPE, onClick = { tab = StorageTab.BY_TYPE })
            Chip("LARGEST", filled = tab == StorageTab.LARGEST, onClick = { tab = StorageTab.LARGEST })
            Spacer(Modifier.weight(1f))
            // No bulk-cleanup flow of its own - the sensible existing action is just surfacing
            // the worst offenders, so this jumps to LARGEST (where DELETE is one tap away) and
            // also hands off to whatever the caller wants to do with it.
            Chip(
                "FREE UP SPACE", background = Azphalt.hues[6], foreground = Azphalt.White,
                onClick = { tab = StorageTab.LARGEST; onFreeUpSpace() }
            )
        }

        when (tab) {
            StorageTab.BY_TYPE -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(stats.byCategory.filter { it.bytes > 0 }.sortedByDescending { it.bytes }, key = { it.label }) { cat ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50))
                                    .background(Azphalt.hues[CATEGORY_HUES[cat.label] ?: 3])
                            )
                            Text(cat.label.uppercase(), color = Azphalt.Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.06.em)
                        }
                        Text(formatFileSize(cat.bytes), color = Azphalt.Ink.copy(alpha = .6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .12f)))
                }
            }

            StorageTab.LARGEST -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stats.largest, key = { it.path }) { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.Ink.copy(alpha = .10f))
                            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(f.name, color = Azphalt.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(formatFileSize(f.sizeBytes), color = Azphalt.Ink.copy(alpha = .55f), fontSize = 9.sp)
                        }
                        Text(
                            "×", color = Azphalt.Ink.copy(alpha = .7f), fontSize = 15.sp,
                            modifier = Modifier.clickable { onDelete(f.path) }.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Azphalt.Ink,
    foreground: Color = Azphalt.Yellow,
    filled: Boolean = true,
    clickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    val bg = if (filled) background else Azphalt.Ink.copy(alpha = .14f)
    val fg = if (filled) foreground else Azphalt.Ink.copy(alpha = .55f)
    Box(
        modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
    }
}
