package com.hereliesaz.hg2gui.ui.menu

import java.io.File

/**
 * A "browse for a file" trigger pill that, once opened, lists real files and directories from
 * [root] down through the same pill stack everything else lives in - there is no separate file
 * picker screen. Every directory is a non-terminal node with its own lazily-resolved children
 * (nothing is scanned until the user actually drills into it); every file is a terminal leaf
 * whose token value is its absolute path, so picking one both completes and auto-runs the
 * command it's an argument to.
 */
object FileBrowser {

    // A busybox-style bin/ or a deep media directory can hold far more entries than the fan-out
    // animation is built for; this caps what's listed per directory rather than silently
    // hanging or rendering something unusable.
    private const val MAX_ENTRIES = 300

    fun pickerNode(id: String, root: File): MenuNode = MenuNode(
        id = id,
        label = "file…",
        cap = "PICK",
        emitsToken = false,
        resolveChildren = { listing(root) }
    )

    private fun listing(dir: File): List<MenuNode> {
        val entries = (dir.listFiles() ?: emptyArray())
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        val nodes = entries.take(MAX_ENTRIES).map { f ->
            if (f.isDirectory) {
                MenuNode(
                    id = "fs:${f.absolutePath}",
                    label = f.name,
                    cap = "DIR",
                    emitsToken = false,
                    resolveChildren = { listing(f) }
                )
            } else {
                MenuNode(
                    id = "fs:${f.absolutePath}",
                    label = f.name,
                    cap = "FILE",
                    value = f.absolutePath
                )
            }
        }

        return if (entries.size > nodes.size) {
            nodes + MenuNode(
                id = "fs:${dir.absolutePath}:more",
                label = "+${entries.size - nodes.size} more",
                cap = "…",
                emitsToken = false
            )
        } else {
            nodes
        }
    }
}
