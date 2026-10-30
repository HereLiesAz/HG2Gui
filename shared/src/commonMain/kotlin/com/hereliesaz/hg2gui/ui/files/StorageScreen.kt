package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.maxContentWidth
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface

/*
 * Storage broken down by content type - real numbers from a recursive walk of the sandbox. The
 * headline "USED OF n GB" / percent figure needs the device's actual capacity though, which this
 * commonMain screen has no way to ask the OS for itself - so it's plumbed in as an optional
 * [StorageStats.totalCapacityBytes] the platform layer fills in (Android does this with
 * `StatFs`); when it's null the header just falls back to the sandbox total alone. Three tabs: the
 * breakdown, the worst offenders named, and the trash (with how long each item has left).
 */

// docs/HG2Gui File Manager.html's own mock predates this taxonomy - it illustrates a 4-category
// example (Source/Photos/Documents/Other) from before Code and Archives existed as their own
// categories (VfsManager.kt's real StorageCategory has 5). Kept as an illustrative mock rather
// than updated to match, but noted here so the two not lining up reads as known drift, not an
// oversight - in particular "Other" no longer uses the mock's own hue-orange (now Archives'),
// so don't assume this map's colors trace back to that doc's legend one-for-one.
private val CATEGORY_HUES = mapOf(
    "Images" to 0, "Documents" to 2, "Code" to 9, "Archives" to 5, "Other" to 4
)

private const val HUE_RESTORE = 3
private const val HUE_DESTRUCTIVE = 6

private enum class StorageTab { BY_TYPE, LARGEST, TRASH }

