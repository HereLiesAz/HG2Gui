package com.hereliesaz.hg2gui.ui.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt

/*
 * The graphical file explorer over VfsManager's sandbox. Same page, same capsule primitive,
 * same "no icons, no borders, no shadows" rules as the rest of Azphalt — folders read as
 * folders because of a trailing "/" and a hue, not a glyph. Drill-down is breadcrumb-based
 * rather than nested indentation, so depth never grows the screen the way PillMenu's cascade
 * never does either.
 */

private val PageYellow = Brush.linearGradient(
    0f to Color(0xFFE8C81E), 0.5f to Color(0xFFD9B615), 1f to Color(0xFFE8C81E)
)

data class VfsEntry(val name: String, val isDirectory: Boolean, val sizeBytes: Long)

@Composable
fun FilesScreen(
    path: String,
    entries: List<VfsEntry>,
    onNavigate: (String) -> Unit,
    onOpenFile: (String) -> Unit,
    onCreateFolder: (String) -> Unit,
    onCreateFile: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var creating by remember(path) { mutableStateOf<CreateMode?>(null) }
    var nameInput by remember(path) { mutableStateOf("") }

    Column(modifier.fillMaxSize().background(PageYellow)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "FILES",
                style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Ink, fontSize = 14.sp)
            )
            Text(
                "CLOSE",
                style = MaterialTheme.typography.labelSmall.copy(color = Azphalt.Ink.copy(alpha = .6f), fontSize = 9.sp),
                modifier = Modifier.clickable(onClick = onBack)
            )
        }

        Breadcrumbs(path = path, onNavigate = onNavigate)

        if (creating != null) {
            CreatePrompt(
                mode = creating!!,
                name = nameInput,
                onNameChange = { nameInput = it },
                onConfirm = {
                    if (nameInput.isNotBlank()) {
                        when (creating) {
                            CreateMode.FOLDER -> onCreateFolder(nameInput.trim())
                            CreateMode.FILE -> onCreateFile(nameInput.trim())
                            null -> {}
                        }
                    }
                    creating = null
                    nameInput = ""
                },
                onCancel = { creating = null; nameInput = "" }
            )
        }

        if (entries.isEmpty()) {
            Text(
                "EMPTY",
                style = MaterialTheme.typography.labelSmall.copy(color = Azphalt.Ink.copy(alpha = .4f), fontSize = 10.sp),
                modifier = Modifier.padding(start = 20.dp, top = 20.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                EntryRow(
                    entry = entry,
                    onClick = { if (entry.isDirectory) onNavigate(entry.name) else onOpenFile(entry.name) },
                    onDelete = { onDelete(entry.name) }
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionPill("+ FOLDER") { creating = CreateMode.FOLDER; nameInput = "" }
            ActionPill("+ FILE") { creating = CreateMode.FILE; nameInput = "" }
        }
    }
}

private enum class CreateMode { FOLDER, FILE }

@Composable
private fun Breadcrumbs(path: String, onNavigate: (String) -> Unit) {
    val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BreadcrumbLabel("/", segments.isEmpty()) { onNavigate("/") }
        var acc = ""
        segments.forEach { seg ->
            acc += "/$seg"
            val target = acc
            Text(
                "/",
                style = MaterialTheme.typography.labelSmall.copy(color = Azphalt.Ink.copy(alpha = .35f), fontSize = 10.sp)
            )
            BreadcrumbLabel(seg, target == "/${segments.joinToString("/")}") { onNavigate(target) }
        }
    }
}

@Composable
private fun BreadcrumbLabel(text: String, current: Boolean, onClick: () -> Unit) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = if (current) Azphalt.Ink else Azphalt.Ink.copy(alpha = .5f),
            fontSize = 10.sp,
            fontWeight = if (current) FontWeight.Black else FontWeight.Normal
        ),
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EntryRow(entry: VfsEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val hue = if (entry.isDirectory) Azphalt.hues[Azphalt.hueOf(entry.name)] else Azphalt.Ink.copy(alpha = .14f)
    val fg = if (entry.isDirectory) Azphalt.White else Azphalt.Ink

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(hue)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            (entry.name + if (entry.isDirectory) "/" else "").uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.09.em
            ),
            maxLines = 1
        )
        Text(
            "×",
            style = MaterialTheme.typography.titleMedium.copy(color = fg.copy(alpha = .7f), fontSize = 13.sp),
            modifier = Modifier.clickable(onClick = onDelete).padding(start = 8.dp)
        )
    }
}

@Composable
private fun ActionPill(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink.copy(alpha = .14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Azphalt.Ink,
                fontSize = 9.sp,
                letterSpacing = 0.06.em
            )
        )
    }
}

@Composable
private fun CreatePrompt(
    mode: CreateMode,
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .heightIn(min = 32.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (mode == CreateMode.FOLDER) "NEW FOLDER" else "NEW FILE",
            style = MaterialTheme.typography.labelSmall.copy(color = Azphalt.Yellow.copy(alpha = .7f), fontSize = 8.sp)
        )
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Azphalt.Yellow, fontSize = 12.sp),
            cursorBrush = SolidColor(Azphalt.Yellow),
            singleLine = true
        )
        Text(
            "OK",
            style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Yellow, fontSize = 10.sp),
            modifier = Modifier.clickable(onClick = onConfirm).padding(horizontal = 8.dp, vertical = 6.dp)
        )
        Text(
            "X",
            style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.Yellow.copy(alpha = .6f), fontSize = 10.sp),
            modifier = Modifier.clickable(onClick = onCancel).padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
