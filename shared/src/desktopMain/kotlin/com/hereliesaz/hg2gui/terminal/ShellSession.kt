package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.managers.StyledSpan

// Dev-tooling only - see shared/build.gradle.kts's own comment on why this target exists at all.
// The PillMenu/TerminalScreen preview this backs never actually runs a shell (every onRun/onNeedInput
// callback PreviewMain wires up is a no-op or canned response), so this only has to satisfy the
// expect/actual compiler check, not do anything real.
actual class ShellSession actual constructor(homePath: String?) {
    actual constructor(homePath: String?, @Suppress("UNUSED_PARAMETER") shellPath: String) : this(homePath)

    private val home = homePath ?: "/"

    actual val isAlive: Boolean = false
    actual val workingDirectory: String = home

    actual fun exec(command: String): ShellSessionResult =
        ShellSessionResult(output = "", exitCode = -1, workingDirectory = home)

    actual fun interrupt() {
        // No-op: nothing is ever running here to interrupt.
    }

    actual fun stream(
        command: String,
        onLine: (line: String) -> Unit,
        onNeedInput: (prompt: String) -> String?,
        onStderrLine: (line: String) -> Unit,
        onStyledLine: (lines: List<List<StyledSpan>>) -> Unit
    ): Int {
        onLine("shell is not running")
        return -1
    }

    actual fun close() {
        // No-op: nothing is ever running here to close.
    }
}
