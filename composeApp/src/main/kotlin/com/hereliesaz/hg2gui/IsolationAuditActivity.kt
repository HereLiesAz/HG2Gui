package com.hereliesaz.hg2gui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.hereliesaz.hg2gui.ai.AiClient
import com.hereliesaz.hg2gui.managers.AiSettings
import com.hereliesaz.hg2gui.terminal.PackageIsolation
import com.hereliesaz.hg2gui.terminal.PackageLifecycleStore
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltHeading
import com.hereliesaz.hg2gui.ui.ConfirmDialog
import com.hereliesaz.hg2gui.ui.FONT_SCALE_MAX_PERCENT
import com.hereliesaz.hg2gui.ui.FONT_SCALE_MIN_PERCENT
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.applyAzphaltGround
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import java.io.File
import java.nio.file.Files
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The Text-size and fullscreen preferences live in TerminalActivity's SharedPreferences; these
// names must stay in step with its PREFS_NAME / PREF_FONT_SCALE_PERCENT / PREF_FULLSCREEN so the
// Editor and the Audit follow the same setting the user picked in Settings.
private const val UI_PREFS_NAME = "hg2gui_prefs"
private const val UI_PREF_FONT_SCALE_PERCENT = "font_scale_percent"
private const val UI_PREF_FULLSCREEN = "fullscreen"
private const val DEFAULT_FONT_SCALE_PERCENT = 100

/** The Text-size setting as a density multiplier, clamped to the range Settings offers (50%..200%). */
internal fun uiScaleFromPercent(percent: Int): Float =
    percent.coerceIn(FONT_SCALE_MIN_PERCENT, FONT_SCALE_MAX_PERCENT) / 100f

/** The user's Text-size setting (1 = 100%), for `HG2GuiTheme(scale = ...)`. */
internal fun Context.readUiScale(): Float =
    uiScaleFromPercent(
        getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(UI_PREF_FONT_SCALE_PERCENT, DEFAULT_FONT_SCALE_PERCENT)
    )

/** The user's immersive ("fullscreen") preference. */
internal fun Context.readFullscreenPref(): Boolean =
    getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE).getBoolean(UI_PREF_FULLSCREEN, false)

/** Hides the system bars (swipe to peek) when [fullscreen], the same way the terminal does. */
internal fun ComponentActivity.applyFullscreenPreference(fullscreen: Boolean) {
    if (!fullscreen) return
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}

class IsolationAuditActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.applyAzphaltGround()
        super.onCreate(savedInstanceState)
        val fullscreen = readFullscreenPref()
        val scale = readUiScale()
        setContent {
            HG2GuiTheme(scale = scale) {
                IsolationAuditScreen(fullscreen = fullscreen, onBack = ::finish)
            }
        }
        applyFullscreenPreference(fullscreen)
    }
}

internal data class IsolationAuditEntry(
    val packageName: String,
    val managerLabel: String,
    val version: String,
    val savedAudit: PackageIsolation.SavedAudit?,
    val fileCount: Int,
    val bytes: Long
)

