package com.hereliesaz.hg2gui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.ai.AiClient
import com.hereliesaz.hg2gui.ai.AiReply
import com.hereliesaz.hg2gui.managers.AiSettings
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.SshPresets
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.managers.WorkflowStore
import com.hereliesaz.hg2gui.mcp.McpServerService
import com.hereliesaz.hg2gui.terminal.Builtins
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.util.GenericFileProvider
import com.hereliesaz.hg2gui.util.Utils
import com.hereliesaz.hg2gui.ui.AiSettingsScreen
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.McpServerScreen
import com.hereliesaz.hg2gui.ui.SessionUiState
import com.hereliesaz.hg2gui.ui.SettingsScreen
import com.hereliesaz.hg2gui.ui.TerminalScreen
import com.hereliesaz.hg2gui.ui.WorkflowFlow
import com.hereliesaz.hg2gui.ui.ai.AiChatScreen
import com.hereliesaz.hg2gui.ui.ai.AiMessage
import com.hereliesaz.hg2gui.ui.files.FilesScreen
import com.hereliesaz.hg2gui.ui.files.StorageCategoryStat
import com.hereliesaz.hg2gui.ui.files.StorageStats
import com.hereliesaz.hg2gui.ui.files.VfsEntry
import com.hereliesaz.hg2gui.ui.files.VfsSearchResult
import com.hereliesaz.hg2gui.ui.guide.CommandGuideScreen
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.CommandTree
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillWrapReveal
import com.hereliesaz.hg2gui.ui.menu.PillWrapRevealState
import com.hereliesaz.hg2gui.ui.ssh.SshFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A live shell paired with the UI state (scrollback, history, cwd) that only it owns. */
private class TerminalSession(val ui: SessionUiState, val engine: TerminalEngine)

private const val PREFS_NAME = "hg2gui_prefs"
private const val PREF_FULLSCREEN = "fullscreen"
private const val PREF_FONT_SCALE_PERCENT = "font_scale_percent"

class TerminalActivity : FragmentActivity() {

    private var sessions by mutableStateOf(listOf<TerminalSession>())
    private var nextSessionNumber = 2

    private enum class Screen { Terminal, Settings, Guide, Files, Mcp, Ai, AiSettings }

    /** Confirms enabling shell.* MCP tools with a biometric prompt before persisting the flag -
     *  this is the one switch that lets a paired agent run arbitrary commands, so it gets a
     *  human-present confirmation step, not just a toggle. Disabling never needs this. */
    private fun requestEnableShellExec() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                McpServerService.setShellExecEnabled(this@TerminalActivity, true)
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable shell execution")
            .setSubtitle("Lets a paired MCP client run real shell commands on this device")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    private data class InitResult(
        val engine: TerminalEngine,
        val tree: List<MenuNode>,
        val fullscreen: Boolean,
        val fontScalePercent: Int
    )

    private val prefs: SharedPreferences by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    // TerminalActivity is singleTop/intoExisting, so a notification tap while it's already
    // running arrives via onNewIntent, not a fresh onCreate - this is how that reaches the
    // Compose tree to switch screens, since setIntent() alone wouldn't trigger recomposition.
    private var lastIntentExtra by mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lastIntentExtra = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Utils.init(applicationContext)
        McpServerService.ensureInitialized(applicationContext)

