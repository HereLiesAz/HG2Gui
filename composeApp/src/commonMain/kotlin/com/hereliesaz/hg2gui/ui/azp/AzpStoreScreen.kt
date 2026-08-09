package com.hereliesaz.hg2gui.ui.azp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt

/** A search result row, or an already-installed package - shared shape for the list. */
data class AzpListing(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val version: String,
    val kind: String,
    val installed: Boolean,
)

private val KINDS = listOf("all", "skill", "mcp", "code", "pack", "asset", "app")

private val PageYellow = Brush.linearGradient(
    0f to Color(0xFFE8C81E), 0.5f to Color(0xFFD9B615), 1f to Color(0xFFE8C81E)
)

/**
 * Browses the azphalt registry (spec/repository-api.md) - the same `pkg`/`apt` idiom, but for
 * `.azp` extensions: search, filter by kind, install. HG2Gui has no `.azp` execution runtime, so
 * "install" downloads + unpacks the archive; only `kind:"skill"` packages do anything further
 * (their SKILL.md content feeds the AI chat's system prompt - see AiClient/AzpLibrary). Every
 * other kind is downloaded for the user's own use elsewhere, same as `apt download`.
 */
@Composable
fun AzpStoreScreen(
    fullscreen: Boolean,
    results: List<AzpListing>,
    busy: Boolean,
    installingId: String?,
    onSearch: (query: String, kind: String) -> Unit,
    onInstall: (AzpListing) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("all") }

    Column(
        Modifier
            .fillMaxSize()
            .background(PageYellow)
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    "‹ BACK", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        Eyebrow("STORE")

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KINDS.forEach { k ->
                val selected = k == kind
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (selected) Azphalt.Ink else Azphalt.Ink.copy(alpha = .12f))
                        .clickable {
                            kind = k
                            onSearch(query.trim(), k)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        k.uppercase(),
                        color = if (selected) Azphalt.Yellow else Azphalt.Ink,
                        fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Azphalt.Yellow, fontSize = 12.sp),
                    cursorBrush = SolidColor(Azphalt.Yellow),
                    singleLine = true,
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query.trim(), kind) })
                )
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (!busy) Azphalt.hues[6] else Azphalt.hues[6].copy(alpha = .4f))
                    .clickable(enabled = !busy) { onSearch(query.trim(), kind) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    if (busy) "…" else "SEARCH",
                    style = MaterialTheme.typography.titleMedium.copy(color = Azphalt.White, fontSize = 9.sp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(results) { listing ->
                AzpRow(listing, installingId == listing.id, onInstall)
            }
        }
    }
}

@Composable
private fun AzpRow(listing: AzpListing, installing: Boolean, onInstall: (AzpListing) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Azphalt.Ink.copy(alpha = .05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    listing.name,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Azphalt.Ink, fontWeight = FontWeight.ExtraBold)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    listing.kind.uppercase(),
                    color = Azphalt.Ink.copy(alpha = .5f),
                    fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
            if (listing.description.isNotBlank()) {
                Text(
                    listing.description,
                    color = Azphalt.Ink.copy(alpha = .65f), fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Text(
                "${listing.author} · v${listing.version}",
                color = Azphalt.Ink.copy(alpha = .45f), fontSize = 9.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(if (listing.installed) Azphalt.Ink.copy(alpha = .3f) else Azphalt.Ink)
                .clickable(enabled = !listing.installed && !installing) { onInstall(listing) }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                when {
                    listing.installed -> "INSTALLED"
                    installing -> "…"
                    else -> "INSTALL"
                },
                color = Azphalt.Yellow,
                fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
            )
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 9.dp)
    )
}
