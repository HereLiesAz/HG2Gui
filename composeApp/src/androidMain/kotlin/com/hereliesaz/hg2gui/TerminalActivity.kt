package com.hereliesaz.hg2gui

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

/*
 * The entry point. This is a terminal app, not a launcher:
 *
 *   - it appears in recents and keeps its state (no clearTaskOnLaunch, no excludeFromRecents,
 *     no stateNotNeeded, no empty taskAffinity)
 *   - launchMode is singleTop, not singleTask
 *   - it does not answer category.HOME, and does not need to be the home screen to be useful
 *   - the soft keyboard is not forced open: the suggestion tree is the input method
 */
class TerminalActivity : ComponentActivity(), Reloadable {

    // Null until background init finishes. Building these before the first frame held the
    // splash screen up indefinitely: MainManager's constructor runs a contacts query, a full
    // package-manager scan, OkHttp setup, a changelog fetch and a shell spawn, and the command
    // tree comes from a full APK-wide class scan (CommandGroup -> DexFile.entries()) — none of
    // it async, all of it between onCreate and setContent. Android never had a frame to
    // dismiss the splash with. Both are only ever written from the main dispatcher (after the
    // background withContext block returns, not inside it), so no synchronization is needed to
    // read them safely from onDestroy.
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

        // Must run before anything below resolves the t-ui folder (loadCommons, and every
        // manager MainManager builds). Tuils.getTuiFolder() needs an application context to
        // resolve an app-scoped storage path; without it there is nowhere sanctioned to fall
        // back to.
        Tuils.init(applicationContext)

        setContent {
            var tree by remember { mutableStateOf<List<MenuNode>?>(null) }
            // The shell reports where it ended up after every command, so `cd` is visible
            // in the header rather than being silently swallowed.
            var cwd by remember { mutableStateOf("") }
            var screen by remember { mutableStateOf(Screen.Terminal) }

            // Ui.fullscreen/Ui.font_scale_percent read once background init loads commons;
            // false/100 (the Ui defaults) until then, so a slow first read never shows a
            // flash of the wrong UI scale — it just starts at 1x like every prior build did.
            var fullscreen by remember { mutableStateOf(false) }
            var fontScalePercent by remember { mutableStateOf(100) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val built = withContext(Dispatchers.Default) {
                    // Must happen before MainManager reads a single XMLPrefsSave, or every
                    // preference read falls back to its default through exception-swallowing
                    // logic instead of the user's saved value — this activity used to skip it
                    // entirely.
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

            // Applies on first read and on every change from Settings — not just once at
            // startup — since fullscreen is something the user can flip live.
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
                            // Applied to the window immediately via the LaunchedEffect(fullscreen)
                            // above, which fires on this state change too — the persisted write
                            // is disk I/O and stays off the main thread, but the setting itself
                            // isn't waiting on it.
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
                        onRun = { line ->
                            val result = currentEngine.run(line)
                            cwd = currentEngine.workingDirectory
                            result
                        }
                    )

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…")
                    }
                }
            }
        }
    }

    /**
     * true: bars hidden, content free to draw under them (TerminalScreen skips the systemBars
     * inset padding to match). false: bars shown; setDecorFitsSystemWindows still leaves the
     * app drawing edge-to-edge (enableEdgeToEdge() above), so TerminalScreen pads itself to the
     * bars instead of relying on the window to reserve that space — the same enableEdgeToEdge()
     * call is why the reporter's screenshot showed pills drawn straight under the nav bar to
     * begin with.
     */
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

    /** A theme change restarts the terminal, the same way it restarted the launcher. */
    override fun reload() {
        finish()
        startActivity(intent)
    }

    override fun addMessage(header: String?, message: String?) {
        // Reload messages were a launcher-startup affordance. The terminal reports through the
        // record tile instead, so there is nothing to queue here.
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) { // COMMAND_REQUEST_PERMISSION
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
