package com.hereliesaz.hg2gui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.SettingsScreen
import com.hereliesaz.hg2gui.ui.TerminalScreen
import com.hereliesaz.hg2gui.ui.menu.CommandTree
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerminalActivity : ComponentActivity(), Reloadable {

    private var main: MainManager? = null
    private var engine: TerminalEngine? = null

    private enum class Screen { Terminal, Settings }

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
            var cwd by remember { mutableStateOf("") }
            var screen by remember { mutableStateOf(Screen.Terminal) }
            var fullscreen by remember { mutableStateOf(false) }
            var fontScalePercent by remember { mutableStateOf(100) }
            val scope = rememberCoroutineScope()

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
                engine = built.engine
                cwd = built.engine.workingDirectory
                tree = built.tree
                fullscreen = built.fullscreen
                fontScalePercent = built.fontScalePercent
            }

            LaunchedEffect(fullscreen) {
                applyFullscreen(fullscreen)
            }

            HG2GuiTheme(scale = fontScalePercent / 100f) {
                val currentEngine = engine
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

                    currentEngine != null && currentTree != null -> TerminalScreen(
                        tree = currentTree,
                        cwd = cwd,
                        fullscreen = fullscreen,
                        onOpenSettings = { screen = Screen.Settings },
                        onRun = { line, onOutput ->
                            currentEngine.run(line).collect { output ->
                                onOutput(output)
                            }
                            cwd = currentEngine.workingDirectory
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
        engine?.destroy()
        main?.destroy()
        super.onDestroy()
    }
}
