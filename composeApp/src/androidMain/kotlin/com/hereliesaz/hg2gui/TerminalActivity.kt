package com.hereliesaz.hg2gui

import android.content.Intent
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hereliesaz.hg2gui.commands.BaseCommandGroup
import com.hereliesaz.hg2gui.commands.tuixt.TuixtActivity
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable
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

class TerminalActivity : ComponentActivity(), Reloadable {

    private var main: MainManager? = null
    private var sessions by mutableStateOf(listOf<TerminalSession>())
    private var nextSessionNumber = 2

    private enum class Screen { Terminal, Settings, Guide, Files }

    private data class InitResult(
        val main: MainManager,
        val engine: TerminalEngine,
        val tree: List<MenuNode>,
        val fullscreen: Boolean,
        val fontScalePercent: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Tuils.init(applicationContext)

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
                    XMLPrefsManager.loadCommons(this@TerminalActivity)

                    val builtMain = MainManager(this@TerminalActivity, this@TerminalActivity)
                    val builtEngine = TerminalEngine(
                        this@TerminalActivity, builtMain, builtMain.mainPack?.currentDirectory
                    )
                    val builtTree = CommandTree.from(
                        builtMain.mainPack?.commandGroup?.commands?.toList().orEmpty()
                    )
                    InitResult(
                        main = builtMain,
                        engine = builtEngine,
                        tree = builtTree,
                        fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen),
                        fontScalePercent = XMLPrefsManager.getInt(Ui.font_scale_percent)
                    )
                }

                main = built.main
                val firstUi = SessionUiState(id = "1", name = "main", cwd = built.engine.workingDirectory)
                sessions = listOf(TerminalSession(firstUi, built.engine))
                activeSessionId = firstUi.id
                tree = built.tree
                fullscreen = built.fullscreen
                fontScalePercent = built.fontScalePercent
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
                            scope.launch(Dispatchers.IO) {
                                Ui.fullscreen.parent().write(Ui.fullscreen, value.toString())
                            }
                        },
                        fontScalePercent = fontScalePercent,
                        onFontScalePercentChange = { value ->
                            fontScalePercent = value
                            scope.launch(Dispatchers.IO) {
                                Ui.font_scale_percent.parent().write(Ui.font_scale_percent, value.toString())
                            }
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
                                    val intent = Intent(this@TerminalActivity, TuixtActivity::class.java)
                                    intent.putExtra(TuixtActivity.PATH, file.absolutePath)
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
                            val m = main
                            if (m != null) {
                                scope.launch {
                                    val newEngine = withContext(Dispatchers.Default) {
                                        TerminalEngine(this@TerminalActivity, m, m.mainPack?.currentDirectory)
                                    }
                                    val id = (nextSessionNumber++).toString()
                                    val newUi = SessionUiState(
                                        id = id, name = "session $id", cwd = newEngine.workingDirectory
                                    )
                                    sessions = sessions + TerminalSession(newUi, newEngine)
                                    activeSessionId = id
                                }
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
                        onRun = { sessionId, line, onOutput ->
                            val session = sessions.first { it.ui.id == sessionId }
                            session.engine.run(line).collect { output -> onOutput(output) }
                            session.ui.cwd = session.engine.workingDirectory
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

    override fun reload() {
        finish()
        startActivity(intent)
    }

    override fun addMessage(header: String, message: String) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val lastCmd = main?.mainPack?.lastCommand
                if (!lastCmd.isNullOrBlank()) {
                    main?.onCommand(lastCmd, null as String?, false)
                }
            } else {
                main?.sendPermissionNotGrantedWarning()
            }
        }
    }

    override fun onDestroy() {
        sessions.forEach { it.engine.destroy() }
        main?.destroy()
        super.onDestroy()
    }
}
