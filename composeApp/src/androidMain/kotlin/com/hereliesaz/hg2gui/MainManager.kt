package com.hereliesaz.hg2gui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Parcelable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.commands.*
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.commands.main.raw.location
import com.hereliesaz.hg2gui.commands.main.specific.RedirectCommand
import com.hereliesaz.hg2gui.managers.*
import com.hereliesaz.hg2gui.managers.music.MusicManager2
import com.hereliesaz.hg2gui.managers.music.MusicService
import com.hereliesaz.hg2gui.managers.notifications.KeeperService
import com.hereliesaz.hg2gui.managers.xml.XMLPrefsManager
import com.hereliesaz.hg2gui.managers.xml.options.Behavior
import com.hereliesaz.hg2gui.managers.xml.options.Theme
import com.hereliesaz.hg2gui.tuils.PrivateIOReceiver
import com.hereliesaz.hg2gui.tuils.StoppableThread
import com.hereliesaz.hg2gui.tuils.Tuils
import com.hereliesaz.hg2gui.tuils.interfaces.CommandExecuter
import com.hereliesaz.hg2gui.tuils.interfaces.OnRedirectionListener
import com.hereliesaz.hg2gui.tuils.interfaces.Redirectator
import com.hereliesaz.hg2gui.tuils.interfaces.Reloadable
import com.hereliesaz.hg2gui.tuils.libsuperuser.Shell
import com.hereliesaz.hg2gui.tuils.libsuperuser.ShellHolder
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.regex.Matcher
import java.util.regex.Pattern

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
 * Core logic coordinator for the application.
 */
class MainManager(private val mContext: Activity, reloadable: Reloadable) {

    companion object {
        // Action strings for Intents used in local broadcasts
        const val ACTION_EXEC = "com.hereliesaz.hg2gui.main_exec"
        const val CMD = "cmd"
        const val NEED_WRITE_INPUT = "writeInput"
        const val ALIAS_NAME = "aliasName"
        const val PARCELABLE = "parcelable"
        const val CMD_COUNT = "cmdCount"
        const val MUSIC_SERVICE = "musicService"

        // Static interactive shell session (shared across the app)
        @JvmField
        var interactive: Shell.Interactive? = null

        // Counter to keep track of command order and avoid race conditions
        @JvmField
        var commandCount = 0
    }

    // --- Redirection Logic ---
    private var redirect: RedirectCommand? = null
    private val redirectator = object : Redirectator {
        override fun prepareRedirection(cmd: Any?) {
            val redirectCmd = cmd as? RedirectCommand ?: return
            redirect = redirectCmd
            redirectionListener?.onRedirectionRequest(redirectCmd)
        }

        override fun cleanup() {
            redirect?.let {
                it.beforeObjects.clear()
                it.afterObjects.clear()
                redirectionListener?.onRedirectionEnd(it)
                redirect = null
            }
        }
    }
    
    private var redirectionListener: OnRedirectionListener? = null
    fun setRedirectionListener(redirectionListener: OnRedirectionListener) {
        this.redirectionListener = redirectionListener
    }

    // Package path where raw command classes are located.
    private val COMMANDS_PKG = "com.hereliesaz.hg2gui.commands.main.raw"

    // --- Triggers ---
    private val triggers = arrayOf<CmdTrigger>(
        GroupTrigger(),
        AliasTrigger(),
        TuiCommandTrigger(),
        AppTrigger(),
        ShellCommandTrigger()
    )

    fun interface CommandCompletionListener {
        fun onCommandComplete()
    }
    
    @Volatile
    private var commandCompletionListener: CommandCompletionListener? = null
    fun setCommandCompletionListener(listener: CommandCompletionListener?) {
        commandCompletionListener = listener
    }

    // MainPack holds references to all managers, passed to commands so they can access system resources.
    val mainPack: MainPack

    // Preferences cached for performance
    private val showAliasValue: Boolean
    private val showAppHistory: Boolean
    private val aliasContentColor: Int
    private val multipleCmdSeparator: String
    private val keeperServiceRunning: Boolean

    // Sub-Managers
    private val aliasManager: AliasManager
    private val rssManager: RssManager
    private val appsManager: AppsManager
    private val contactManager: ContactManager?
    private val musicManager2: MusicManager2?
    private val themeManager: ThemeManager
    private val htmlExtractManager: HTMLExtractManager
    private val commandRepository: CommandRepository
    private var messagesManager: MessagesManager? = null

    private val receiver: BroadcastReceiver