        setContent {
            var tree by remember { mutableStateOf<List<MenuNode>?>(null) }
            var activeSessionId by remember { mutableStateOf("") }
            var screen by remember {
                mutableStateOf(if (intent?.getBooleanExtra(McpServerService.EXTRA_OPEN_MCP, false) == true) Screen.Mcp else Screen.Terminal)
            }
            var fullscreen by remember { mutableStateOf(false) }
            var fontScalePercent by remember { mutableStateOf(100) }
            var aiApiKey by remember { mutableStateOf(AiSettings.apiKey(this@TerminalActivity)) }
            var aiMessages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
            var aiBusy by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(lastIntentExtra) {
                if (lastIntentExtra?.getBooleanExtra(McpServerService.EXTRA_OPEN_MCP, false) == true) {
                    screen = Screen.Mcp
                }
            }

            // "The pill becomes the page": the Files pill grows out around the screen edge,
            // then the loop it closes floods with a vertical wipe that reveals the file
            // explorer already open on its root - see PillWrapReveal.
            val filesWrap = remember { PillWrapRevealState() }
            var filesOrigin by remember { mutableStateOf(Rect.Zero) }
            val filesHue = remember { Azphalt.hues[Azphalt.hueOf("/")] }
            fun openFiles() {
                filesWrap.origin = filesOrigin
                screen = Screen.Files
                scope.launch { filesWrap.open() }
            }
            fun closeFiles() {
                scope.launch {
                    filesWrap.close()
                    screen = Screen.Terminal
                }
            }

            suspend fun vfsListDir(path: String): List<VfsEntry> = withContext(Dispatchers.IO) {
                val dir = VfsManager.resolve(this@TerminalActivity, path) ?: return@withContext emptyList()
                (dir.listFiles() ?: emptyArray())
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    .map { f ->
                        VfsEntry(
                            name = f.name,
                            path = VfsManager.pathOf(this@TerminalActivity, f),
                            isDirectory = f.isDirectory,
                            sizeBytes = if (f.isFile) f.length() else 0L,
                            modifiedAt = f.lastModified(),
                            isImage = f.isFile && VfsManager.isImage(f)
                        )
                    }
            }

            suspend fun vfsSearch(query: String): List<VfsSearchResult> = withContext(Dispatchers.IO) {
                VfsManager.search(this@TerminalActivity, query).map { f ->
                    VfsSearchResult(
                        entry = VfsEntry(
                            name = f.name,
                            path = VfsManager.pathOf(this@TerminalActivity, f),
                            isDirectory = f.isDirectory,
                            sizeBytes = if (f.isFile) f.length() else 0L,
                            modifiedAt = f.lastModified(),
                            isImage = f.isFile && VfsManager.isImage(f)
                        ),
                        parentPath = VfsManager.pathOf(this@TerminalActivity, f.parentFile ?: f)
                    )
                }
            }

            suspend fun vfsStorageStats(): StorageStats = withContext(Dispatchers.IO) {
                val breakdown = VfsManager.storageByType(this@TerminalActivity)
                StorageStats(
                    totalBytes = breakdown.totalBytes,
                    byCategory = breakdown.byCategory.map { (category, bytes) -> StorageCategoryStat(category.label, bytes) },
                    largest = breakdown.largestFiles.map { f ->
                        VfsEntry(
                            name = f.name,
                            path = VfsManager.pathOf(this@TerminalActivity, f),
                            isDirectory = f.isDirectory,
                            sizeBytes = f.length(),
                            modifiedAt = f.lastModified(),
                            isImage = VfsManager.isImage(f)
                        )
                    }
                )
            }

            LaunchedEffect(Unit) {
                val built = withContext(Dispatchers.Default) {
                    // A no-op before a bootstrap exists; picks up an install from before these
                    // scripts existed too, not just a fresh one.
                    DistroManager.ensureBundledScripts(this@TerminalActivity)
                    val builtEngine = TerminalEngine(this@TerminalActivity)
                    val builtTree = CommandTree.from(this@TerminalActivity)
                    InitResult(
                        engine = builtEngine,
                        tree = builtTree,
                        fullscreen = prefs.getBoolean(PREF_FULLSCREEN, false),
                        fontScalePercent = prefs.getInt(PREF_FONT_SCALE_PERCENT, 100)
                    )
                }

                val firstUi = SessionUiState(id = "1", name = "main", cwd = built.engine.workingDirectory)
                sessions = listOf(TerminalSession(firstUi, built.engine))
                activeSessionId = firstUi.id
                tree = built.tree
                fullscreen = built.fullscreen
                fontScalePercent = built.fontScalePercent

                // Real Termux never shows an empty shell either - it installs its own bootstrap
                // automatically, once, before the first prompt appears. Match that instead of
                // leaving Shell showing only the one pill that fixes it, waiting to be found.
                if (!DistroManager.isInstalled(this@TerminalActivity)) {
                    firstUi.running = true
                    val entryId = firstUi.buffer.size
                    firstUi.buffer = firstUi.buffer + TerminalHistoryEntry(command = "bootstrap", isRunning = true)
                    built.engine.run("bootstrap") { "" }.collect { output ->
                        firstUi.buffer = firstUi.buffer.mapIndexed { i, e ->
                            if (i == entryId) e.copy(output = output) else e
                        }
                    }
                    firstUi.buffer = firstUi.buffer.mapIndexed { i, e ->
                        if (i == entryId) e.copy(isRunning = false) else e
                    }
                    firstUi.running = false
                    tree = withContext(Dispatchers.IO) {
                        DistroManager.ensureBundledScripts(this@TerminalActivity)
                        CommandTree.from(this@TerminalActivity)
                    }
                }
            }

            LaunchedEffect(fullscreen) {
                applyFullscreen(fullscreen)
            }

            HG2GuiTheme(scale = fontScalePercent / 100f) {
                val currentTree = tree
                Box(Modifier.fillMaxSize()) {
                when {
                    screen == Screen.Settings -> SettingsScreen(
                        fullscreen = fullscreen,
                        onFullscreenChange = { value ->
                            fullscreen = value
                            prefs.edit { putBoolean(PREF_FULLSCREEN, value) }
                        },
                        fontScalePercent = fontScalePercent,
                        onFontScalePercentChange = { value ->
                            fontScalePercent = value
                            prefs.edit { putInt(PREF_FONT_SCALE_PERCENT, value) }
                        },
                        onOpenMcpServer = { screen = Screen.Mcp },
                        onOpenAiSettings = { screen = Screen.AiSettings },
                        onBack = { screen = Screen.Terminal }
                    )

                    screen == Screen.AiSettings -> AiSettingsScreen(
                        fullscreen = fullscreen,
                        apiKey = aiApiKey,
                        onSave = { newKey ->
                            aiApiKey = newKey
                            AiSettings.setApiKey(this@TerminalActivity, newKey)
                            screen = Screen.Settings
                        },
                        onBack = { screen = Screen.Settings }
                    )

                    screen == Screen.Ai -> AiChatScreen(
                        fullscreen = fullscreen,
                        messages = aiMessages,
                        apiKeyConfigured = !aiApiKey.isNullOrBlank(),
                        busy = aiBusy,
                        onAsk = { question ->
                            val key = aiApiKey
                            val session = sessions.firstOrNull { it.ui.id == activeSessionId }
                            if (key != null && session != null) {
                                aiMessages = aiMessages + AiMessage(fromUser = true, text = question)
                                aiBusy = true
                                scope.launch {
                                    val reply = try {
                                        AiClient.ask(key, session.ui.cwd, question)
                                    } catch (e: Exception) {
                                        AiReply(text = "error: ${e.message}", command = null)
                                    }
                                    aiMessages = aiMessages + AiMessage(
                                        fromUser = false, text = reply.text, command = reply.command
                                    )
                                    aiBusy = false
                                }
                            }
                        },
                        onUseCommand = { command ->
                            sessions.firstOrNull { it.ui.id == activeSessionId }?.let { session ->
                                session.ui.tokens = emptyList()
                                session.ui.inputText = command
                            }
                            screen = Screen.Terminal
                        },
                        onOpenSettings = { screen = Screen.AiSettings },
                        onBack = { screen = Screen.Terminal }
                    )

                    screen == Screen.Mcp -> {
                        val running by McpServerService.isRunning.collectAsState()
                        val token by McpServerService.token.collectAsState()
                        val port by McpServerService.port.collectAsState()
                        val shellExecEnabled by McpServerService.shellExecEnabled.collectAsState()
                        McpServerScreen(
                            fullscreen = fullscreen,
                            running = running,
                            port = port,
                            token = token,
                            shellExecEnabled = shellExecEnabled,
                            onStart = { ContextCompat.startForegroundService(this@TerminalActivity, Intent(this@TerminalActivity, McpServerService::class.java)) },
                            onStop = {
                                startService(Intent(this@TerminalActivity, McpServerService::class.java).apply { action = McpServerService.ACTION_STOP })
                            },
                            onRequestShellExec = { requestEnableShellExec() },
                            onDisableShellExec = { McpServerService.setShellExecEnabled(this@TerminalActivity, false) },
                            onBack = { screen = Screen.Settings }
                        )
                    }

                    screen == Screen.Guide -> CommandGuideScreen(
                        tree = currentTree.orEmpty(),
                        fullscreen = fullscreen,
                        onCommandSelected = { tokens ->
                            sessions.firstOrNull { it.ui.id == activeSessionId }?.let { session ->
                                session.ui.tokens = tokens
                                session.ui.inputText = ""
                            }
                        },
                        onBack = { screen = Screen.Terminal }
                    )

                    sessions.isNotEmpty() && currentTree != null -> TerminalScreen(
                        tree = currentTree,
                        sessions = sessions.map { it.ui },
                        activeSessionId = activeSessionId,
                        onSessionPick = { activeSessionId = it },
                        onNewSession = {
                            scope.launch {
                                val newEngine = withContext(Dispatchers.Default) {
                                    TerminalEngine(this@TerminalActivity)
                                }
                                val id = (nextSessionNumber++).toString()
                                val newUi = SessionUiState(
                                    id = id, name = "session $id", cwd = newEngine.workingDirectory
                                )
                                sessions = sessions + TerminalSession(newUi, newEngine)
                                activeSessionId = id
                            }
                        },
                        onCloseSession = { id ->
                            if (sessions.size > 1) {
                                val closing = sessions.firstOrNull { it.ui.id == id }
                                if (closing != null) {
                                    val remaining = sessions.filterNot { it.ui.id == id }
                                    sessions = remaining
                                    if (activeSessionId == id) {
                                        activeSessionId = remaining.first().ui.id
                                    }
                                    closing.engine.destroy()
                                }
                            }
                        },
                        fullscreen = fullscreen,
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenGuide = { screen = Screen.Guide },
                        onOpenFiles = { openFiles() },
                        onFilesButtonPositioned = { filesOrigin = it },
                        onCopy = { text ->
                            val clipboard = getSystemService(ClipboardManager::class.java)
                            clipboard?.setPrimaryClip(ClipData.newPlainText("HG2Gui", text))
                        },
                        onShare = { text ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            startActivity(Intent.createChooser(intent, "Share"))
                        },
                        onWizard = { wizardId ->
                            val session = sessions.firstOrNull { it.ui.id == activeSessionId }
                            when {
                                wizardId == "ssh-new" && session != null -> scope.launch {
                                    SshFlow.runNewConnectionWizard(session.ui) { preset ->
                                        withContext(Dispatchers.IO) { SshPresets.save(this@TerminalActivity, preset) }
                                    }
                                }
                                wizardId == "workflow-new" && session != null -> scope.launch {
                                    WorkflowFlow.runNewWorkflowWizard(session.ui) { workflow ->
                                        withContext(Dispatchers.IO) { WorkflowStore.save(this@TerminalActivity, workflow) }
                                    }
                                }
                                wizardId.startsWith("workflow-run:") && session != null -> {
                                    val name = wizardId.removePrefix("workflow-run:")
                                    scope.launch {
                                        val workflow = withContext(Dispatchers.IO) {
                                            WorkflowStore.list(this@TerminalActivity).find { it.name == name }
                                        }
                                        if (workflow != null) WorkflowFlow.runWorkflowWizard(session.ui, workflow)
                                    }
                                }
                                wizardId == "ai-chat" -> screen = Screen.Ai
                            }
                        },
                        onRun = { sessionId, line, onOutput, onNeedInput ->
                            val session = sessions.first { it.ui.id == sessionId }
                            session.engine.run(line, onNeedInput).collect { output -> onOutput(output) }
                            session.ui.cwd = session.engine.workingDirectory
                            // A package manager (pkg/apt/pip/npm) can change what's actually on
                            // PATH; re-scan so the Shell pills reflect that instead of the
                            // snapshot from whenever the tree was last built.
                            tree = withContext(Dispatchers.IO) { CommandTree.from(this@TerminalActivity) }
                        }
                    )

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…")
                    }
                }

                if (screen == Screen.Files || filesWrap.active) {
                    PillWrapReveal(state = filesWrap, hue = filesHue) {
                        FilesScreen(
                            fullscreen = fullscreen,
                            listDir = { path -> vfsListDir(path) },
                            search = { query -> vfsSearch(query) },
                            storageStats = { vfsStorageStats() },
                            onOpenFile = { path ->
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        VfsManager.resolve(this@TerminalActivity, path)
                                    }
                                    if (file != null) {
                                        val intent = Intent(this@TerminalActivity, EditorActivity::class.java)
                                        intent.putExtra(EditorActivity.PATH, file.absolutePath)
                                        startActivity(intent)
                                    }
                                }
                            },
                            onCreateFolder = { parentPath, name ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, parentPath)?.let { VfsManager.mkdir(it, name) }
                                }
                            },
                            onCreateFile = { parentPath, name ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, parentPath)?.let { VfsManager.touch(it, name) }
                                }
                            },
                            onDelete = { path ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, path)?.let { VfsManager.delete(it) }
                                }
                            },
                            onRename = { path, newName ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, path)?.let { VfsManager.rename(it, newName) }
                                }
                            },
                            onMove = { path, targetDirPath ->
                                withContext(Dispatchers.IO) {
                                    val file = VfsManager.resolve(this@TerminalActivity, path)
                                    val target = VfsManager.resolve(this@TerminalActivity, targetDirPath)
                                    if (file != null && target != null) VfsManager.moveInto(file, target)
                                }
                            },
                            onCopy = { path, targetDirPath ->
                                withContext(Dispatchers.IO) {
                                    val file = VfsManager.resolve(this@TerminalActivity, path)
                                    val target = VfsManager.resolve(this@TerminalActivity, targetDirPath)
                                    if (file != null && target != null) VfsManager.copyInto(file, target)
                                }
                            },
                            onShare = { path ->
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        VfsManager.resolve(this@TerminalActivity, path)
                                    }
                                    if (file != null && file.isFile) {
                                        val uri = FileProvider.getUriForFile(this@TerminalActivity, GenericFileProvider.PROVIDER_NAME, file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "*/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        startActivity(Intent.createChooser(intent, "Share ${file.name}"))
                                    }
                                }
                            },
                            onBack = { closeFiles() },
                            modifier = Modifier.then(
                                if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars)
                            )
                        )
                    }
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (requestCode == PermissionCodes.COMMAND_SUGGESTION_REQUEST_PERMISSION && granted) {
            // ContactManager's own refresh is what's listening for this - it's the one builtin
            // whose permission grant needs to kick off work with no further user input.
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ContactManager.ACTION_REFRESH))
        }
        // Every other builtin's permission request (COMMAND_REQUEST_PERMISSION) is answered by
        // just running the same pill or command again - the system permission dialog itself is
        // the only feedback needed in the meantime.
    }

    override fun onDestroy() {
        sessions.forEach { it.engine.destroy() }
        Builtins.destroy(this)
        super.onDestroy()
    }
}
