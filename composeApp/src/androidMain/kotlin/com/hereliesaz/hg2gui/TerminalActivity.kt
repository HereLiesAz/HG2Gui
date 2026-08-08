package com.hereliesaz.hg2gui

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.terminal.Builtins
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.util.Utils
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.SessionUiState
import com.hereliesaz.hg2gui.ui.SettingsScreen
import com.hereliesaz.hg2gui.ui.TerminalScreen
import com.hereliesaz.hg2gui.ui.files.FilesScreen
import com.hereliesaz.hg2gui.ui.files.VfsEntry
import com.hereliesaz.hg2gui.ui.guide.CommandGuideScreen
import com.hereliesaz.hg2gui.ui.menu.CommandTree
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A live shell paired with the UI state (scrollback, history, cwd) that only it owns. */
private class TerminalSession(val ui: SessionUiState, val engine: TerminalEngine)

private const val PREFS_NAME = "hg2gui_prefs"
private const val PREF_FULLSCREEN = "fullscreen"
private const val PREF_FONT_SCALE_PERCENT = "font_scale_percent"

class TerminalActivity : ComponentActivity() {

    private var sessions by mutableStateOf(listOf<TerminalSession>())
    private var nextSessionNumber = 2

    private enum class Screen { Terminal, Settings, Guide, Files }

    private data class InitResult(
        val engine: TerminalEngine,
        val tree: List<MenuNode>,
        val fullscreen: Boolean,
        val fontScalePercent: Int
    )

    private val prefs: SharedPreferences by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Utils.init(applicationContext)