@Composable
private fun IsolationAuditScreen(fullscreen: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf<List<IsolationAuditEntry>?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableIntStateOf(0) }
    // The Keystore read happens off the main thread, once, rather than on every recomposition.
    val apiKey by produceState<String?>(null, context) {
        value = withContext(Dispatchers.IO) { AiSettings.apiKey(context) }
    }

    LaunchedEffect(refreshToken) {
        refreshing = true
        entries = withContext(Dispatchers.IO) {
            PackageLifecycleStore.installed(context)
                .filter { it.isolated }
                .map { pkg ->
                    val root = PackageIsolation.root(context, pkg)
                    val files = runCatching {
                        root.walkTopDown()
                            .onEnter { !Files.isSymbolicLink(it.toPath()) }
                            .filter(File::isFile)
                            .toList()
                    }.getOrDefault(emptyList())
                    IsolationAuditEntry(
                        packageName = pkg.name,
                        managerLabel = pkg.managerLabel,
                        version = pkg.version,
                        savedAudit = PackageIsolation.latestAudit(context, pkg),
                        fileCount = files.size,
                        bytes = files.sumOf { it.length() }
                    )
                }
        }
        refreshing = false
    }

    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)
    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen, ime = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AzphaltChip("‹ BACK", onBack, contentDescription = "Back")
            AzphaltChip(
                label = if (refreshing) "REFRESHING…" else "REFRESH",
                onClick = { refreshToken += 1 },
                enabled = !refreshing,
                contentDescription = if (refreshing) "Refreshing audits" else "Refresh audits"
            )
        }
        AzphaltHeading("Isolation audit")
        Text(
            "Latest completed isolated run per package: persisted sandbox diff plus observed runtime activity. " +
                "Runtime observation is sampled and is not syscall-complete.",
            color = muted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        when (val current = entries) {
            null -> Text("READING AUDITS…", color = muted, fontSize = 12.sp, modifier = Modifier.liveRegionPolite())
            emptyList<IsolationAuditEntry>() -> Text("No isolated packages.", color = onPage, modifier = Modifier.liveRegionPolite())
            else -> current.forEach { entry -> AuditCard(entry, apiKey) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AuditCard(entry: IsolationAuditEntry, apiKey: String?) {
    val scope = rememberCoroutineScope()
    val stamp = entry.savedAudit?.timestampMillis
    var aiResult by rememberSaveable(entry.packageName, stamp) { mutableStateOf<String?>(null) }
    var aiBusy by remember(entry.packageName, stamp) { mutableStateOf(false) }
    var confirmSend by remember(entry.packageName, stamp) { mutableStateOf(false) }
    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)

    Column(
        Modifier.fillMaxWidth().background(Azphalt.Ink.copy(alpha = .09f), RoundedCornerShape(22.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(entry.packageName, color = onPage, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text(
            "${entry.managerLabel} · ${entry.version.ifBlank { "installed" }} · " +
                "${entry.fileCount} sandbox files · ${formatBytes(entry.bytes)}",
            color = muted,
            fontSize = 11.sp
        )

        val saved = entry.savedAudit
        if (saved == null) {
            Text("No completed-run audit yet. Run this isolated package, then refresh.", color = muted, fontSize = 12.sp)
        } else {
            SectionLabel("CAPTURED ${formatTimestamp(saved.timestampMillis)}")
            SandboxDiff(entry.packageName, saved.audit)
            RuntimeTelemetry(entry.packageName, saved.audit.telemetry)

            if (!apiKey.isNullOrBlank()) {
                AzphaltChip(
                    label = if (aiBusy) "ASKING AI…" else "ASK AI · SEND SUMMARY",
                    onClick = { confirmSend = true },
                    enabled = !aiBusy,
                    contentDescription = if (aiBusy) "Asking the AI" else "Ask the AI to interpret this audit; shows what will be sent first"
                )
            }
            aiResult?.let { result ->
                SectionLabel("AI INTERPRETATION")
                Text(result, color = onPage, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.liveRegionPolite())
            }
        }
    }

    if (confirmSend && !apiKey.isNullOrBlank()) {
        ConfirmDialog(
            title = "SEND AUDIT SUMMARY TO AI?",
            message = aiDisclosureText(),
            confirmLabel = "SEND",
            onConfirm = {
                confirmSend = false
                if (aiBusy) return@ConfirmDialog
                aiBusy = true
                scope.launch {
                    aiResult = runCatching {
                        AiClient.ask(
                            apiKey = apiKey,
                            // Placeholder, not the sandbox's real absolute path - the device's
                            // on-disk layout (user id, package dir) isn't the AI's business.
                            cwd = "/sandbox",
                            question = auditInterpretationPrompt(entry)
                        ).text
                    }.getOrElse { "AI interpretation failed: ${it.message ?: it.javaClass.simpleName}" }
                    aiBusy = false
                }
            },
            onDismiss = { confirmSend = false }
        )
    }
}

/** A small caps label above a block of audit rows. Muted, but legible (see [StatusKind.Muted]). */
@Composable
private fun SectionLabel(text: String) {
    Text(text, color = statusColor(StatusKind.Muted), fontWeight = FontWeight.Black, fontSize = 11.sp)
}

@Composable
private fun SandboxDiff(packageName: String, audit: PackageIsolation.Audit) {
    SectionLabel("SANDBOX DIFF · +${audit.created.size}  ~${audit.modified.size}  −${audit.deleted.size}")
    if (audit.changedCount == 0) {
        Text("No sandbox filesystem changes observed.", color = statusColor(StatusKind.Muted), fontSize = 12.sp)
        return
    }
    AuditRows("$packageName/created", "+", audit.created)
    AuditRows("$packageName/modified", "~", audit.modified)
    AuditRows("$packageName/deleted", "−", audit.deleted)
}

@Composable
private fun RuntimeTelemetry(packageName: String, telemetry: List<String>) {
    SectionLabel("RUNTIME · ${telemetry.size} OBSERVATIONS")
    if (telemetry.isEmpty()) {
        Text("No sampled runtime activity recorded.", color = statusColor(StatusKind.Muted), fontSize = 12.sp)
        return
    }
    val groups = telemetry.groupBy(::telemetryKind)
    listOf("AUTHORITY", "NETWORK", "PROCESS", "FILES", "OTHER").forEach { kind ->
        val rows = groups[kind].orEmpty()
        if (rows.isNotEmpty()) {
            SectionLabel(kind)
            ExpandableRows("$packageName/telemetry/$kind", rows.map(::telemetryDetail))
        }
    }
}

@Composable
private fun AuditRows(stateKey: String, marker: String, paths: List<String>) {
    ExpandableRows(stateKey, paths.map { "$marker $it" })
}

/**
 * [rows] as text, the first [MAX_ROWS_PER_SECTION] at first and [MAX_ROWS_PER_SECTION] more per tap
 * of the "… N MORE" chip (a sandbox can hold tens of thousands of files, so it opens in steps rather
 * than composing them all at once). Once past the first page a SHOW LESS chip folds it back.
 * [stateKey] must be unique per list on the screen; it survives recreation.
 */
@Composable
private fun ExpandableRows(stateKey: String, rows: List<String>) {
    var shown by rememberSaveable(stateKey) { mutableStateOf(MAX_ROWS_PER_SECTION) }
    val onPage = Azphalt.currentGround.onPage
    rows.take(shown).forEach { row ->
        Text(row, color = onPage, fontSize = 11.sp, lineHeight = 15.sp)
    }
    val remaining = rows.size - shown
    if (remaining > 0) {
        AzphaltChip(
            label = "… $remaining MORE",
            onClick = { shown = nextVisibleCount(shown, rows.size, MAX_ROWS_PER_SECTION) },
            contentDescription = "Show more rows, $remaining remaining"
        )
    } else if (shown > MAX_ROWS_PER_SECTION) {
        AzphaltChip("SHOW LESS", { shown = MAX_ROWS_PER_SECTION }, contentDescription = "Show fewer rows")
    }
}

/** How many rows to show after one "more" tap: [current] plus [step], never past [total]. */
internal fun nextVisibleCount(current: Int, total: Int, step: Int): Int =
    minOf(total, current + step).coerceAtLeast(current)

/**
 * What the confirmation shows before the audit summary leaves the device. It describes exactly what
 * [auditInterpretationPrompt] builds - keep the two in step (the caps are the same constant).
 */
internal fun aiDisclosureText(): String =
    "This sends a text summary of this package's latest run to Anthropic, using your API key: " +
        "the package name and version; the sandbox file count and size; how many files were created, " +
        "modified and deleted; the file names (never the folders) of up to $AI_MAX_ROWS in each of those groups, " +
        "leaving out any whose path mentions a password, secret, token, key or similar; how many runtime " +
        "events of each kind were observed; and the protocol and remote port (never the address) of up to " +
        "$AI_MAX_ROWS network connections. Full paths, command lines and addresses stay on this device."

internal fun auditInterpretationPrompt(entry: IsolationAuditEntry): String {
    val audit = entry.savedAudit?.audit ?: return "No audit exists."
    fun safeRows(rows: List<String>): List<String> = rows
        .filterNot { SENSITIVE_WORDS.containsMatchIn(it) }
        .take(AI_MAX_ROWS)
    return buildString {
        appendLine("Interpret this HG2Gui isolated-package audit. Give a concise risk/behavior summary. Do not propose elevated commands unless explicitly necessary, and remember the runtime sampler is best-effort rather than syscall-complete.")
        appendLine("Package: ${entry.packageName} ${entry.version}")
        appendLine("Sandbox: ${entry.fileCount} files, ${formatBytes(entry.bytes)}")
        appendLine("Created ${audit.created.size}, modified ${audit.modified.size}, deleted ${audit.deleted.size}.")
        safeRows(audit.created).forEach { appendLine("created: ${it.substringAfterLast('/')}") }
        safeRows(audit.modified).forEach { appendLine("modified: ${it.substringAfterLast('/')}") }
        safeRows(audit.deleted).forEach { appendLine("deleted: ${it.substringAfterLast('/')}") }
        // Raw telemetry rows carry full command lines, file paths and peer addresses - only
        // per-category counts, plus network peers with the address redacted, leave the device.
        audit.telemetry.groupingBy(::telemetryKind).eachCount().toSortedMap().forEach { (kind, count) ->
            appendLine("observed $kind events: $count")
        }
        audit.telemetry.filter { telemetryKind(it) == "NETWORK" }
            .mapNotNull(::redactedNetworkRow)
            .distinct()
            .take(AI_MAX_ROWS)
            .forEach { appendLine("network: $it") }
    }
}

internal fun telemetryKind(line: String): String = when {
    line.startsWith("authority-attempt:") -> "AUTHORITY"
    line.startsWith("network:") -> "NETWORK"
    line.startsWith("process:") || line.startsWith("root-process:") -> "PROCESS"
    line.startsWith("file-") -> "FILES"
    else -> "OTHER"
}

/**
 * `network:<proto>:<local>:<remote>:<state>:<inode>` (from /proc/net, hex `ADDR:PORT` pairs)
 * reduced to `<proto> remote port <n>` - the peer's address is withheld, only its port (which
 * says what kind of service was contacted) is kept. Null for a row that doesn't parse.
 */
internal fun redactedNetworkRow(line: String): String? {
    val fields = line.removePrefix("network:").split(':')
    val proto = fields.firstOrNull()?.takeIf { it.matches(Regex("[a-z0-9]+")) }
    val remotePort = fields.getOrNull(4)?.toIntOrNull(16)
    return if (proto == null || remotePort == null) null else "$proto remote port $remotePort"
}

private fun telemetryDetail(line: String): String = line.substringAfter(':', line)

private fun formatTimestamp(timestampMillis: Long): String = if (timestampMillis <= 0L) {
    "UNKNOWN TIME"
} else {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(timestampMillis))
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KiB".format(bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    else -> "%.1f GiB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

private val SENSITIVE_WORDS = Regex("(?i)(password|passwd|secret|token|api[_-]?key|authorization|bearer|credential|private[_-]?key)")
private const val MAX_ROWS_PER_SECTION = 40

/** Cap on each list of rows that goes to the AI - and that [aiDisclosureText] promises. */
internal const val AI_MAX_ROWS = 24
