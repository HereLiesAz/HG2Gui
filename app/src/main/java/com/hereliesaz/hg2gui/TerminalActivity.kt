package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.TerminalScreen
import com.hereliesaz.hg2gui.ui.menu.CommandTree

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

    private lateinit var main: MainManager
    private lateinit var engine: TerminalEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        main = MainManager(this, this)
        engine = TerminalEngine(this, main, main.mainPack?.currentDirectory)

        val tree = CommandTree.from(
            main.mainPack?.commandGroup?.commands?.toList().orEmpty()
        )

        setContent {
            HG2GuiTheme {
                // The shell reports where it ended up after every command, so `cd` is visible
                // in the header rather than being silently swallowed.
                var cwd by remember { mutableStateOf(engine.workingDirectory) }

                TerminalScreen(
                    tree = tree,
                    cwd = cwd,
                    onRun = { line ->
                        val result = engine.run(line)
                        cwd = engine.workingDirectory
                        result
                    }
                )
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
        engine.destroy()
        main.destroy()
        super.onDestroy()
    }
}