        setContent {
            var tree by remember { mutableStateOf<List<MenuNode>?>(null) }
            var activeSessionId by remember { mutableStateOf("") }
            var screen by remember { mutableStateOf(Screen.Terminal) }
            var fullscreen by remember { mutableStateOf(false) }
            var fontScalePercent by remember { mutableStateOf(100) }
            var filesPath by remember { mutableStateOf("/") }
            var filesEntries by remember { mutableStateOf(listOf<VfsEntry>()) }
            val scope = rememberCoroutineScope()

            fun refreshFiles() {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        VfsManager.currentPath() to VfsManager.list(this@TerminalActivity).map { f ->
                            VfsEntry(f.name, f.isDirectory, if (f.isFile) f.length() else 0L)
                        }
                    }
                    filesPath = result.first
                    filesEntries = result.second
                }
            }

            LaunchedEffect(Unit) {
                val built = withContext(Dispatchers.Default) {
                    val builtEngine = TerminalEngine(this@TerminalActivity)
                    val builtTree = CommandTree.from(this@TerminalActivity)
                    InitResult(
                        engine = builtEngine,
                        tree = builtTree,
                        fullscreen = prefs.getBoolean(PREF_FULLSCREEN, false),
                        fontScalePercent = prefs.getInt(PREF_FONT_SCALE_PERCENT, 100)
                    )
                }

                val firstUi = SessionUiState(id = "1", name = "main", cwd = built.engine.workingDirectory)
                sessions = listOf(TerminalSession(firstUi, built.engine))
                activeSessionId = firstUi.id
                tree = built.tree
                fullscreen = built.fullscreen
                fontScalePercent = built.fontScalePercent

                // Real Termux never shows an empty shell either - it installs its own bootstrap
                // automatically, once, before the first prompt appears. Match that instead of
                // leaving Shell showing only the one pill that fixes it, waiting to be found.
                if (!DistroManager.isInstalled(this@TerminalActivity)) {
                    firstUi.running = true
                    val entryId = firstUi.buffer.size
                    firstUi.buffer = firstUi.buffer + TerminalHistoryEntry(command = "bootstrap", isRunning = true)
                    built.engine.run("bootstrap") { "" }.collect { output ->
                        firstUi.buffer = firstUi.buffer.mapIndexed { i, e ->
                            if (i == entryId) e.copy(output = output) else e
                        }
                    }
                    firstUi.buffer = firstUi.buffer.mapIndexed { i, e ->
                        if (i == entryId) e.copy(isRunning = false) else e
                    }
                    firstUi.running = false
                    tree = withContext(Dispatchers.IO) { CommandTree.from(this@TerminalActivity) }
                }
            }

            LaunchedEffect(fullscreen) {
                applyFullscreen(fullscreen)
            }

            HG2GuiTheme(scale = fontScalePercent / 100f) {
                val currentTree = tree
                when {
                    screen == Screen.Settings -> SettingsScreen(
                        fullscreen = fullscreen,
                        onFullscreenChange = { value ->
                            fullscreen = value
                            prefs.edit { putBoolean(PREF_FULLSCREEN, value) }
                        },
                        fontScalePercent = fontScalePercent,
                        onFontScalePercentChange = { value ->
                            fontScalePercent = value
                            prefs.edit { putInt(PREF_FONT_SCALE_PERCENT, value) }
                        },
                        onBack = { screen = Screen.Terminal }
                    )

                    screen == Screen.Guide -> CommandGuideScreen(
                        tree = currentTree.orEmpty(),
                        fullscreen = fullscreen,
                        onCommandSelected = { tokens ->
                            sessions.firstOrNull { it.ui.id == activeSessionId }?.let { session ->
                                session.ui.tokens = tokens
                                session.ui.inputText = ""
                            }
                        },
                        onBack = { screen = Screen.Terminal }
                    )

                    screen == Screen.Files -> FilesScreen(
                        path = filesPath,
                        entries = filesEntries,
                        onNavigate = { target ->
                            scope.launch {
                                withContext(Dispatchers.IO) { VfsManager.cd(this@TerminalActivity, target) }
                                refreshFiles()
                            }
                        },
                        onOpenFile = { name ->
                            scope.launch {
                                val file = withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, name)
                                }
                                if (file != null) {
                                    val intent = Intent(this@TerminalActivity, EditorActivity::class.java)
                                    intent.putExtra(EditorActivity.PATH, file.absolutePath)
                                    startActivity(intent)
                                }
                            }
                        },
                        onCreateFolder = { name ->
                            scope.launch {
                                withContext(Dispatchers.IO) { VfsManager.mkdir(this@TerminalActivity, name) }
                                refreshFiles()
                            }
                        },
                        onCreateFile = { name ->
                            scope.launch {
                                withContext(Dispatchers.IO) { VfsManager.touch(this@TerminalActivity, name) }
                                refreshFiles()
                            }
                        },
                        onDelete = { name ->
                            scope.launch {
                                withContext(Dispatchers.IO) { VfsManager.delete(this@TerminalActivity, name) }
                                refreshFiles()
                            }
                        },
                        onBack = { screen = Screen.Terminal },
                        modifier = Modifier.then(
                            if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars)
                        )
                    )

                    sessions.isNotEmpty() && currentTree != null -> TerminalScreen(
                        tree = currentTree,
                        sessions = sessions.map { it.ui },
                        activeSessionId = activeSessionId,
                        onSessionPick = { activeSessionId = it },
                        onNewSession = {
                            scope.launch {
                                val newEngine = withContext(Dispatchers.Default) {
                                    TerminalEngine(this@TerminalActivity)
                                }
                                val id = (nextSessionNumber++).toString()
                                val newUi = SessionUiState(
                                    id = id, name = "session $id", cwd = newEngine.workingDirectory
                                )
                                sessions = sessions + TerminalSession(newUi, newEngine)
                                activeSessionId = id
                            }
                        },
                        onCloseSession = { id ->
                            if (sessions.size > 1) {
                                val closing = sessions.firstOrNull { it.ui.id == id }
                                if (closing != null) {
                                    val remaining = sessions.filterNot { it.ui.id == id }
                                    sessions = remaining
                                    if (activeSessionId == id) {
                                        activeSessionId = remaining.first().ui.id
                                    }
                                    closing.engine.destroy()
                                }
                            }
                        },
                        fullscreen = fullscreen,
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenGuide = { screen = Screen.Guide },
                        onOpenFiles = {
                            refreshFiles()
                            screen = Screen.Files
                        },
                        onRun = { sessionId, line, onOutput, onNeedInput ->
                            val session = sessions.first { it.ui.id == sessionId }
                            session.engine.run(line, onNeedInput).collect { output -> onOutput(output) }
                            session.ui.cwd = session.engine.workingDirectory
                            // A package manager (pkg/apt/pip/npm) can change what's actually on
                            // PATH; re-scan so the Shell pills reflect that instead of the
                            // snapshot from whenever the tree was last built.
                            tree = withContext(Dispatchers.IO) { CommandTree.from(this@TerminalActivity) }
                        }
                    )

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…")
                    }
                }
            }
        }
    }

    private fun applyFullscreen(fullscreen: Boolean) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == PermissionCodes.COMMAND_SUGGESTION_REQUEST_PERMISSION && granted) {
            // ContactManager's own refresh is what's listening for this - it's the one builtin
            // whose permission grant needs to kick off work with no further user input.
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ContactManager.ACTION_REFRESH))
        }
        // Every other builtin's permission request (COMMAND_REQUEST_PERMISSION) is answered by
        // just running the same pill or command again - the system permission dialog itself is
        // the only feedback needed in the meantime.
    }

    override fun onDestroy() {
        sessions.forEach { it.engine.destroy() }
        Builtins.destroy(this)
        super.onDestroy()
    }
}
