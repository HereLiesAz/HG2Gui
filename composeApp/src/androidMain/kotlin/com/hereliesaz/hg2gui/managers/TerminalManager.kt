package com.hereliesaz.hg2gui.managers

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import com.hereliesaz.hg2gui.PanicActivity
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.managers.xml.options.Ui
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.interfaces.CommandExecuter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ScrollView

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

/**
 * Manages the terminal state and history, refactored for Compose.
 * This version removes direct View dependencies and provides a StateFlow for the UI.
 */
class TerminalManager(
    private val mContext: Context,
    private val mainPack: MainPack,
    private val executer: CommandExecuter
) {
    // Legacy View support
    private var terminalView: TextView? = null
    private var inputView: EditText? = null
    private var prefixView: TextView? = null
    private var submitView: ImageView? = null
    private var backView: View? = null
    private var nextView: View? = null
    private var deleteView: View? = null
    private var pasteView: View? = null

    constructor(
        terminalView: TextView,
        inputView: EditText,
        prefixView: TextView,
        submitView: ImageView?,
        backView: View?,
        nextView: View?,
        deleteView: View?,
        pasteView: View?,
        mContext: Context,
        mainPack: MainPack,
        executer: CommandExecuter
    ) : this(mContext, mainPack, executer) {
        this.terminalView = terminalView
        this.inputView = inputView
        this.prefixView = prefixView
        this.submitView = submitView
        this.backView = backView
        this.nextView = nextView
        this.deleteView = deleteView
        this.pasteView = pasteView

        setupLegacyViews()
    }

    private fun setupLegacyViews() {
        setDefaultHint()
        inputView?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                simulateEnter()
                true
            } else false
        }
        submitView?.setOnClickListener { simulateEnter() }
        backView?.setOnClickListener { getPreviousCommand()?.let { setInput(it) } }
        nextView?.setOnClickListener { getNextCommand()?.let { setInput(it) } }
        deleteView?.setOnClickListener { setInput("") }
        pasteView?.setOnClickListener {
            val clipboard = mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.let { setInput(getInput() + it) }
        }
    }

    fun setDefaultHint() {
        inputView?.hint = getPrefix()
    }

    fun setHint(hint: String) {
        inputView?.hint = hint
    }

    fun getTerminalText(): String = terminalView?.text?.toString() ?: ""

    fun scrollToEnd() {
        terminalView?.let { tv ->
            val scroll = tv.parent?.parent as? ScrollView
            scroll?.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun getInput(): String = inputView?.text?.toString() ?: ""

    fun setInput(s: String) {
        inputView?.setText(s)
        focusInputEnd()
    }

    fun simulateEnter() {
        val input = getInput()
        onNewInput(input)
        setInput("")
    }

    fun requestInputFocus() {
        inputView?.requestFocus()
    }

    fun getInputView(): EditText? = inputView

    fun getInputWindowToken() = inputView?.windowToken

    fun focusInputEnd() {
        inputView?.let { it.setSelection(it.text.length) }
    }

    fun onBackPressed() {
        // Legacy back button logic
    }
    companion object {
        const val CATEGORY_INPUT = 10
        const val CATEGORY_OUTPUT = 11
        const val CATEGORY_NO_COLOR = 20
        const val NO_COLOR = Int.MAX_VALUE

        const val FORMAT_INPUT = "%i"
        const val FORMAT_OUTPUT = "%o"
        const val FORMAT_PREFIX = "%p"
        const val FORMAT_NEWLINE = "%n"
    }


    private val _history = MutableStateFlow<List<TerminalHistoryEntry>>(emptyList())
    val history: StateFlow<List<TerminalHistoryEntry>> = _history.asStateFlow()

    private val cmdList = mutableListOf<String>()
    private var howBack = -1
    private val CMD_LIST_SIZE = 40

    private val clearAfterMs = XMLPrefsManager.getInt(Behavior.clear_after_seconds) * 1000
    private val clearAfterCmds = XMLPrefsManager.getInt(Behavior.clear_after_cmds)
    private val maxLines = XMLPrefsManager.getInt(Behavior.max_lines)

    private var clearCmdsCount = 0
    private var suMode = false

    private val prefix: String = XMLPrefsManager.get(Ui.input_prefix) ?: ""
    private val suPrefix: String = XMLPrefsManager.get(Ui.input_root_prefix) ?: ""

    private val handler = Handler(Looper.getMainLooper())
    private val clearRunnable = object : Runnable {
        override fun run() {
            clear()
            if (clearAfterMs > 0) handler.postDelayed(this, clearAfterMs.toLong())
        }
    }

    init {
        if (clearAfterMs > 0) handler.postDelayed(clearRunnable, clearAfterMs.toLong())
    }

    fun onNewInput(input: String, obj: Any? = null) {
        val cmd = input.trim()
        if (cmd.isNotEmpty()) {
            clearCmdsCount++
            if (clearAfterCmds > 0 && clearCmdsCount % clearAfterCmds == 0) clear()

            // Add to history
            if (cmdList.size >= CMD_LIST_SIZE) {
                cmdList.removeAt(0)
            }
            cmdList.add(cmd)
            howBack = -1

            // Add to visual buffer
            _history.update { it + TerminalHistoryEntry(command = cmd, isRunning = true) }
        }

        executer.execute(cmd, obj)
    }

    fun setOutput(output: CharSequence?, type: Int) {
        if (output == null || output.isEmpty()) return
        appendOutput(output.toString(), type)
    }

    fun setOutput(color: Int, output: CharSequence?) {
        if (output == null || output.isEmpty()) return

        if (color == Color.RED) {
            val intent = Intent(mContext, PanicActivity::class.java)
            intent.putExtra(PanicActivity.EXTRA_ERROR_MESSAGE, output.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }

        appendOutput(output.toString(), CATEGORY_OUTPUT)
    }

    /**
     * Appends text to the terminal. Supports streaming by updating the last entry if it's running.
     */
    fun appendOutput(text: String, type: Int = CATEGORY_OUTPUT) {
        _history.update { current ->
            if (current.isEmpty()) {
                listOf(TerminalHistoryEntry(command = "", output = text, type = type))
            } else {
                val last = current.last()
                // If the last entry is a command that is still "running", append to its output.
                // Otherwise, create a new output-only entry.
                if (last.isRunning || last.command.isEmpty()) {
                    val newList = current.toMutableList()
                    newList[newList.size - 1] = last.copy(
                        output = if (last.output.isEmpty()) text else last.output + "\n" + text
                    )
                    newList
                } else {
                    current + TerminalHistoryEntry(command = "", output = text, type = type)
                }
            }
        }
        
        // Handle max lines
        if (maxLines > 0) {
            _history.update { current ->
                if (current.size > maxLines) current.takeLast(maxLines) else current
            }
        }

        // Legacy View update
        terminalView?.let { tv ->
            tv.append((if (tv.text.isNotEmpty()) "\n" else "") + text)
            scrollToEnd()
        }
    }

    fun finishLastCommand() {
        _history.update { current ->
            if (current.isEmpty()) return@update current
            val last = current.last()
            if (last.isRunning) {
                val newList = current.toMutableList()
                newList[newList.size - 1] = last.copy(isRunning = false)
                newList
            } else {
                current
            }
        }
    }

    fun clear() {
        _history.value = emptyList()
        clearCmdsCount = 0
        terminalView?.text = ""
    }

    fun getPreviousCommand(): String? {
        if (cmdList.isEmpty()) return null
        if (howBack == -1) howBack = cmdList.size
        if (howBack > 0) {
            howBack--
            return cmdList[howBack]
        }
        return null
    }

    fun getNextCommand(): String? {
        if (howBack != -1 && howBack < cmdList.size) {
            howBack++
            return if (howBack == cmdList.size) "" else cmdList[howBack]
        }
        return null
    }

    fun onRoot() {
        suMode = true
    }

    fun onStandard() {
        suMode = false
    }

    fun getPrefix(): String = if (suMode) suPrefix else prefix
}
