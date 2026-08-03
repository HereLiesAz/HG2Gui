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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.TerminalScreen
import com.hereliesaz.hg2gui.ui.menu.CommandTree
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import kotlinx.coroutines.Dispatchers
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Must run before anything below resolves the t-ui folder (loadCommons, and every
        // manager MainManager builds). Tuils.getTuiFolder() needs an application context to
        // resolve an app-scoped storage path; without it there is nowhere sanctioned to fall
        // back to.
        Tuils.init(applicationContext)

        setContent {
            HG2GuiTheme {
                var tree by remember { mutableStateOf<List<MenuNode>?>(null) }
                // The shell reports where it ended up after every command, so `cd` is visible
                // in the header rather than being silently swallowed.
                var cwd by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    val (builtMain, builtEngine, builtTree) = withContext(Dispatchers.Default) {
                        // Must happen before MainManager reads a single XMLPrefsSave, or every
                        // preference read falls back to its default through
                        // exception-swallowing logic instead of the user's saved value — this
                        // activity used to skip it entirely.
                        XMLPrefsManager.loadCommons(this@TerminalActivity)

                        val builtMain = MainManager(this@TerminalActivity, this@TerminalActivity)
                        val builtEngine = TerminalEngine(
                            this@TerminalActivity, builtMain, builtMain.mainPack?.currentDirectory
                        )
                        val builtTree = CommandTree.from(
                            builtMain.mainPack?.commandGroup?.commands?.toList().orEmpty()
                        )
                        Triple(builtMain, builtEngine, builtTree)
                    }

                    main = builtMain
                    engine = builtEngine
                    cwd = builtEngine.workingDirectory
                    tree = builtTree
                }

                val currentEngine = engine
                val currentTree = tree
                if (currentEngine != null && currentTree != null) {
                    TerminalScreen(
                        tree = currentTree,
                        cwd = cwd,
                        onRun = { line ->
                            val result = currentEngine.run(line)
                            cwd = currentEngine.workingDirectory
                            result
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…")
                    }
                }
            }
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

    override fun onDestroy() {
        engine?.destroy()
        main?.destroy()
        super.onDestroy()
    }
}
