package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.hereliesaz.hg2gui.terminal.PackageIsolation
import com.hereliesaz.hg2gui.terminal.PackageLifecycleStore
import com.hereliesaz.hg2gui.ui.HG2GuiTheme
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IsolationAuditActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HG2GuiTheme {
                IsolationAuditScreen(onBack = ::finish)
            }
        }
    }
}

private data class IsolationAuditEntry(
    val packageName: String,
    val managerLabel: String,
    val version: String,
    val savedAudit: PackageIsolation.SavedAudit?,
    val fileCount: Int,
    val bytes: Long
)

@Composable
private fun IsolationAuditScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var entries by remember { mutableStateOf<List<IsolationAuditEntry>?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshToken) {
        entries = withContext(Dispatchers.IO) {
            PackageLifecycleStore.installed(context)
                .filter { it.isolated }
                .map { pkg ->
                    val root = PackageIsolation.root(context, pkg)
                    val files = runCatching {
                        root.walkTopDown().filter { it.isFile }.toList()
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
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuditAction("‹ BACK", onBack)
            AuditAction("REFRESH") { refreshToken += 1 }
        }
        Text("ISOLATION AUDIT", color = Azphalt.Ink, fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text(
            "Latest completed isolated run per package: persisted sandbox diff plus observed runtime activity. " +
                "Runtime observation is sampled and is not syscall-complete.",
            color = Azphalt.Ink.copy(alpha = .62f),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        when (val current = entries) {
            null -> Text("READING AUDITS…", color = Azphalt.Ink.copy(alpha = .55f), fontSize = 10.sp)
            emptyList<IsolationAuditEntry>() -> {
                Text("No isolated packages.", color = Azphalt.Ink.copy(alpha = .6f))
            }
            else -> current.forEach { entry -> AuditCard(entry) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AuditAction(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Azphalt.Yellow,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .background(Azphalt.Ink, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AuditCard(entry: IsolationAuditEntry) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Azphalt.Ink.copy(alpha = .09f), RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(entry.packageName, color = Azphalt.Ink, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text(
            "${entry.managerLabel} · ${entry.version.ifBlank { "installed" }} · " +
                "${entry.fileCount} sandbox files · ${formatBytes(entry.bytes)}",
            color = Azphalt.Ink.copy(alpha = .55f),
            fontSize = 9.sp
        )

        val saved = entry.savedAudit
        if (saved == null) {
            Text(
                "No completed-run audit yet. Run this isolated package, then refresh.",
                color = Azphalt.Ink.copy(alpha = .58f),
                fontSize = 11.sp
            )
        } else {
            Text(
                "CAPTURED ${formatTimestamp(saved.timestampMillis)}",
                color = Azphalt.Ink.copy(alpha = .5f),
                fontWeight = FontWeight.Black,
                fontSize = 9.sp
            )
            SandboxDiff(saved.audit)
            RuntimeTelemetry(saved.audit.telemetry)
        }
    }
}

@Composable
private fun SandboxDiff(audit: PackageIsolation.Audit) {
    Text(
        "SANDBOX DIFF · +${audit.created.size}  ~${audit.modified.size}  −${audit.deleted.size}",
        color = Azphalt.Ink.copy(alpha = .5f),
        fontWeight = FontWeight.Black,
        fontSize = 9.sp
    )
    if (audit.changedCount == 0) {
        Text("No sandbox filesystem changes observed.", color = Azphalt.Ink.copy(alpha = .62f), fontSize = 10.sp)
        return
    }
    AuditRows("+", audit.created)
    AuditRows("~", audit.modified)
    AuditRows("−", audit.deleted)
}

@Composable
private fun RuntimeTelemetry(telemetry: List<String>) {
    Text(
        "RUNTIME · ${telemetry.size} OBSERVATIONS",
        color = Azphalt.Ink.copy(alpha = .5f),
        fontWeight = FontWeight.Black,
        fontSize = 9.sp
    )
    if (telemetry.isEmpty()) {
        Text("No sampled runtime activity recorded.", color = Azphalt.Ink.copy(alpha = .62f), fontSize = 10.sp)
        return
    }
    val groups = telemetry.groupBy(::telemetryKind)
    listOf("AUTHORITY", "NETWORK", "PROCESS", "FILES", "OTHER").forEach { kind ->
        val rows = groups[kind].orEmpty()
        if (rows.isNotEmpty()) {
            Text(kind, color = Azphalt.Ink.copy(alpha = .48f), fontWeight = FontWeight.Black, fontSize = 9.sp)
            rows.take(MAX_ROWS_PER_SECTION).forEach { row ->
                Text(
                    telemetryDetail(row),
                    color = Azphalt.Ink.copy(alpha = .82f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
            if (rows.size > MAX_ROWS_PER_SECTION) {
                Text(
                    "… ${rows.size - MAX_ROWS_PER_SECTION} more",
                    color = Azphalt.Ink.copy(alpha = .52f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun AuditRows(marker: String, paths: List<String>) {
    paths.take(MAX_ROWS_PER_SECTION).forEach { path ->
        Text("$marker $path", color = Azphalt.Ink.copy(alpha = .82f), fontSize = 10.sp, lineHeight = 14.sp)
    }
    if (paths.size > MAX_ROWS_PER_SECTION) {
        Text(
            "… ${paths.size - MAX_ROWS_PER_SECTION} more",
            color = Azphalt.Ink.copy(alpha = .52f),
            fontSize = 9.sp
        )
    }
}

private fun telemetryKind(line: String): String = when {
    line.startsWith("authority-attempt:") -> "AUTHORITY"
    line.startsWith("network:") -> "NETWORK"
    line.startsWith("process:") || line.startsWith("root-process:") -> "PROCESS"
    line.startsWith("file-") -> "FILES"
    else -> "OTHER"
}

private fun telemetryDetail(line: String): String = line.substringAfter(':', line)

private fun formatTimestamp(timestampMillis: Long): String = if (timestampMillis <= 0L) {
    "UNKNOWN TIME"
} else {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(timestampMillis))
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KiB".format(bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    else -> "%.1f GiB".format(bytes / (1024.0 * 1024.0 * 1024.0))
}

private const val MAX_ROWS_PER_SECTION = 40
