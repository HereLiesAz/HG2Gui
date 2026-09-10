package com.hereliesaz.hg2gui.terminal

import android.view.KeyEvent as NativeKeyEvent
import com.termux.terminal.KeyHandler
import com.termux.terminal.TerminalEmulator
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Drives a wrapped terminal UI while re-reading semantic state after every generated action.
 * The child application remains authoritative: if expected state cannot be observed, HG2Gui stops
 * generating input and returns control to RAW rather than guessing through a changed screen.
 */
object TuiInteractionController {
    data class Result(val success: Boolean, val reason: String? = null)

    suspend fun activate(holder: FullScreenPtySession, layerIndex: Int, targetIndex: Int): Result {
        val navigated = navigateTo(holder, layerIndex, targetIndex)
        if (!navigated.success) return navigated
        val before = TuiTerminalAdapter.snapshot(holder) ?: return Result(false, "terminal state unavailable")
        val item = before.layers.getOrNull(layerIndex)?.items?.getOrNull(targetIndex)
            ?: return Result(false, "target disappeared")
        val key = if (item.control == TuiControlKind.ACTION) NativeKeyEvent.KEYCODE_ENTER else NativeKeyEvent.KEYCODE_SPACE
        sendKey(holder, key)
        delay(45)
        val after = TuiTerminalAdapter.snapshot(holder) ?: return Result(true)

        if (item.control != TuiControlKind.ACTION) {
            val updated = after.layers.getOrNull(layerIndex)?.items?.getOrNull(targetIndex)
                ?: return Result(false, "control disappeared after toggle")
            if (item.checked != null && updated.checked == item.checked) {
                return Result(false, "application did not acknowledge toggle")
            }
            return Result(true)
        }

        if (after == before) return Result(false, "application did not acknowledge selection")
        return Result(true)
    }

    suspend fun navigateTo(holder: FullScreenPtySession, layerIndex: Int, targetIndex: Int): Result {
        var snapshot = TuiTerminalAdapter.snapshot(holder) ?: return Result(false, "terminal state unavailable")
        var layer = snapshot.layers.getOrNull(layerIndex) ?: return Result(false, "menu layer disappeared")
        var current = layer.activeIndex ?: return Result(false, "menu selection is not observable")
        if (targetIndex !in layer.items.indices) return Result(false, "target is no longer present")

        val targetLabel = layer.items[targetIndex].label
        var guard = 0
        while (current != targetIndex && guard++ < layer.items.size + 4) {
            val direction = if (targetIndex > current) NativeKeyEvent.KEYCODE_DPAD_DOWN else NativeKeyEvent.KEYCODE_DPAD_UP
            sendKey(holder, direction)
            delay(35)
            val next = TuiTerminalAdapter.snapshot(holder) ?: return Result(false, "screen became unreadable while navigating")
            val nextLayer = next.layers.getOrNull(layerIndex) ?: return Result(false, "menu structure changed while navigating")
            val observed = nextLayer.activeIndex ?: return Result(false, "selection stopped being observable")
            if (observed == current) return Result(false, "application did not acknowledge navigation")
            if (targetIndex !in nextLayer.items.indices || nextLayer.items[targetIndex].label != targetLabel) {
                return Result(false, "menu reordered while navigating")
            }
            snapshot = next
            layer = nextLayer
            current = observed
        }
        return if (current == targetIndex) Result(true) else Result(false, "target could not be reconciled")
    }

    suspend fun activateTab(holder: FullScreenPtySession, targetIndex: Int): Result {
        var snapshot = TuiTerminalAdapter.snapshot(holder) ?: return Result(false, "terminal state unavailable")
        var current = snapshot.tabs.indexOfFirst { it.active }.takeIf { it >= 0 }
            ?: return Result(false, "active tab is not observable")
        if (targetIndex !in snapshot.tabs.indices) return Result(false, "tab disappeared")
        val targetLabel = snapshot.tabs[targetIndex].label
        var guard = 0
        while (current != targetIndex && guard++ < snapshot.tabs.size + 2) {
            val key = if (targetIndex > current) NativeKeyEvent.KEYCODE_DPAD_RIGHT else NativeKeyEvent.KEYCODE_DPAD_LEFT
            sendKey(holder, key)
            delay(35)
            val next = TuiTerminalAdapter.snapshot(holder) ?: return Result(false, "screen became unreadable while switching tabs")
            if (targetIndex !in next.tabs.indices || next.tabs[targetIndex].label != targetLabel) {
                return Result(false, "tabs changed while navigating")
            }
            val observed = next.tabs.indexOfFirst { it.active }.takeIf { it >= 0 }
                ?: return Result(false, "active tab stopped being observable")
            if (observed == current) return Result(false, "application did not acknowledge tab navigation")
            snapshot = next
            current = observed
        }
        return if (current == targetIndex) Result(true) else Result(false, "target tab could not be reconciled")
    }

    suspend fun selectResult(holder: FullScreenPtySession, currentRow: Int, targetRow: Int): Result {
        val delta = targetRow - currentRow
        val key = if (delta >= 0) NativeKeyEvent.KEYCODE_DPAD_DOWN else NativeKeyEvent.KEYCODE_DPAD_UP
        repeat(abs(delta)) { sendKey(holder, key); delay(25) }
        sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
        return Result(true)
    }

    /** Sends a real terminal mouse press/release only when the child has enabled mouse tracking. */
    fun mouseClick(holder: FullScreenPtySession, row: Int, column: Int): Result {
        val emulator = holder.session.emulator ?: return Result(false, "terminal state unavailable")
        if (!emulator.isMouseTrackingActive()) return Result(false, "application is not accepting terminal mouse input")
        val terminalRow = row + 1
        val terminalColumn = column.coerceAtLeast(0) + 1
        emulator.sendMouseEvent(TerminalEmulator.MOUSE_LEFT_BUTTON, terminalColumn, terminalRow, true)
        emulator.sendMouseEvent(TerminalEmulator.MOUSE_LEFT_BUTTON, terminalColumn, terminalRow, false)
        return Result(true)
    }

    fun confirm(holder: FullScreenPtySession, yes: Boolean) {
        holder.sendText(if (yes) "y" else "n")
        sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
    }

    private fun sendKey(holder: FullScreenPtySession, keyCode: Int) {
        val code = KeyHandler.getCode(keyCode, 0, holder.cursorApplicationMode, holder.keypadApplicationMode)
        if (code != null) holder.sendText(code)
    }
}