@Composable
fun StorageScreen(
    stats: StorageStats?,
    onDelete: (path: String) -> Unit,
    // TRASH-1: onDelete above moves something in; this is the trash's own lifecycle - bundled
    // (see TrashPanelState) rather than four more parameters here.
    trash: TrashPanelState = TrashPanelState(),
    onBack: () -> Unit,
    fullscreen: Boolean,
    // Jumps to the LARGEST tab (see "SEE LARGEST FILES" below) and then calls this, so the host can
    // refresh whatever that list is built from first. The jump itself needs no host.
    onFreeUpSpace: () -> Unit = {},
    // A failed delete used to have nowhere to show up while this screen was the one on screen -
    // FilesScreen's own opError banner lives in a Column this screen's caller returns out of
    // reaching, so the failure only surfaced later, disconnected from the tap, if the user
    // happened to navigate back to Browse first. This screen now owns its own copy of that banner
    // instead of relying on a parent Column it may never actually be composed inside of.
    errorMessage: String? = null,
    onErrorDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    // The current time, for the trash rows' "deleted N days ago - purged in M days". 0 (the
    // default) means "unknown": the rows then state the plain 30-day rule instead.
    nowMillis: Long = 0L
) {
    var tab by remember { mutableStateOf(StorageTab.BY_TYPE) }
    var deleteTarget by remember { mutableStateOf<VfsEntry?>(null) }
    var purgeTarget by remember { mutableStateOf<VfsTrashEntry?>(null) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)
    val danger = statusColor(StatusKind.Error)

    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen = fullscreen, ime = false)
            .maxContentWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("‹ BACK", onClick = onBack, contentDescription = "Back to files")
            AzphaltHeading("STORAGE")
        }

        errorMessage?.let { message ->
            ErrorBanner(message, onErrorDismiss, Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
        }

        if (stats == null) {
            StatusLabel("COUNTING…", Modifier.padding(start = 20.dp, top = 24.dp).liveRegionPolite())
            return
        }

        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
            val capacity = stats.totalCapacityBytes
            val usedOfCapacity = stats.usedCapacityBytes
            if (capacity != null && capacity > 0 && usedOfCapacity != null) {
                Text("USED OF ${formatFileSize(capacity)}", color = muted, style = labelTextStyle())
                // The partition's own used bytes against its own capacity - stats.totalBytes is
                // only the sandbox's own contents, dividing that by the whole partition's capacity
                // would read as a near-zero percent regardless of how full the device actually is.
                val percent = ((usedOfCapacity.toDouble() / capacity.toDouble()) * 100).toInt().coerceIn(0, 100)
                Text(
                    "$percent%", color = onPage,
                    fontSize = 44.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black
                )
            } else {
                Text("USED IN THIS SANDBOX", color = muted, style = labelTextStyle())
                Text(
                    formatFileSize(stats.totalBytes), color = onPage,
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

        // Horizontally scrollable rather than a fixed Row with a weighted spacer - at larger UI
        // scale/system font sizes or on a narrow device, the tabs plus padding and gaps can exceed
        // the available width; the spacer used to just collapse to zero and push the last chip
        // partly or wholly off-screen instead of making room for it.
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AzphaltChip(
                "BY TYPE", onClick = { tab = StorageTab.BY_TYPE },
                selected = tab == StorageTab.BY_TYPE, role = Role.Tab
            )
            AzphaltChip(
                "LARGEST", onClick = { tab = StorageTab.LARGEST },
                selected = tab == StorageTab.LARGEST, role = Role.Tab
            )
            AzphaltChip(
                if (trash.items.isEmpty()) "TRASH" else "TRASH (${trash.items.size})",
                onClick = { tab = StorageTab.TRASH },
                selected = tab == StorageTab.TRASH, role = Role.Tab,
                contentDescription = if (trash.items.isEmpty()) "Trash, empty" else "Trash, ${trash.items.size} items"
            )
            if (tab == StorageTab.TRASH && trash.items.isNotEmpty()) {
                AzphaltChip(
                    "EMPTY TRASH", onClick = { confirmEmptyTrash = true },
                    tint = Azphalt.hues[HUE_DESTRUCTIVE],
                    contentDescription = "Empty the trash, deleting everything in it for good"
                )
            }
        }

        when (tab) {
            StorageTab.BY_TYPE -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(stats.byCategory.filter { it.bytes > 0 }.sortedByDescending { it.bytes }, key = { it.label }) { cat ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier.size(8.dp).clip(RoundedCornerShape(percent = 50))
                                    .background(Azphalt.hues[CATEGORY_HUES[cat.label] ?: 3])
                            )
                            Text(
                                cat.label.uppercase(),
                                color = onPage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.06.em,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(formatFileSize(cat.bytes), color = muted, style = captionTextStyle())
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .12f)))
                }
                // m-4: this used to be a red "FREE UP SPACE" button that, on the LARGEST tab,
                // did nothing at all. It is now an honestly named way into that list, where each
                // row has its own TRASH action - the one real "free up space" flow this screen has.
                if (stats.largest.isNotEmpty()) {
                    item(key = "__largest_link__") {
                        Box(Modifier.padding(top = 12.dp)) {
                            AzphaltChip(
                                "SEE LARGEST FILES ›",
                                onClick = { tab = StorageTab.LARGEST; onFreeUpSpace() },
                                contentDescription = "See the largest files, where each can be moved to the trash"
                            )
                        }
                    }
                }
            }

            StorageTab.LARGEST -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "__largest_note__") {
                    Text(
                        if (stats.largest.isEmpty()) "NO FILES YET" else
                            "TRASH moves a file to the trash, where it can be restored. ${trashRetentionNote()}",
                        color = muted, style = captionTextStyle()
                    )
                }
                items(stats.largest, key = { it.path }) { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(AzphaltSurface.capsule)
                            .background(Azphalt.Ink.copy(alpha = .10f))
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // M-6: the name takes whatever the fixed-width action leaves and ellipsizes;
                        // before, it took everything and squeezed the action to nothing.
                        Column(Modifier.weight(1f)) {
                            Text(
                                f.name, color = onPage, style = nameTextStyle(),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${formatFileSize(f.sizeBytes)} · ${vfsParentPath(f.path)}",
                                color = muted, style = captionTextStyle(),
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        RowAction("TRASH", "Move ${f.name} to the trash", danger, { deleteTarget = f })
                    }
                }
            }

            // TRASH-1: each row offers both directions - RESTORE puts it back where it came
            // from, DELETE FOREVER purges that one item for good (its own ConfirmDialog below,
            // since unlike the soft delete above this one really is final). M-13: every row says
            // how long it has left, because the trash empties itself.
            StorageTab.TRASH -> if (trash.items.isEmpty()) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                    StatusLabel("NOTHING IN THE TRASH")
                    Spacer(Modifier.height(6.dp))
                    Text(trashRetentionNote(), color = muted, style = captionTextStyle())
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "__trash_note__") {
                        Text(trashRetentionNote(), color = muted, style = captionTextStyle())
                    }
                    items(trash.items, key = { it.id }) { entry ->
                        // A card, not a pill: name and age on their own lines, the actions on a
                        // third, so a 60-character name can never push RESTORE out of the row.
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(AzphaltSurface.note)
                                .background(Azphalt.Ink.copy(alpha = .10f))
                                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 2.dp)
                        ) {
                            Text(
                                if (entry.isDirectory) entry.name + "/" else entry.name,
                                color = onPage, style = nameTextStyle(),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                formatFileSize(entry.sizeBytes) + " · " +
                                    trashAgeDescription(entry.deletedAtMillis, nowMillis),
                                color = muted, style = captionTextStyle(),
                                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AzphaltChip(
                                    "RESTORE", onClick = { trash.onRestore(entry) },
                                    tint = Azphalt.hues[HUE_RESTORE],
                                    contentDescription = "Restore ${entry.name}"
                                )
                                RowAction(
                                    "DELETE FOREVER", "Delete ${entry.name} forever", danger,
                                    { purgeTarget = entry }
                                )
                            }
                        }
                    }
                }
            }
        }

        deleteTarget?.let { entry ->
            ConfirmDialog(
                title = "DELETE ${entry.name}?",
                // TRASH-1: no longer final - moved into the TRASH tab above, recoverable there
                // until the retention period (M-13) runs out.
                message = trashConfirmMessage(entry.name, entry.isDirectory),
                confirmLabel = "DELETE",
                onConfirm = { onDelete(entry.path); deleteTarget = null },
                onDismiss = { deleteTarget = null }
            )
        }

        purgeTarget?.let { entry ->
            ConfirmDialog(
                title = "DELETE ${entry.name} FOR GOOD?",
                message = if (entry.isDirectory) {
                    "This deletes ${entry.name} and everything inside it. This can't be undone."
                } else {
                    "This deletes ${entry.name}. This can't be undone."
                },
                confirmLabel = "DELETE",
                onConfirm = { trash.onPurgeTrash(entry); purgeTarget = null },
                onDismiss = { purgeTarget = null }
            )
        }

        if (confirmEmptyTrash) {
            ConfirmDialog(
                title = "EMPTY TRASH?",
                message = "This deletes everything in the trash for good. This can't be undone.",
                confirmLabel = "EMPTY TRASH",
                onConfirm = { trash.onEmptyTrash(); confirmEmptyTrash = false },
                onDismiss = { confirmEmptyTrash = false }
            )
        }
    }
}
