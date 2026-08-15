package com.hereliesaz.hg2gui.ui.files

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

/** [totalCapacityBytes] is the real device/partition capacity, when the platform can supply one
 *  (e.g. via `StatFs` on Android) - null when it can't, since commonMain has no cross-platform way
 *  to ask the OS how big the disk is. The header falls back to just the sandbox total when it's
 *  absent instead of pretending to know a percentage it doesn't have. */
data class StorageStats(
    val totalBytes: Long,
    val byCategory: List<StorageCategoryStat>,
    val largest: List<VfsEntry>,
    val totalCapacityBytes: Long? = null
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

fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> {
        val tenths = bytes / (1024L * 1024L * 1024L / 10L)
        "${tenths / 10}.${tenths % 10} GB"
    }
}
