package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.editor.EditorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hosts [EditorScreen]. A real Activity (not just an in-app screen) because it is also the
 * target of the VIEW/EDIT intent-filters in the manifest - another app opening a text file
 * needs a launchable entry point, not app-internal navigation state.
 */
class EditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val path = intent.getStringExtra(PATH) ?: intent.data?.path
        if (path == null) {
            finish()
            return
        }
        val file = File(path)

        // SYS-1: this Activity is exported (so another app's VIEW/EDIT intent can reach it - see
        // the manifest), which means the path above is not necessarily one this app chose. It
        // still executes with this app's own file permissions regardless of who sent the intent,
        // so a caller with zero storage access of its own could otherwise use this Activity as a
        // confused deputy to read (and, via Save, tamper with) this app's private preference
        // files - most sensitively shared_prefs, where the AI API key and MCP pairing token live
        // in plaintext. filesDir itself (VFS's sandbox root, the Termux prefix) stays reachable -
        // both this app's own "edit" command and the Files screen's "open in editor" legitimately
        // point here - only the sibling directories no legitimate caller has any reason to name.
        val dataRoot = File(applicationInfo.dataDir ?: filesDir.parentFile?.path.orEmpty())
        val offLimits = listOf("shared_prefs", "databases", "cache", "code_cache").map { File(dataRoot, it) }
        if (offLimits.any { dir -> file.isWithin(dir) }) {
            finish()
            return
        }

        setContent {
            var content by remember { mutableStateOf<String?>(null) }
            var error by remember { mutableStateOf<String?>(null) }
            var dirty by remember { mutableStateOf(false) }
            // SYS-2: dirty existed only to show/hide the Save pill - neither exit path ever
            // consulted it before tearing the Activity down. Both the BACK pill and the system
            // back gesture now route through this instead of finish() directly.
            var confirmDiscard by remember { mutableStateOf(false) }
            fun requestBack() {
                if (dirty) confirmDiscard = true else finish()
            }
            BackHandler(enabled = dirty) { confirmDiscard = true }
            val scope = rememberCoroutineScope()

            LaunchedEffect(path) {
                val (loaded, loadError) = withContext(Dispatchers.IO) {
                    try {
                        // SYS-3: read/write both defaulted to UTF-8 with no check at all, despite
                        // the manifest wiring this same Activity to .db files by extension - a
                        // binary file opened this way gets its bytes silently replaced with UTF-8
                        // replacement characters, and Save re-encodes that already-mangled text
                        // back to disk, permanently. Sniffing for a NUL byte in the lead bytes is
                        // the same heuristic `file`/`grep -I`/git use to call something binary;
                        // real UTF-8 text never legitimately contains one.
                        if (file.looksBinary()) {
                            null to "This looks like a binary file - can't edit it here."
                        } else {
                            file.readText() to null
                        }
                    } catch (e: OutOfMemoryError) {
                        null to "This file is too big to edit here - try a real editor for it."
                    } catch (e: Exception) {
                        null to (e.message ?: "Could not read this file.")
                    }
                }
                content = loaded
                error = loadError
            }

            HG2GuiTheme {
                EditorScreen(
                    fileName = file.name,
                    content = content,
                    error = error,
                    dirty = dirty,
                    onContentChange = { content = it; dirty = true },
                    onSave = {
                        val current = content ?: return@EditorScreen
                        scope.launch(Dispatchers.IO) {
                            try {
                                file.writeText(current)
                                dirty = false
                            } catch (e: Exception) {
                                error = e.message ?: "Could not save this file."
                            }
                        }
                    },
                    onBack = { requestBack() }
                )

                if (confirmDiscard) {
                    ConfirmDialog(
                        title = "DISCARD CHANGES?",
                        message = "${file.name} has unsaved edits - leaving now throws them away.",
                        confirmLabel = "DISCARD",
                        onConfirm = { finish() },
                        onDismiss = { confirmDiscard = false }
                    )
                }
            }
        }
    }

    companion object {
        const val PATH = "path"
    }
}

/** True iff [this] canonicalizes to somewhere inside [dir]. */
private fun File.isWithin(dir: File): Boolean {
    val target = canonicalFile
    val root = dir.canonicalFile
    return target == root || target.path.startsWith(root.path + File.separator)
}

/** A NUL byte anywhere in the first 8000 bytes marks a file as binary - the same lead-bytes
 *  heuristic `file`/`grep -I`/git use, since real UTF-8 text never legitimately contains one. */
private fun File.looksBinary(): Boolean {
    val probe = ByteArray(8000)
    val read = inputStream().use { it.read(probe) }
    return probe.copyOf(read.coerceAtLeast(0)).any { it == 0.toByte() }
}
