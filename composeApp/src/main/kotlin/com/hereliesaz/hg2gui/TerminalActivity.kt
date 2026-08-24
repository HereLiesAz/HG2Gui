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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
import com.hereliesaz.hg2gui.managers.CommandHistoryStore
import com.hereliesaz.hg2gui.managers.ContactManager
import com.hereliesaz.hg2gui.managers.OsContextStore
import com.hereliesaz.hg2gui.managers.previewFile
import com.hereliesaz.hg2gui.managers.PtyPreference
import com.hereliesaz.hg2gui.managers.SshPresets
import com.hereliesaz.hg2gui.managers.TerminalHistoryEntry
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.managers.WorkflowStore
import com.hereliesaz.hg2gui.mcp.McpServerService
import com.hereliesaz.hg2gui.terminal.Builtins
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.FullScreenPtySession
import com.hereliesaz.hg2gui.terminal.ShellSession
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import com.hereliesaz.hg2gui.terminal.fullScreenCommandOf
import com.hereliesaz.hg2gui.util.GenericFileProvider
import com.hereliesaz.hg2gui.util.Utils
import com.hereliesaz.hg2gui.ui.AiSettingsScreen
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.EnvironmentScreen
import com.hereliesaz.hg2gui.ui.HistoryScreen
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.JobProgressBar
import com.hereliesaz.hg2gui.ui.JobProgressBarState
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
import com.hereliesaz.hg2gui.ui.ssh.SshFlow
import com.hereliesaz.hg2gui.ui.terminal.FullScreenTerminalScreen
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A live shell paired with the UI state (scrollback, history, cwd) that only it owns. */
private class TerminalSession(val ui: SessionUiState, val engine: TerminalEngine)

// Matches the fallback PillWrapReveal.kt/PillPerimeterReveal.kt's own render code uses when a
// reveal's origin rect hasn't been reported yet - see this file's own revealDefaultBaseWidthPx.
private val REVEAL_DEFAULT_BASE_WIDTH = 64.dp
private val REVEAL_MIN_THICKNESS = 34.dp

private const val PREFS_NAME = "hg2gui_prefs"
private const val PREF_FULLSCREEN = "fullscreen"
private const val PREF_FONT_SCALE_PERCENT = "font_scale_percent"

// DistroManager.bootstrap()'s own progress line - "Downloaded: 12MB / 40MB" - parsed rather than
// having that Flow<String> emit structured progress instead, which would ripple the shared
// TerminalEngine.run contract every other command (a real shell command, a Builtin) also uses.
// Total is "?" when the server didn't report Content-Length, which this simply doesn't match -
// no fraction to show without a real denominator, so the bar just holds wherever it last was.
private val BOOTSTRAP_PROGRESS_LINE = Regex("""Downloaded: (\d+)MB / (\d+)MB""")

internal fun bootstrapDownloadFraction(line: String): Float? =
    BOOTSTRAP_PROGRESS_LINE.find(line)?.destructured?.let { (downloaded, total) ->
        total.toFloatOrNull()?.takeIf { it > 0f }?.let { totalMb ->
            (downloaded.toFloat() / totalMb).coerceIn(0f, 1f)
        }
    }

class TerminalActivity : FragmentActivity() {

    private var sessions by mutableStateOf(listOf<TerminalSession>())
    private var nextSessionNumber = 2
    // Non-null only while the first-launch bootstrap download is in flight - see the
    // JobProgressBar overlay and its own doc comment for why this tracks real byte progress
    // rather than the fixed, indeterminate timeline the Motion Sheet's own demo uses.
    private var bootstrapProgress by mutableStateOf<JobProgressBarState?>(null)

