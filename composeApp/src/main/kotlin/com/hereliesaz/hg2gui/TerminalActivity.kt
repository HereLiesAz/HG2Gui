package com.hereliesaz.hg2gui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.hereliesaz.hg2gui.ai.AiClient
import com.hereliesaz.hg2gui.ai.AiReply
import com.hereliesaz.hg2gui.azp.AzpClient
import com.hereliesaz.hg2gui.azp.AzpInstaller
import com.hereliesaz.hg2gui.azp.AzpTrust
import com.hereliesaz.hg2gui.azp.ScriptInstaller
import com.hereliesaz.hg2gui.managers.AiSettings
import com.hereliesaz.hg2gui.managers.AzpLibrary
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.OsContextStore
import com.hereliesaz.hg2gui.managers.PtyPreference
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
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.McpServerScreen
import com.hereliesaz.hg2gui.ui.SessionUiState
import com.hereliesaz.hg2gui.ui.SettingsScreen
import com.hereliesaz.hg2gui.ui.TerminalScreen
import com.hereliesaz.hg2gui.ui.WorkflowFlow
import com.hereliesaz.hg2gui.ui.ai.AiChatScreen
import com.hereliesaz.hg2gui.ui.ai.AiMessage
import com.hereliesaz.hg2gui.ui.azp.AzpListing
import com.hereliesaz.hg2gui.ui.azp.AzpStoreScreen
import com.hereliesaz.hg2gui.ui.files.FilesScreen
import com.hereliesaz.hg2gui.ui.files.PathPickerScreen
import com.hereliesaz.hg2gui.ui.files.StorageCategoryStat
import com.hereliesaz.hg2gui.ui.files.StorageStats
import com.hereliesaz.hg2gui.ui.files.VfsEntry
import com.hereliesaz.hg2gui.ui.files.VfsSearchResult
import com.hereliesaz.hg2gui.ui.guide.CommandGuideScreen
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.CommandTree
import com.hereliesaz.hg2gui.ui.menu.FileBrowser
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PerimeterRevealState
import com.hereliesaz.hg2gui.ui.menu.PillPerimeterReveal
import com.hereliesaz.hg2gui.ui.menu.PillWrapReveal
import com.hereliesaz.hg2gui.ui.menu.PillWrapRevealState
import com.hereliesaz.hg2gui.ui.ssh.SshFlow
import java.io.File
import kotlinx.coroutines.CompletableDeferred
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

    private enum class Screen { Terminal, Settings, Guide, Files, Mcp, Ai, AiSettings, Azp }

    /** Confirms enabling shell.* MCP tools with a biometric prompt before persisting the flag -
     *  this is the one switch that lets a paired agent run arbitrary commands, so it gets a
     *  human-present confirmation step, not just a toggle. Disabling never needs this. */
    private fun requestEnableShellExec() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                McpServerService.setShellExecEnabled(this@TerminalActivity, true)
            }

            // MCP-6: only onAuthenticationSucceeded was ever overridden - on a device with no
            // enrolled biometric (the common case for this to fail on), the prompt errors out
            // immediately and this switch was permanently unreachable with zero feedback that
            // anything had even been tapped.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Toast.makeText(this@TerminalActivity, "Couldn't confirm: $errString", Toast.LENGTH_LONG).show()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable shell execution")
            .setSubtitle("Lets a paired MCP client run real shell commands on this device")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }

    /** POST_NOTIFICATIONS gates whether the foreground-service notification - the only visible
     *  sign a shell is exposed to a paired agent - can actually show. The manifest declares it,
     *  but only requesting it here, at the moment the server would start, makes that declaration
     *  do anything; starting the service without ever asking is how it ends up running with no
     *  visible indicator at all on a device where the permission was never granted. */
    private fun startMcpServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PermissionCodes.MCP_NOTIFICATION_REQUEST_PERMISSION
            )
        }
        ContextCompat.startForegroundService(this, Intent(this, McpServerService::class.java))
    }

    private data class InitResult(
        val engine: TerminalEngine,
        val tree: List<MenuNode>,
        val fullscreen: Boolean,
        val fontScalePercent: Int,
        val usePty: Boolean
    )

    private val prefs: SharedPreferences by lazy { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }

    // The registry's signingKeys rarely change, so a successful fetch is cached for the rest of
    // the process rather than re-requested on every install. A *failed* fetch (offline, 5xx,
    // malformed response) is deliberately never cached - caching it would mean no package could
    // ever come back TRUSTED again this process, even once the network recovers.
    private var azpTrustedKeysCache: List<String>? = null

    // AZP-4: null here means "couldn't reach the registry to check" (offline, a 404, a malformed
    // response) - a real signal, distinct from a *successful* fetch that legitimately came back
    // empty (true today: the live registry doesn't publish signingKeys yet). Collapsing both to
    // emptyList() used to make a failed check look identical to "confirmed, nothing to trust
    // yet," and a VALID verdict built on that failure would overstate exactly the confidence a
    // failed check can't back up.
    private suspend fun azpTrustedKeys(): List<String>? {
        azpTrustedKeysCache?.let { return it }
        val keys = try {
            AzpClient.discovery()?.signingKeys?.map { it.publicKey }
        } catch (e: Exception) {
            null
        }
        if (keys != null) azpTrustedKeysCache = keys
        return keys
    }

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
            var usePty by remember { mutableStateOf(false) }
            var aiApiKey by remember { mutableStateOf(AiSettings.apiKey(this@TerminalActivity)) }
            var aiMessages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
            var aiBusy by remember { mutableStateOf(false) }
            var azpResults by remember { mutableStateOf<List<AzpListing>>(emptyList()) }
            var azpBusy by remember { mutableStateOf(false) }
            var azpInstallingId by remember { mutableStateOf<String?>(null) }
            // AZP-2: a script package's dependency/wrapper step (ScriptInstaller.install, which
            // can run `pkg install -y` unconfirmed) only proceeds without asking first when the
            // signature is TRUSTED or VALID - anything else (most saliently UNSIGNED, since only
            // a corrupted signature was ever rejected outright) pauses on this and waits for a tap.
            var azpTrustConfirm by remember { mutableStateOf<Pair<AzpListing, CompletableDeferred<Boolean>>?>(null) }
            // UX-4: closing a tab with a command actually running kills that process mid-run with
            // no warning today - this pauses on a tap first; a tab that's just sitting idle still
            // closes immediately, same as before.
            var closeSessionConfirm by remember { mutableStateOf<TerminalSession?>(null) }
            fun closeSession(closing: TerminalSession) {
                val remaining = sessions.filterNot { it.ui.id == closing.ui.id }
                sessions = remaining
                if (activeSessionId == closing.ui.id) {
                    activeSessionId = remaining.first().ui.id
                }
                closing.engine.destroy()
            }
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

            // The Select File/Folder pill: same "the pill becomes the page" family as Files, but
            // running the screen's full perimeter edge by edge from wherever this pick's own
            // trail crumb lands - see PillPerimeterReveal. onCrumbPositioned (wired into
            // TerminalScreen below) keeps this map fresh with every trail crumb's own rect.
            val pathPickerState = remember { PerimeterRevealState() }
            var pathPickerRoot by remember { mutableStateOf("") }
            val pathPickerHue = remember { Azphalt.hues[Azphalt.hueOf(FileBrowser.WIZARD_PREFIX)] }
            val crumbRects = remember { mutableStateMapOf<String, Rect>() }

            fun closePathPicker() {
                scope.launch { pathPickerState.close() }
            }

            // UI-1/UI-2/UI-3: each of these screens keeps its own internal navigation state
            // privately (Files' Storage/Search/select mode/drill depth, the Guide's index/entry
            // drill-down, the path picker's folder depth) - one instance per screen, handed down
            // so the screen can report "I have a level to step up through" to the BackHandler
            // below instead of it always closing the whole screen. See BackStepState's own doc.
            val filesBackStep = remember { BackStepState() }
            val guideBackStep = remember { BackStepState() }
            val pathPickerBackStep = remember { BackStepState() }

            suspend fun realFsListDir(path: String): List<VfsEntry> = withContext(Dispatchers.IO) {
                val dir = File(path)
                (dir.listFiles() ?: emptyArray())
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    .map { f ->
                        VfsEntry(
                            name = f.name,
                            path = f.absolutePath,
                            isDirectory = f.isDirectory,
                            sizeBytes = if (f.isFile) f.length() else 0L,
                            modifiedAt = f.lastModified()
                        )
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
                // Real device capacity/usage for the "USED OF n GB" framing - the sandboxed vfs
                // model has no way to know either about itself, so both come straight from StatFs
                // on the partition backing the app's private storage rather than anything
                // VfsManager tracks. Used is the partition's own used bytes (capacity minus
                // available), not breakdown.totalBytes - that's only the sandbox's own contents,
                // dividing it by the whole partition's capacity would read as a near-zero percent.
                val (capacityBytes, usedBytes) = try {
                    val statFs = android.os.StatFs(filesDir.path)
                    val capacity = statFs.blockSizeLong * statFs.blockCountLong
                    val available = statFs.blockSizeLong * statFs.availableBlocksLong
                    capacity to (capacity - available)
                } catch (e: Exception) {
                    null to null
                }
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
                    },
                    totalCapacityBytes = capacityBytes,
                    usedCapacityBytes = usedBytes
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
                        fontScalePercent = prefs.getInt(PREF_FONT_SCALE_PERCENT, 100),
                        usePty = PtyPreference.isEnabled(this@TerminalActivity)
                    )
                }

                val firstUi = SessionUiState(id = "1", name = "main", cwd = built.engine.workingDirectory)
                sessions = listOf(TerminalSession(firstUi, built.engine))
                activeSessionId = firstUi.id
                tree = built.tree
                fullscreen = built.fullscreen
                fontScalePercent = built.fontScalePercent
                usePty = built.usePty

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

                // Fire-and-forget: the flags this discovers only show up the next time a pill
                // menu's own resolveChildren runs (see HelpCatalog's own doc comment), never
                // this frame - there's nothing here worth blocking startup on.
                launch(Dispatchers.IO) { CommandTree.warmHelpCache(this@TerminalActivity) }
            }

            LaunchedEffect(fullscreen) {
                applyFullscreen(fullscreen)
            }

            // UI-1/UI-2/UI-3: nothing in the app previously intercepted system back or the
            // edge-swipe gesture, so either one closed the whole app - or, once each screen's own
            // BACK pill was checked here too, whichever secondary screen was open - from any depth
            // in one press, discarding whatever internal navigation state (Files' Storage/Search/
            // select mode/drill chain, the Guide's index/entry drill-down, the path picker's
            // folder depth) that screen was privately sitting on. Each *BackStep above is that
            // screen's own report of "I still have a level to step up through" - checked first, so
            // system back steps up exactly one level at a time, same as each screen's own BACK
            // pill already does, before ever falling through to closing the whole screen. Disabled
            // at the true root (Terminal, nothing open) so system back still backgrounds/exits the
            // app there, same as before.
            val atRoot = screen == Screen.Terminal && !filesWrap.active && !pathPickerState.active
            BackHandler(enabled = !atRoot) {
                when {
                    pathPickerState.active && pathPickerBackStep.canStepBack -> pathPickerBackStep.stepBack()
                    pathPickerState.active -> closePathPicker()
                    (screen == Screen.Files || filesWrap.active) && filesBackStep.canStepBack -> filesBackStep.stepBack()
                    screen == Screen.Files || filesWrap.active -> closeFiles()
                    screen == Screen.Guide && guideBackStep.canStepBack -> guideBackStep.stepBack()
                    screen == Screen.AiSettings -> screen = Screen.Settings
                    screen == Screen.Mcp -> screen = Screen.Settings
                    else -> screen = Screen.Terminal
                }
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
                        usePty = usePty,
                        onUsePtyChange = { value ->
                            usePty = value
                            PtyPreference.setEnabled(this@TerminalActivity, value)
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
                                        val skills = withContext(Dispatchers.IO) {
                                            AzpLibrary.installedSkillTexts(this@TerminalActivity)
                                        }
                                        AiClient.ask(key, session.ui.cwd, question, skills)
                                    } catch (e: Exception) {
                                        AiReply(text = "error: ${e.message}", command = null)
                                    }
                                    aiMessages = aiMessages + AiMessage(
                                        fromUser = false, text = reply.text, command = reply.command, parts = reply.parts
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

                    screen == Screen.Azp -> AzpStoreScreen(
                        fullscreen = fullscreen,
                        results = azpResults,
                        busy = azpBusy,
                        installingId = azpInstallingId,
                        onSearch = { query, kind ->
                            azpBusy = true
                            scope.launch {
                                val response = try {
                                    AzpClient.search(query, kind.takeUnless { it == "all" })
                                } catch (e: Exception) {
                                    null
                                }
                                val installed = withContext(Dispatchers.IO) {
                                    AzpLibrary.installed(this@TerminalActivity).associateBy { it.id }
                                }
                                azpResults = response?.packages.orEmpty().map { pkg ->
                                    val inst = installed[pkg.id]
                                    AzpListing(
                                        id = pkg.id, name = pkg.name, author = pkg.author,
                                        description = pkg.description, version = pkg.version,
                                        kind = pkg.kind, installed = inst != null,
                                        trust = inst?.trust?.name ?: "",
                                        scriptCommand = inst?.let { AzpLibrary.scriptCommand(this@TerminalActivity, it.id) }.orEmpty()
                                    )
                                }
                                azpBusy = false
                            }
                        },
                        onInstall = { listing ->
                            azpInstallingId = listing.id
                            scope.launch {
                                val trustedKeys = azpTrustedKeys()
                                val result = try {
                                    val rawInstall = withContext(Dispatchers.IO) {
                                        val bytes = AzpClient.download(listing.id, listing.version) ?: return@withContext null
                                        AzpInstaller.install(
                                            this@TerminalActivity, listing.id, listing.version, bytes, trustedKeys.orEmpty()
                                        )
                                    }
                                    // A failed key fetch (trustedKeys == null) can only ever have
                                    // checked a package against zero keys - VALID under those
                                    // conditions means "the check couldn't rule anything in or
                                    // out," not "confirmed internally consistent."
                                    val install = if (trustedKeys == null && rawInstall?.trust == AzpTrust.VALID) {
                                        rawInstall.copy(trust = AzpTrust.UNVERIFIABLE)
                                    } else {
                                        rawInstall
                                    }
                                    if (install == null) {
                                        null
                                    } else {
                                        // Only a TRUSTED or VALID signature (an internally-consistent
                                        // one, whether or not the signer is registry-vouched-for)
                                        // proceeds unconfirmed - UNSIGNED and anything else pauses
                                        // here for an explicit tap before running the dependency
                                        // install and wiring an executable onto PATH.
                                        val scriptCommand = install.script?.let { script ->
                                            val proceed = install.trust == AzpTrust.TRUSTED || install.trust == AzpTrust.VALID ||
                                                run {
                                                    val decision = CompletableDeferred<Boolean>()
                                                    azpTrustConfirm = listing to decision
                                                    decision.await()
                                                }
                                            if (proceed) {
                                                val outcome = withContext(Dispatchers.IO) {
                                                    ScriptInstaller.install(this@TerminalActivity, listing.id, listing.version, script)
                                                }
                                                (outcome as? ScriptInstaller.Result.Installed)?.command
                                            } else null
                                        }
                                        withContext(Dispatchers.IO) {
                                            AzpLibrary.record(
                                                this@TerminalActivity, listing.id, listing.name, listing.version,
                                                install.kind, install.skillIds, install.trust, scriptCommand
                                            )
                                        }
                                        install to scriptCommand
                                    }
                                } catch (e: Exception) {
                                    // A dropped connection or a malformed archive/manifest must
                                    // not crash the app - the row just stays on INSTALL.
                                    null
                                }
                                if (result != null) {
                                    val (install, scriptCommand) = result
                                    azpResults = azpResults.map {
                                        if (it.id == listing.id) {
                                            it.copy(installed = true, trust = install.trust.name, scriptCommand = scriptCommand.orEmpty())
                                        } else it
                                    }
                                }
                                azpInstallingId = null
                            }
                        },
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
                            onStart = { startMcpServer() },
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
                        onBack = { screen = Screen.Terminal },
                        backStep = guideBackStep
                    )

                    sessions.isNotEmpty() && currentTree != null -> TerminalScreen(
                        tree = currentTree,
                        sessions = sessions.map { it.ui },
                        activeSessionId = activeSessionId,
                        onSessionPick = { activeSessionId = it },
                        onNewSession = {
                            // A deliberate tap, never mid-gesture - the one place this session
                            // rerolls the ground (capped at Azphalt.MAX_GROUND_REROLLS regardless
                            // of how many new tabs get opened).
                            Azphalt.rerollGround()
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
                                    if (closing.ui.running) {
                                        closeSessionConfirm = closing
                                    } else {
                                        closeSession(closing)
                                    }
                                }
                            }
                        },
                        fullscreen = fullscreen,
                        onOpenSettings = { screen = Screen.Settings },
                        onOpenGuide = { screen = Screen.Guide },
                        onOpenFiles = { openFiles() },
                        onFilesButtonPositioned = { filesOrigin = it },
                        onCrumbPositioned = { id, rect -> crumbRects[id] = rect },
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
                                wizardId == "azp-store" -> screen = Screen.Azp
                                wizardId.startsWith("switchos:") -> {
                                    val os = wizardId.removePrefix("switchos:")
                                    scope.launch {
                                        OsContextStore.set(this@TerminalActivity, os)
                                        tree = withContext(Dispatchers.IO) { CommandTree.from(this@TerminalActivity) }
                                    }
                                }
                                wizardId.startsWith(FileBrowser.WIZARD_PREFIX) -> {
                                    val crumbId = wizardId.removePrefix(FileBrowser.WIZARD_PREFIX)
                                    pathPickerRoot = session?.ui?.cwd?.takeIf { it.isNotBlank() }
                                        ?: CommandTree.pickerRoot(this@TerminalActivity).absolutePath
                                    pathPickerState.origin = crumbRects[crumbId] ?: Rect.Zero
                                    scope.launch { pathPickerState.open() }
                                }
                            }
                        },
                        onRun = { sessionId, line, onOutput, onNeedInput ->
                            val session = sessions.first { it.ui.id == sessionId }
                            session.engine.run(line, onNeedInput).collect { output -> onOutput(output) }
                            session.ui.cwd = session.engine.workingDirectory
                            // A package manager (pkg/apt/apt-get/dpkg) can change what's actually
                            // on PATH; re-scan so the Shell pills reflect that instead of the
                            // snapshot from whenever the tree was last built. Every other command
                            // (ls, pwd, cat, ...) can't change installed packages, so re-running
                            // CommandTree.from's dpkg .list scan after each of those would just be
                            // wasted file I/O and a pointless menu recomposition.
                            val ranCommand = line.trim().substringBefore(' ')
                            if (ranCommand in CommandTree.PACKAGE_MANAGER_COMMANDS) {
                                tree = withContext(Dispatchers.IO) {
                                    // A fresh install can add binaries this pill menu has never
                                    // probed for --help flags before; catch those up here too,
                                    // rather than waiting on the next app launch's own warm-up.
                                    CommandTree.warmHelpCache(this@TerminalActivity)
                                    CommandTree.from(this@TerminalActivity)
                                }
                            }
                        }
                    )

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…")
                    }
                }

                azpTrustConfirm?.let { (listing, decision) ->
                    ConfirmDialog(
                        title = "INSTALL UNVERIFIED PACKAGE?",
                        message = "${listing.name} isn't signed by a trusted key. Installing it will run " +
                            "\"pkg install\" for whatever dependencies it declares and add its command to " +
                            "this device's PATH.",
                        confirmLabel = "INSTALL",
                        onConfirm = { decision.complete(true); azpTrustConfirm = null },
                        onDismiss = { decision.complete(false); azpTrustConfirm = null }
                    )
                }

                closeSessionConfirm?.let { closing ->
                    ConfirmDialog(
                        title = "CLOSE ${closing.ui.name}?",
                        message = "A command is still running in this session - closing it now kills that process.",
                        confirmLabel = "CLOSE",
                        onConfirm = { closeSession(closing); closeSessionConfirm = null },
                        onDismiss = { closeSessionConfirm = null }
                    )
                }

                if (screen == Screen.Files || filesWrap.active) {
                    PillWrapReveal(state = filesWrap, hue = filesHue) {
                        FilesScreen(
                            fullscreen = fullscreen,
                            nowMillis = System.currentTimeMillis(),
                            listDir = { path -> vfsListDir(path) },
                            search = { query -> vfsSearch(query) },
                            storageStats = { vfsStorageStats() },
                            onOpenFile = { path ->
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) {
                                        VfsManager.resolve(this@TerminalActivity, path)
                                    }
                                    if (file != null) {
                                        // SYS-1: handed off via a plain static field, not an Intent
                                        // extra - see EditorActivity.pendingInternalPath.
                                        EditorActivity.pendingInternalPath = file.absolutePath
                                        startActivity(Intent(this@TerminalActivity, EditorActivity::class.java))
                                    }
                                }
                            },
                            onCreateFolder = { parentPath, name ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, parentPath)?.let { VfsManager.mkdir(it, name) } ?: false
                                }
                            },
                            onCreateFile = { parentPath, name ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, parentPath)?.let { VfsManager.touch(it, name) } ?: false
                                }
                            },
                            onDelete = { path ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, path)?.let { VfsManager.delete(it) } ?: false
                                }
                            },
                            onRename = { path, newName ->
                                withContext(Dispatchers.IO) {
                                    VfsManager.resolve(this@TerminalActivity, path)?.let { VfsManager.rename(it, newName) } ?: false
                                }
                            },
                            onMove = { path, targetDirPath ->
                                withContext(Dispatchers.IO) {
                                    val file = VfsManager.resolve(this@TerminalActivity, path)
                                    val target = VfsManager.resolve(this@TerminalActivity, targetDirPath)
                                    if (file != null && target != null) VfsManager.moveInto(file, target) else false
                                }
                            },
                            onCopy = { path, targetDirPath ->
                                withContext(Dispatchers.IO) {
                                    val file = VfsManager.resolve(this@TerminalActivity, path)
                                    val target = VfsManager.resolve(this@TerminalActivity, targetDirPath)
                                    if (file != null && target != null) VfsManager.copyInto(file, target) else false
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
                            onShareMultiple = { paths ->
                                scope.launch {
                                    val uris = withContext(Dispatchers.IO) {
                                        paths.mapNotNull { path ->
                                            VfsManager.resolve(this@TerminalActivity, path)?.takeIf { it.isFile }?.let { file ->
                                                FileProvider.getUriForFile(this@TerminalActivity, GenericFileProvider.PROVIDER_NAME, file)
                                            }
                                        }
                                    }
                                    if (uris.isNotEmpty()) {
                                        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                            type = "*/*"
                                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        startActivity(Intent.createChooser(intent, "Share ${uris.size} items"))
                                    }
                                }
                            },
                            onBack = { closeFiles() },
                            backStep = filesBackStep,
                            modifier = Modifier.then(
                                if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars)
                            )
                        )
                    }
                }

                if (pathPickerState.active) {
                    // Appends straight to session.ui.tokens, same slot PillMenu's own picks feed
                    // - CommandLine's chip row and RUN both read from it directly. The one gap:
                    // PillMenu keeps its own separate trail/tokens for the pill stack's own
                    // display, unaware of this external append, so if a pill were picked again
                    // after this it would overwrite active.tokens with its own (file-less) view.
                    // Harmless today - every command offering this picker treats it as the last
                    // argument - but would need PillMenu to expose a trail-sync hook to stay safe
                    // if a future command sequenced more pills after a file/folder pick.
                    PillPerimeterReveal(state = pathPickerState, hue = pathPickerHue) {
                        PathPickerScreen(
                            startPath = pathPickerRoot,
                            listDir = { path -> realFsListDir(path) },
                            onSelectFile = { path ->
                                sessions.firstOrNull { it.ui.id == activeSessionId }?.let { session ->
                                    session.ui.tokens = session.ui.tokens + path
                                    session.ui.inputText = ""
                                }
                                closePathPicker()
                            },
                            onSelectFolder = { path ->
                                sessions.firstOrNull { it.ui.id == activeSessionId }?.let { session ->
                                    session.ui.tokens = session.ui.tokens + path
                                    session.ui.inputText = ""
                                }
                                closePathPicker()
                            },
                            onCancel = { closePathPicker() },
                            backStep = pathPickerBackStep,
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
