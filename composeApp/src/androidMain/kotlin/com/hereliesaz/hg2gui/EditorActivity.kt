package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

        setContent {
            var content by remember { mutableStateOf<String?>(null) }
            var error by remember { mutableStateOf<String?>(null) }
            var dirty by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(path) {
                val (loaded, loadError) = withContext(Dispatchers.IO) {
                    try {
                        file.readText() to null
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
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val PATH = "path"
    }
}