    private enum class Screen { Terminal, Settings, Guide, Files, Mcp, Ai, AiSettings, Azp, FullScreenApp, Environment, History }

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
            // Screen.AiSettings is reachable from both Settings and the AI chat - every exit used
            // to hardcode a return to Settings regardless of which one actually opened it, so
            // entering from the chat and then saving/backing out stranded the user on a screen
            // they never visited in this chain. Set right before navigating to AiSettings, read by
            // every one of its own exits below.
            var aiSettingsCameFrom by remember { mutableStateOf(Screen.Settings) }
            var fullscreen by remember { mutableStateOf(false) }
            var fontScalePercent by remember { mutableStateOf(100) }
            var usePty by remember { mutableStateOf(false) }
            var aiApiKey by remember { mutableStateOf(AiSettings.apiKey(this@TerminalActivity)) }
            var aiMessages by remember { mutableStateOf<List<AiMessage>>(emptyList()) }
            var aiBusy by remember { mutableStateOf(false) }
            var azpResults by remember { mutableStateOf<List<AzpListing>>(emptyList()) }
            var azpBusy by remember { mutableStateOf(false) }
            // Cancelled before every new search starts (see onSearch below) so a stale response
            // from an earlier query/kind can never land after a newer one and silently overwrite
            // it - AzpStoreScreen already gates its own kind chips/search field on !azpBusy, but
            // this is what actually makes overlapping requests impossible rather than just unlikely.
            var azpSearchJob by remember { mutableStateOf<Job?>(null) }
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
            // S1: set right before screen = Screen.FullScreenApp (onRun's dispatch below); torn
            // down (session.kill()) on every exit path - back gesture, the ✕ pill, or the child
            // process finishing on its own - so a killed/finished pty never lingers.
            var fullScreenSession by remember { mutableStateOf<FullScreenPtySession?>(null) }
            // W5: cleared every time History opens so a stale search from a previous visit
            // doesn't linger; see CommandHistoryStore's own doc comment for the log itself.
            var historyQuery by remember { mutableStateOf("") }
            fun closeSession(closing: TerminalSession) {
                val remaining = sessions.filterNot { it.ui.id == closing.ui.id }
                sessions = remaining
                if (activeSessionId == closing.ui.id) {
                    activeSessionId = remaining.first().ui.id
                }
                closing.engine.destroy()
            }
            val scope = rememberCoroutineScope()

            // PillWrapRevealState/PerimeterRevealState.open()/close() need the real screen size
            // in px (they can't reach a BoxWithConstraints of their own from a plain suspend
            // function called outside the composable that renders them) plus the same base-
            // width/thickness fallback PillWrapReveal/PillPerimeterReveal's own render code falls
            // back to when a pill's origin rect hasn't been reported yet.
            val revealDensity = LocalDensity.current
            val revealConfiguration = LocalConfiguration.current
            val revealFullWidthPx = with(revealDensity) { revealConfiguration.screenWidthDp.dp.toPx() }
            val revealFullHeightPx = with(revealDensity) { revealConfiguration.screenHeightDp.dp.toPx() }
            val revealDefaultBaseWidthPx = with(revealDensity) { REVEAL_DEFAULT_BASE_WIDTH.toPx() }
            val revealMinThicknessPx = with(revealDensity) { REVEAL_MIN_THICKNESS.toPx() }

            LaunchedEffect(lastIntentExtra) {
                if (lastIntentExtra?.getBooleanExtra(McpServerService.EXTRA_OPEN_MCP, false) == true) {
                    screen = Screen.Mcp
                }
            }

