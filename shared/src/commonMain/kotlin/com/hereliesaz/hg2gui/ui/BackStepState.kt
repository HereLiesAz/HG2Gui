package com.hereliesaz.hg2gui.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Lets a screen that keeps its own internal navigation state (Files' Storage/Search/select-mode/
 * drill depth, the Guide's index/entry drill-down, the path picker's folder depth) report whether
 * it has one internal step left to unwind before the host activity's own system-back handling
 * treats the gesture as "close this screen entirely."
 *
 * The screen updates [canStepBack]/[stepBack] every recomposition (typically via a top-level
 * `SideEffect`) to reflect its current internal state. The host - TerminalActivity's own single
 * outer `BackHandler`, since androidx.activity.compose.BackHandler lives in androidMain and isn't
 * visible from these commonMain screens - checks [canStepBack] first and calls [stepBack] instead
 * of its own full-close handling whenever there's an internal level to step up through first.
 */
class BackStepState {
    var canStepBack: Boolean by mutableStateOf(false)
    var stepBack: () -> Unit by mutableStateOf({})
}
