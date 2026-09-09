package com.hereliesaz.hg2gui.terminal

import android.view.KeyEvent as NativeKeyEvent
import com.termux.terminal.KeyHandler
import kotlinx.coroutines.delay

/**
 * Drives a wrapped terminal UI while re-reading semantic state after every generated action.
 * The child application remains authoritative: if the expected selection cannot be observed,
 * the operation stops instead of blindly continuing through a changed screen.
 */
object TuiInteractionController {
    data class Result(val success: Boolean, val reason: String? = null)

    suspend fun choose(holder: FullScreenPtySession, layerIndex: Int, targetIndex: Int): Result {
        var snapshot = TuiTerminalAdapter.snapshot(holder)
            ?: return Result(false, "terminal state unavailable")
        var layer = snapshot.layers.getOrNull(layerIndex)
            ?: return Result(false, "menu layer disappeared")
        var current = layer.activeIndex
            ?: return Result(false, "menu selection is not observable")
        if (targetIndex !in layer.items.indices) return Result(false, "target is no longer present")

        var guard = 0
        while (current != targetIndex && guard++ < layer.items.size + 4) {
            val direction = if (targetIndex > current) NativeKeyEvent.KEYCODE_DPAD_DOWN else NativeKeyEvent.KEYCODE_DPAD_UP
            sendKey(holder, direction)
            delay(35)
            val next = TuiTerminalAdapter.snapshot(holder)
                ?: return Result(false, "screen became unreadable while navigating")
            val nextLayer = next.layers.getOrNull(layerIndex)
                ?: return Result(false, "menu structure changed while navigating")
            val observed = nextLayer.activeIndex
                ?: return Result(false, "selection stopped being observable")
            if (observed == current) return Result(false, "application did not acknowledge navigation")
            snapshot = next
            layer = nextLayer
            current = observed
        }
        if (current != targetIndex) return Result(false, "target could not be reconciled")

        val expectedLabel = layer.items[targetIndex].label
        sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
        delay(45)
        val after = TuiTerminalAdapter.snapshot(holder)
        if (after != null) {
            val still = after.layers.getOrNull(layerIndex)
            val stillIndex = still?.activeIndex
            if (stillIndex == targetIndex && still.items.getOrNull(targetIndex)?.label == expectedLabel && after == snapshot) {
                return Result(false, "application did not acknowledge selection")
            }
        }
        return Result(true)
    }

    fun activateTab(holder: FullScreenPtySession, currentIndex: Int, targetIndex: Int) {
        val delta = targetIndex - currentIndex
        val key = if (delta >= 0) NativeKeyEvent.KEYCODE_DPAD_RIGHT else NativeKeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(delta)) { sendKey(holder, key) }
    }

    fun toggleCurrent(holder: FullScreenPtySession) = sendKey(holder, NativeKeyEvent.KEYCODE_SPACE)

    fun confirm(holder: FullScreenPtySession, yes: Boolean) {
        holder.sendText(if (yes) "y" else "n")
        sendKey(holder, NativeKeyEvent.KEYCODE_ENTER)
    }

    private fun sendKey(holder: FullScreenPtySession, keyCode: Int) {
        val code = KeyHandler.getCode(keyCode, 0, holder.cursorApplicationMode, holder.keypadApplicationMode)
        if (code != null) holder.sendText(code)
    }
}