            // "The pill becomes the page": the Files pill runs the screen's full perimeter edge
            // by edge, the same choreography every entrance into Files or the picker uses - see
            // PillPerimeterReveal.
            val filesWrap = remember { PerimeterRevealState() }
            var filesOrigin by remember { mutableStateOf(Rect.Zero) }
            val filesHue = remember { Azphalt.hues[Azphalt.hueOf("/")] }
            // Tracks whichever of open()/close() is currently driving filesWrap's Animatables, so
            // the other one can be cancelled before a new run starts instead of both racing the
            // same Animatable objects - a second run's animateTo()/snapTo() call silently steals
            // control from whichever run got there first, and if that first run was the one still
            // due to flip `active`/`screen` back, neither ever happens and the Files overlay is
            // stuck on screen. Paired with the active-guards below, which also stop a re-tap on an
            // already-open (or already-opening) Files pill from restarting the reveal from scratch.
            var filesJob by remember { mutableStateOf<Job?>(null) }
            fun openFiles() {
                if (filesWrap.active) return
                filesWrap.active = true
                filesWrap.origin = filesOrigin
                screen = Screen.Files
                filesJob = scope.launch {
                    filesWrap.open(revealFullWidthPx, revealFullHeightPx, revealDefaultBaseWidthPx, revealMinThicknessPx)
                }
            }
            fun closeFiles() {
                if (!filesWrap.active) return
                filesJob?.cancel()
                filesJob = scope.launch {
                    filesWrap.close(revealFullWidthPx, revealFullHeightPx, revealDefaultBaseWidthPx, revealMinThicknessPx)
                    // Only this run's own screen, not whatever the user may have navigated to
                    // since - closeFiles() can now only ever be reached while Files is still the
                    // active screen (see the excluded when-branch below), but this stays a
                    // conditional reset rather than unconditional out of caution.
                    if (screen == Screen.Files) screen = Screen.Terminal
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
            // Same open()/close() race guard as filesJob above - see its comment.
            var pathPickerJob by remember { mutableStateOf<Job?>(null) }

            fun closePathPicker() {
                if (!pathPickerState.active) return
                pathPickerJob?.cancel()
                pathPickerJob = scope.launch {
                    pathPickerState.close(revealFullWidthPx, revealFullHeightPx, revealDefaultBaseWidthPx, revealMinThicknessPx)
                }
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
                    // "job"/"fail" (Motion Sheet): a real progress bar for the one long-running
                    // task this app has real byte-count data for, replacing what used to be only
                    // a wall of scrolling "Downloaded: XMB / YMB" text lines.
                    val progress = JobProgressBarState()
                    bootstrapProgress = progress
                    var failed = false
                    built.engine.run("bootstrap", onNeedInput = { "" }).collect { output ->
                        firstUi.buffer = firstUi.buffer.mapIndexed { i, e ->
                            if (i == entryId) e.copy(output = output) else e
                        }
                        if (output.startsWith("Error")) {
                            failed = true
                        } else {
                            bootstrapDownloadFraction(output)?.let { progress.advanceTo(it) }
                        }
                    }
                    if (failed) progress.fail() else progress.complete()
                    bootstrapProgress = null
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
                    screen == Screen.AiSettings -> screen = aiSettingsCameFrom
                    screen == Screen.Mcp -> screen = Screen.Settings
                    screen == Screen.Environment -> screen = Screen.Settings
                    screen == Screen.History -> screen = Screen.Settings
                    screen == Screen.FullScreenApp -> {
                        fullScreenSession?.kill()
                        fullScreenSession = null
                        screen = Screen.Terminal
                    }
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
                        onOpenAiSettings = { aiSettingsCameFrom = Screen.Settings; screen = Screen.AiSettings },
                        onOpenEnvironment = { screen = Screen.Environment },
                        onOpenHistory = { historyQuery = ""; screen = Screen.History },
                        onBack = { screen = Screen.Terminal }
                    )

                    screen == Screen.Environment -> {
                        val bootstrap = ShellSession.bootstrapBashEnv(this@TerminalActivity, null)
                        EnvironmentScreen(
                            installed = bootstrap != null,
                            env = bootstrap?.second.orEmpty(),
                            onBack = { screen = Screen.Settings },
                            fullscreen = fullscreen
                        )
                    }

                    screen == Screen.History -> HistoryScreen(
                        entries = CommandHistoryStore.search(this@TerminalActivity, historyQuery),
                        query = historyQuery,
                        onQueryChange = { historyQuery = it },
                        onSelect = { command ->
                            sessions.firstOrNull { it.ui.id == activeSessionId }?.ui?.inputText = command
                            screen = Screen.Terminal
                        },
                        onBack = { screen = Screen.Settings },
                        fullscreen = fullscreen,
                        nowMillis = System.currentTimeMillis()
                    )

                    screen == Screen.AiSettings -> AiSettingsScreen(
                        fullscreen = fullscreen,
                        apiKey = aiApiKey,
                        onSave = { newKey ->
                            aiApiKey = newKey
                            AiSettings.setApiKey(this@TerminalActivity, newKey)
                            screen = aiSettingsCameFrom
                        },
                        onBack = { screen = aiSettingsCameFrom }
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
                        onOpenSettings = { aiSettingsCameFrom = Screen.Ai; screen = Screen.AiSettings },
                        onBack = { screen = Screen.Terminal }
                    )

                    screen == Screen.Azp -> AzpStoreScreen(
                        fullscreen = fullscreen,
                        results = azpResults,
                        busy = azpBusy,
                        installingId = azpInstallingId,
                        onSearch = { query, kind ->
                            azpBusy = true
                            azpSearchJob?.cancel()
                            azpSearchJob = scope.launch {
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

                    // Files/PathPicker's own reveal takes ~1-2s to run (see openFiles()/the
                    // wizard-pill dispatch above), and neither is a real screen change - `screen`
                    // itself only flips to Files synchronously, never for the picker. Without this
                    // branch, TerminalScreen (the catch-all below) would keep rendering - and
                    // staying fully interactive, since PillWrapReveal/PillPerimeterReveal draw no
                    // touch-blocking scrim of their own - underneath the growing reveal for that
                    // whole window, letting a wizard pill tapped through it start a real navigation
                    // (`screen = Screen.Ai`, etc.) while the reveal is still mid-flight and stack
                    // its own overlay on top of wherever that navigation lands.
                    screen == Screen.Files || filesWrap.active || pathPickerState.active -> {}

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
                                wizardId.startsWith(FileBrowser.WIZARD_PREFIX) -> if (!pathPickerState.active) {
                                    val crumbId = wizardId.removePrefix(FileBrowser.WIZARD_PREFIX)
                                    pathPickerRoot = session?.ui?.cwd?.takeIf { it.isNotBlank() }
                                        ?: CommandTree.pickerRoot(this@TerminalActivity).absolutePath
                                    pathPickerState.active = true
                                    pathPickerState.origin = crumbRects[crumbId] ?: Rect.Zero
                                    pathPickerJob = scope.launch {
                                        pathPickerState.open(
                                            revealFullWidthPx,
                                            revealFullHeightPx,
                                            revealDefaultBaseWidthPx,
                                            revealMinThicknessPx
                                        )
                                    }
                                }
                            }
                        },
                        onInterrupt = { sessionId ->
                            sessions.firstOrNull { it.ui.id == sessionId }?.engine?.interrupt()
                        },
                        onRun = onRun@{ sessionId, line, onOutput, onNeedInput, onExit, onStderr, onStyledOutput ->
                            val session = sessions.first { it.ui.id == sessionId }
                            // W5: recorded regardless of which path this takes below, or whether
                            // the command later succeeds - a shell's own history does the same.
                            CommandHistoryStore.record(this@TerminalActivity, session.ui.name, line, System.currentTimeMillis())
                            val fullScreenCommand = fullScreenCommandOf(line)
                            if (fullScreenCommand != null) {
                                if (!PtyPreference.isEnabled(this@TerminalActivity)) {
                                    onOutput(
                                        "$fullScreenCommand needs a real pseudoterminal - enable " +
                                            "\"Real pseudoterminal\" in Settings first"
                                    )
                                    onExit(1)
                                } else {
                                    val launched = FullScreenPtySession.launch(
                                        this@TerminalActivity, session.engine.workingDirectory, line
                                    )
                                    if (launched == null) {
                                        onOutput("no Termux bootstrap installed - can't run $fullScreenCommand")
                                        onExit(1)
                                    } else {
                                        fullScreenSession = launched
                                        screen = Screen.FullScreenApp
                                        onOutput("opening $fullScreenCommand full-screen…")
                                        onExit(0)
                                    }
                                }
                                return@onRun
                            }
                            session.engine.run(line, onNeedInput, onExit, onStderr, onStyledOutput)
                                .collect { output -> onOutput(output) }
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

                    screen == Screen.FullScreenApp && fullScreenSession != null -> FullScreenTerminalScreen(
                        holder = fullScreenSession!!,
                        onExit = {
                            fullScreenSession?.kill()
                            fullScreenSession = null
                            screen = Screen.Terminal
                        }
                    )

                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading…")
                    }
                }

                bootstrapProgress?.let { progress ->
                    JobProgressBar(
                        state = progress,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
                    PillPerimeterReveal(state = filesWrap, hue = filesHue) {
                        FilesScreen(
                            fullscreen = fullscreen,
                            nowMillis = System.currentTimeMillis(),
                            listDir = { path -> vfsListDir(path) },
                            search = { query -> vfsSearch(query) },
                            storageStats = { vfsStorageStats() },
                            previewFile = { path -> withContext(Dispatchers.IO) { previewFile(this@TerminalActivity, path) } },
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
