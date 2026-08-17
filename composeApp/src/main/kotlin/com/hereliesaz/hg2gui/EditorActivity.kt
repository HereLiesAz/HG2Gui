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

        // SYS-1: this Activity is exported (so another app's VIEW/EDIT intent can reach it - see
        // the manifest). Because android:exported="true" lets ANY app start it via an *explicit*
        // component Intent - bypassing the VIEW/EDIT intent-filter entirely, action/mimeType/extras
        // and all - an Intent extra was never a safe place to distinguish "this app's own Files
        // screen or edit command opened this" from "some other installed app crafted this Intent."
        // A previous version of this check trusted a "path" String extra and merely blocklisted a
        // few sibling directories (shared_prefs/databases/cache/code_cache), leaving filesDir - the
        // VFS sandbox root and $HOME for the real shell (see DistroManager.homeDir) - reachable to
        // ANY caller, including one that never had storage access of its own. Since this app's SSH
        // feature routinely puts private keys under $HOME/.ssh, that was a real confused-deputy
        // path: a zero-permission app could read (or, via Save, overwrite) this app's SSH keys.
        //
        // The fix: internal callers now hand off the path through [pendingInternalPath], a plain
        // in-process field an external Intent can never populate (there's no Intent, extra, or data
        // Uri for an attacker to spoof it through) - see the launch site in TerminalActivity. Only
        // that trusted handoff is allowed to open a path anywhere in this app's private storage.
        // Anything that actually arrived via an Intent (implicit VIEW/EDIT, or an explicit
        // component Intent from another app) is treated as untrusted regardless of what it claims,
        // and is denied outright if it resolves anywhere inside this app's own private data dir -
        // this app's own private files were never meant to be reachable through the public
        // VIEW/EDIT surface at all.
        val internalPath = pendingInternalPath
        pendingInternalPath = null
        val path = internalPath ?: intent.data?.path
        if (path == null) {
            finish()
            return
        }
        val file = File(path)

        if (internalPath == null) {
            val dataRoot = File(applicationInfo.dataDir ?: filesDir.parentFile?.path.orEmpty())
            if (file.isWithin(dataRoot)) {
                finish()
                return
            }
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
        // Set by a trusted in-process caller (see TerminalActivity's "open in editor"/edit-command
        // launch site) immediately before starting this Activity, then consumed once in onCreate.
        // Deliberately NOT an Intent extra - an exported Activity can be started by any other app
        // via an explicit component Intent, so any Intent extra (however it's named) is something
        // an external caller can also set. A plain static field has no such surface: nothing about
        // starting this Activity from outside the process can populate it.
        @Volatile
        var pendingInternalPath: String? = null
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