    init {
        // Load preferences
        keeperServiceRunning = XMLPrefsManager.getBoolean(Behavior.tui_notification)
        showAliasValue = XMLPrefsManager.getBoolean(Behavior.show_alias_content)
        showAppHistory = XMLPrefsManager.getBoolean(Behavior.show_launch_history)
        aliasContentColor = XMLPrefsManager.getColor(Theme.alias_content_color)
        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator) ?: ""

        // CommandGroup manages categorization of commands
        val group = CommandGroup(mContext, COMMANDS_PKG)

        contactManager = try {
            ContactManager(mContext)
        } catch (e: Exception) {
            Tuils.log(e)
            null
        }

        appsManager = AppsManager(mContext)
        aliasManager = AliasManager(mContext)

        // HTTP Client for network operations (Weather, RSS)
        val client = OkHttpClient.Builder()
            .cache(Cache(mContext.cacheDir, (10 * 1024 * 1024).toLong()))
            .build()

        // Initialize other managers
        rssManager = RssManager(mContext, client)
        themeManager = ThemeManager(client, mContext, reloadable)
        musicManager2 = if (XMLPrefsManager.getBoolean(Behavior.enable_music)) MusicManager2(mContext) else null
        ChangelogManager.printLog(mContext, client)
        htmlExtractManager = HTMLExtractManager(mContext, client)

        if (XMLPrefsManager.getBoolean(Behavior.show_hints)) {
            messagesManager = MessagesManager(mContext)
        }

        // Initialize Command Repository (indexes available commands)
        commandRepository = CommandRepository()
        // Create the MainPack data transfer object
        mainPack = MainPack(
            mContext, group, aliasManager, appsManager, musicManager2,
            contactManager, redirectator, rssManager, client, commandRepository
        )
        // Populate command repository with available commands
        commandRepository.update(mainPack)

        // Initialize Shell
        val shellHolder = ShellHolder(mContext)
        interactive = shellHolder.build()
        mainPack.shellHolder = shellHolder

