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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.editor.EditorScreen
import android.content.Intent
import android.net.Uri
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
        //
        // The trusted path also survives recreation (rotation, process death): savedInstanceState
        // is written and restored by the system for this Activity instance only - no other app can
        // populate it - so it is as trustworthy as the original in-process handoff.
        val internalPath = pendingInternalPath ?: savedInstanceState?.getString(STATE_INTERNAL_PATH)
        pendingInternalPath = null
        restoredInternalPath = internalPath

        // For content:// URIs, Uri.getPath() returns the encoded URI path, not a filesystem path.
        // Only file:// URIs resolve to a real filesystem path via getPath(). Content URIs are read
        // via contentResolver.openInputStream — we keep the Uri and don't convert to a File path.
        val externalUri: Uri? = if (internalPath == null) intent.data else null
        val isContentUri = externalUri?.scheme.equals("content", ignoreCase = true)
        val path: String? = when {
            internalPath != null -> internalPath
            isContentUri -> null  // resolved below via contentResolver
            // Only a real file:// URI names a filesystem path - any other scheme's getPath()
            // (http, a custom scheme) is not something to open as a local file.
            externalUri?.scheme.equals("file", ignoreCase = true) -> externalUri?.path
            else -> null
        }

        if (path == null && !isContentUri) {
            finish()
            return
        }
        val file = path?.let { File(it) }

        if (internalPath == null && file != null) {
            val dataRoot = File(applicationInfo.dataDir ?: filesDir.parentFile?.path.orEmpty())
            if (file.isWithin(dataRoot)) {
                finish()
                return
            }
        }

        val displayName = file?.name ?: externalUri?.lastPathSegment ?: "file"
        // A file:// path from another app carries no grant of its own - this app (which may hold
        // all-files storage access) would be reading/writing it on that app's behalf, a confused
        // deputy. So an externally supplied file:// path only opens after the user explicitly
        // confirms it here; content:// URIs instead carry the sender's own scoped grant.
        val needsFileConsent = internalPath == null && file != null

        setContent {
            // Unsaved edits survive recreation. A very large buffer is not saved (it would overflow
            // the saved-state Binder transaction); it is simply reloaded from disk, and dirty is
            // dropped with it so the reload is never mistaken for unsaved work.
            var editState by rememberSaveable(stateSaver = EditStateSaver) { mutableStateOf(EditState(null, false)) }
            val content = editState.content
            val dirty = editState.dirty
            var fileConsent by rememberSaveable { mutableStateOf(!needsFileConsent) }
            var error by remember { mutableStateOf<String?>(null) }
            // SYS-2: dirty existed only to show/hide the Save pill - neither exit path ever
            // consulted it before tearing the Activity down. Both the BACK pill and the system
            // back gesture now route through this instead of finish() directly.
            var confirmDiscard by remember { mutableStateOf(false) }
            fun requestBack() {
                if (dirty) confirmDiscard = true else finish()
            }
            BackHandler(enabled = dirty) { confirmDiscard = true }
            val scope = rememberCoroutineScope()
            val resolver = contentResolver

            LaunchedEffect(path, externalUri, fileConsent) {
                if (!fileConsent) return@LaunchedEffect
                // Restored from saved state - don't clobber the user's (possibly unsaved) text.
                if (editState.content != null) return@LaunchedEffect
                val (loaded, loadError) = withContext(Dispatchers.IO) {
                    try {
                        val bytes: ByteArray = when {
                            file != null -> file.readBytes()
                            externalUri != null -> resolver.openInputStream(externalUri)?.use { it.readBytes() }
                                ?: error("Cannot open content URI")
                            else -> error("No file or URI")
                        }
                        // SYS-3: read/write both defaulted to UTF-8 with no check at all, despite
                        // the manifest wiring this same Activity to .db files by extension - a
                        // binary file opened this way gets its bytes silently replaced with UTF-8
                        // replacement characters, and Save re-encodes that already-mangled text
                        // back to disk, permanently. Sniffing for a NUL byte in the lead bytes is
                        // the same heuristic `file`/`grep -I`/git use to call something binary;
                        // real UTF-8 text never legitimately contains one.
                        if (bytes.take(8000).any { it == 0.toByte() }) {
                            null to "This looks like a binary file - can't edit it here."
                        } else {
                            bytes.toString(Charsets.UTF_8) to null
                        }
                    } catch (e: OutOfMemoryError) {
                        null to "This file is too big to edit here - try a real editor for it."
                    } catch (e: Exception) {
                        null to (e.message ?: "Could not read this file.")
                    }
                }
                editState = EditState(loaded, false)
                error = loadError
            }

            HG2GuiTheme {
                EditorScreen(
                    fileName = displayName,
                    content = content,
                    error = error,
                    dirty = dirty,
                    onContentChange = { editState = EditState(it, true) },
                    onDismissError = { error = null },
                    onSave = {
                        val current = content ?: return@EditorScreen
                        scope.launch(Dispatchers.IO) {
                            try {
                                when {
                                    file != null && fileConsent -> file.writeText(current)
                                    externalUri != null -> {
                                        val writable = (intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0
                                        if (!writable) {
                                            withContext(Dispatchers.Main) { error = "Cannot save back to external URI" }
                                            return@launch
                                        }
                                        resolver.openOutputStream(externalUri, "wt")
                                            ?.use { it.bufferedWriter(Charsets.UTF_8).use { w -> w.write(current) } }
                                            ?: run {
                                                withContext(Dispatchers.Main) { error = "Cannot open URI for writing" }
                                                return@launch
                                            }
                                    }
                                    else -> {
                                        withContext(Dispatchers.Main) { error = "Cannot save back to external URI" }
                                        return@launch
                                    }
                                }
                                // State writes must happen on the main thread to satisfy Compose's
                                // snapshot isolation; @Volatile alone is not sufficient here. Edits
                                // typed while the write was in flight are not on disk yet, so the
                                // buffer is only clean if it still matches what was written.
                                withContext(Dispatchers.Main) {
                                    error = null
                                    if (editState.content == current) editState = editState.copy(dirty = false)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) { error = e.message ?: "Could not save this file." }
                            }
                        }
                    },
                    onBack = { requestBack() }
                )

                if (!fileConsent) {
                    ConfirmDialog(
                        title = "OPEN FILE FROM ANOTHER APP?",
                        message = "Another app asked HG2Gui to open ${file?.absolutePath}. " +
                            "HG2Gui will read it and can save over it with its own storage access.",
                        confirmLabel = "OPEN",
                        onConfirm = { fileConsent = true },
                        onDismiss = { finish() }
                    )
                }

                if (confirmDiscard) {
                    ConfirmDialog(
                        title = "DISCARD CHANGES?",
                        message = "$displayName has unsaved edits - leaving now throws them away.",
                        confirmLabel = "DISCARD",
                        onConfirm = { finish() },
                        onDismiss = { confirmDiscard = false }
                    )
                }
            }
        }
    }

    // The trusted path this instance opened, kept so onSaveInstanceState can carry it across
    // recreation (pendingInternalPath is consumed once and is null by then).
    private var restoredInternalPath: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        restoredInternalPath?.let { outState.putString(STATE_INTERNAL_PATH, it) }
    }

    companion object {
        private const val STATE_INTERNAL_PATH = "hg2.editor.internalPath"

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

/** Editor buffer plus its unsaved flag, saved together so they can never disagree after restore. */
private data class EditState(val content: String?, val dirty: Boolean)

/** Buffers above this many chars are not saved - the saved-state bundle has a ~1MB Binder limit. */
private const val MAX_SAVED_CHARS = 100_000

private val EditStateSaver = androidx.compose.runtime.saveable.Saver<EditState, ArrayList<Any?>>(
    save = { st ->
        if (st.content != null && st.content.length <= MAX_SAVED_CHARS) arrayListOf(st.content, st.dirty)
        else arrayListOf(null, false)
    },
    restore = { EditState(it[0] as String?, it[1] as Boolean) }
)
