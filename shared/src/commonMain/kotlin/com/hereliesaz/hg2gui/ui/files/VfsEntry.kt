package com.hereliesaz.hg2gui.ui.files

import com.hereliesaz.hg2gui.managers.StyledSpan

/**
 * One node in the sandboxed filesystem, identified by an absolute path from the vfs root
 * (e.g. "/Downloads/photo.png") - not a `java.io.File`, so this stays platform-agnostic and the
 * actual I/O lives entirely on the Android side, reached only through the callbacks
 * [FilesScreen] is handed.
 */
data class VfsEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedAt: Long = 0L,
    val isImage: Boolean = false
)

/** [entry]'s own containing directory, so a search result can show where it lives. */
data class VfsSearchResult(val entry: VfsEntry, val parentPath: String)

data class StorageCategoryStat(val label: String, val bytes: Long)

/**
 * A trashed item, as this commonMain screen needs it - a copy of managers/VfsManager.kt's own
 * (androidMain-only) TrashEntry, the same "shared UI, platform-only I/O" boundary [VfsEntry]
 * itself already draws. [id] is opaque here; restoring/purging still happens by handing the
 * whole entry back to the platform callback, not by this screen re-deriving anything from it.
 */
data class VfsTrashEntry(
    val id: String,
    val originalPath: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val deletedAtMillis: Long
)

/** [totalCapacityBytes] and [usedCapacityBytes] are the real device/partition capacity and usage,
 *  when the platform can supply them (e.g. via `StatFs` on Android) - null when it can't, since
 *  commonMain has no cross-platform way to ask the OS how big the disk is or how full it is. The
 *  header falls back to just the sandbox total when they're absent instead of pretending to know a
 *  percentage it doesn't have. [usedCapacityBytes] is the partition's own used bytes (capacity
 *  minus available) - deliberately NOT [totalBytes], which is only the sandbox's own contents and
 *  would make "used of n GB" read as a near-zero percentage against the whole partition. */
data class StorageStats(
    val totalBytes: Long,
    val byCategory: List<StorageCategoryStat>,
    val largest: List<VfsEntry>,
    val totalCapacityBytes: Long? = null,
    val usedCapacityBytes: Long? = null
)

/** The parent path of [path] ("/a/b/c" -> "/a/b"; "/a" -> "/"). */
fun vfsParentPath(path: String): String {
    val trimmed = path.trimEnd('/')
    val slash = trimmed.lastIndexOf('/')
    return if (slash <= 0) "/" else trimmed.substring(0, slash)
}

/** Joins a directory path and a child name into a normalized absolute path. */
fun vfsChildPath(parent: String, name: String): String {
    val base = parent.trimEnd('/')
    return if (base.isEmpty()) "/$name" else "$base/$name"
}

/** F3: a tapped file's own in-place preview - [text] is null for anything not readable as text
 *  (a binary, or a read that failed), in which case the preview surface says what the file is
 *  instead of appearing inert rather than attempting to render raw bytes. [styled] is non-empty
 *  only when a highlighter was actually found installed and ran cleanly; empty means "render
 *  [text] plain" - a pluggable highlighter that reports what's present, never assumed. [isMarkdown]
 *  routes [text] through the markdown renderer instead of either plain or [styled] rendering. */
data class FilePreview(
    val text: String?,
    val styled: List<List<StyledSpan>> = emptyList(),
    val isMarkdown: Boolean = false,
    val truncated: Boolean = false
)

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> {
        val tenths = bytes / (1024L * 1024L * 1024L / 10L)
        "${tenths / 10}.${tenths % 10} GB"
    }
}