        // Register BroadcastReceiver for internal events
        val filter = IntentFilter().apply {
            addAction(ACTION_EXEC)
            addAction(location.ACTION_LOCATION_CMD_GOT)
            addAction(UIManager.ACTION_UPDATE_SUGGESTIONS)
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (action == UIManager.ACTION_UPDATE_SUGGESTIONS) {
                    commandRepository.update(mainPack)
                } else if (action == ACTION_EXEC) {
                    var cmd = intent.getStringExtra(CMD) ?: intent.getStringExtra(PrivateIOReceiver.TEXT) ?: return

                    // Check for stale commands
                    val cmdCount = intent.getIntExtra(CMD_COUNT, -1)
                    if (cmdCount < commandCount) return
                    commandCount++

                    val aliasName = intent.getStringExtra(ALIAS_NAME)
                    val needWriteInput = intent.getBooleanExtra(NEED_WRITE_INPUT, false)
                    val p = intent.getParcelableExtra<Parcelable>(PARCELABLE)

                    // If requested, echo the command to the input field
                    if (needWriteInput) {
                        val i = Intent(PrivateIOReceiver.ACTION_INPUT).apply {
                            putExtra(PrivateIOReceiver.TEXT, cmd)
                        }
                        LocalBroadcastManager.getInstance(context.applicationContext).sendBroadcast(i)
                    }

                    // Execute based on type
                    if (p != null && p is AppsManager.LaunchInfo) {
                        onCommand(cmd, p, intent.getBooleanExtra(MUSIC_SERVICE, false))
                    } else {
                        onCommand(cmd, aliasName, intent.getBooleanExtra(MUSIC_SERVICE, false))
                    }
                } else if (action == location.ACTION_LOCATION_CMD_GOT) {
                    // Handle async location result
                    Tuils.sendOutput(
                        context,
                        "Lat: ${intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0.0)}; Long: ${
                            intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0.0)
                        }"
                    )
                    TuiLocationManager.instance(context).rm(location.ACTION_LOCATION_CMD_GOT)
                }
            }
        }

        LocalBroadcastManager.getInstance(mContext.applicationContext).registerReceiver(receiver, filter)
    }

    /**
     * Updates background services when a command is executed.
     */
    private fun updateServices(cmd: String, wasMusicService: Boolean) {
        if (keeperServiceRunning) {
            val i = Intent(mContext, KeeperService::class.java).apply {
                putExtra(KeeperService.CMD_KEY, cmd)
                putExtra(KeeperService.PATH_KEY, mainPack.currentDirectory?.absolutePath)
            }
            mContext.startService(i)
        }

        if (wasMusicService) {
            val i = Intent(mContext, MusicService::class.java)
            mContext.startService(i)
        }
    }

    fun onCommand(input: String, launchInfo: AppsManager.LaunchInfo?, wasMusicService: Boolean) {
        if (launchInfo == null) {
            onCommand(input, null as String?, wasMusicService)
            return
        }

        updateServices(input, wasMusicService)

        // Verify if the input matches the app label
        if (launchInfo.unspacedLowercaseLabel == Tuils.removeSpaces(input.lowercase())) {
            performLaunch(mainPack, launchInfo, input)
        } else {
            // Fallback to standard processing
            onCommand(input, null as String?, wasMusicService)
        }
    }

    private val colorExtractor = Pattern.compile("(#[^(]{6})\\[([^\\)]*)\\]", Pattern.CASE_INSENSITIVE)

    fun onCommand(input: String, alias: String?, wasMusicService: Boolean) {
        var processedInput = Tuils.removeUnncesarySpaces(input)

        if (alias == null) updateServices(processedInput, wasMusicService)

        // --- Redirection Handling ---
        redirect?.let {
            if (!it.isWaitingPermission) {
                it.afterObjects.add(processedInput)
            }
            val output = it.onRedirect(mainPack)
            Tuils.sendOutput(mContext, output)
            return
        }

        // Show alias expansion if enabled
        if (alias != null && showAliasValue) {
            Tuils.sendOutput(aliasContentColor, mContext, aliasManager.formatLabel(alias, processedInput))
        }

        // --- Multiple Commands ---
        val cmds = if (multipleCmdSeparator.isNotEmpty()) {
            processedInput.split(multipleCmdSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            arrayOf(processedInput)
        }

        // --- Execution Loop ---
        for (c in cmds.indices) {
            var cmd = cmds[c]
            var color = TerminalManager.NO_COLOR

            val m = colorExtractor.matcher(cmd)
            if (m.matches()) {
                try {
                    color = Color.parseColor(m.group(1))
                    cmd = m.group(2)
                } catch (e: Exception) {
                    color = TerminalManager.NO_COLOR
                }
            }

            mainPack.clear()
            mainPack.commandColor = color

            // Iterate through triggers until one handles the command
            for (trigger in triggers) {
                val r = try {
                    trigger.trigger(mainPack, cmd)
                } catch (e: Exception) {
                    Tuils.sendOutput(mContext, Tuils.getStackTrace(e))
                    break
                }
                if (r) {
                    messagesManager?.afterCmd()
                    break
                }
            }
        }
    }

    fun onLongBack() {
        Tuils.sendInput(mContext, Tuils.EMPTYSTRING)
    }

    fun sendPermissionNotGrantedWarning() {
        redirectator.cleanup()
    }

    fun dispose() {
        mainPack.dispose()
    }

    fun destroy() {
        mainPack.destroy()
        TuiLocationManager.disposeStatic()
        messagesManager?.onDestroy()
        themeManager.dispose()
        htmlExtractManager.dispose(mContext)
        aliasManager.dispose()
        LocalBroadcastManager.getInstance(mContext.applicationContext).unregisterReceiver(receiver)

        object : StoppableThread() {
            override fun run() {
                super.run()
                try {
                    interactive?.kill()
                    interactive?.close()
                } catch (e: Exception) {
                    Tuils.log(e)
                    Tuils.toFile(e)
                }
            }
        }.start()
    }

    fun executer(): CommandExecuter {
        return CommandExecuter { input, obj ->
            val li = if (obj is AppsManager.LaunchInfo) obj else null
            onCommand(input, li, false)
        }
    }

    private var appFormat: String? = null
    private var outputColor: Int = 0

    private val pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
    private val pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)
    private val pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE or Pattern.LITERAL)

    fun performLaunch(mainPack: MainPack, i: AppsManager.LaunchInfo, input: String): Boolean {
        val intent = appsManager.getIntent(i) ?: return false

        // Show launch history message if enabled
        if (showAppHistory) {
            if (appFormat == null) {
                appFormat = XMLPrefsManager.get(Behavior.app_launch_format)
                outputColor = XMLPrefsManager.getColor(Theme.output_color)
            }

            var a = appFormat!!
            a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(intent.component!!.className))
            a = pp.matcher(a).replaceAll(Matcher.quoteReplacement(intent.component!!.packageName))
            a = pl.matcher(a).replaceAll(Matcher.quoteReplacement(i.publicLabel))
            a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE))

            val text = SpannableString(a).apply {
                setSpan(ForegroundColorSpan(outputColor), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val s = TimeManager.instance?.replace(text) ?: text

            Tuils.sendOutput(mainPack, s, TerminalManager.CATEGORY_OUTPUT)
        }

        Tuils.getContext(mainPack).startActivity(intent)
        return true
    }

    interface CmdTrigger {
        @Throws(Exception::class)
        fun trigger(info: MainPack, input: String): Boolean
    }

    private inner class AliasTrigger : CmdTrigger {
        override fun trigger(info: MainPack, input: String): Boolean {
            val alias = aliasManager.getAlias(input, true)
            val aliasValue = alias[0] ?: return false
            val aliasName = alias[1]
            val residual = alias[2]

            val expandedValue = aliasManager.format(aliasValue, residual ?: "")
            onCommand(expandedValue, aliasName, false)
            return true
        }
    }

    private inner class GroupTrigger : CmdTrigger {
        override fun trigger(info: MainPack, input: String): Boolean {
            val spaceIndex = input.indexOf(Tuils.SPACE)
            val name: String
            val remainingInput: String?

            if (spaceIndex != -1) {
                name = input.substring(0, spaceIndex)
                remainingInput = input.substring(spaceIndex + 1)
            } else {
                name = input
                remainingInput = null
            }

            val appGroups = info.appsManager.groups
            if (appGroups != null) {
                for (g in appGroups) {
                    if (name == g.name()) {
                        return if (remainingInput == null) {
                            @Suppress("UNCHECKED_CAST")
                            Tuils.sendOutput(
                                mContext,
                                AppsManager.AppUtils.printApps(
                                    AppsManager.AppUtils.labelList(g.members() as List<AppsManager.LaunchInfo>, false)
                                )
                            )
                            true
                        } else {
                            g.use(mainPack, remainingInput)
                        }
                    }
                }
            }
            return false
        }
    }

    private inner class ShellCommandTrigger : CmdTrigger {
        private val CD_CODE = 10
        private val PWD_CODE = 11

        private val result: Shell.OnCommandResultListener = object : Shell.OnCommandResultListener {
            override fun onCommandResult(commandCode: Int, exitCode: Int, output: MutableList<String>) {
                if (commandCode == CD_CODE) {
                    interactive?.addCommand("pwd", PWD_CODE, this)
                } else if (commandCode == PWD_CODE && output.size == 1) {
                    val f = File(output[0])
                    if (f.exists()) {
                        mainPack.currentDirectory = f
                        LocalBroadcastManager.getInstance(mContext.applicationContext)
                            .sendBroadcast(Intent(UIManager.ACTION_UPDATE_HINT))
                    }
                }
            }
        }

        override fun trigger(info: MainPack, input: String): Boolean {
            object : StoppableThread() {
                override fun run() {
                    if (input.trim().equals("su", ignoreCase = true)) {
                        if (Shell.SU.available()) {
                            LocalBroadcastManager.getInstance(mContext.applicationContext)
                                .sendBroadcast(Intent(UIManager.ACTION_ROOT))
                        }
                        interactive?.addCommand("su")
                    } else if (input.contains("cd ")) {
                        interactive?.addCommand(input, CD_CODE, result)
                    } else {
                        interactive?.addCommand(input)
                    }
                }
            }.start()
            return true
        }
    }

    private inner class AppTrigger : CmdTrigger {
        override fun trigger(info: MainPack, input: String): Boolean {
            val i = appsManager.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS)
            return i != null && performLaunch(info, i, input)
        }
    }

    private inner class TuiCommandTrigger : CmdTrigger {
        override fun trigger(info: MainPack, input: String): Boolean {
            val command = CommandTuils.parse(input, info)
            if (command == null) {
                commandCompletionListener?.onCommandComplete()
                return false
            }

            mainPack.lastCommand = input

            object : StoppableThread() {
                override fun run() {
                    super.run()
                    try {
                        // execStream() was declared on StreamableCommand but nothing ever called
                        // it - this always fell through to exec() below instead, so a
                        // StreamableCommand's real work (e.g. bootstrap's download) never
                        // happened; its exec() only ever returns a placeholder string.
                        val stream = command.execStream(info)
                        if (stream != null) {
                            runBlocking {
                                stream.collect { line ->
                                    Tuils.sendOutput(info, line, TerminalManager.CATEGORY_OUTPUT)
                                }
                            }
                        } else {
                            val output = command.exec(info)
                            if (output != null) {
                                Tuils.sendOutput(info, output, TerminalManager.CATEGORY_OUTPUT)
                            }
                        }
                    } catch (e: Exception) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e))
                        Tuils.log(e)
                    } finally {
                        commandCompletionListener?.onCommandComplete()
                    }
                }
            }.start()
            return true
        }
    }

    interface Group {
        fun members(): List<*>?
        fun use(mainPack: MainPack, input: String): Boolean
        fun name(): String?
    }
}
