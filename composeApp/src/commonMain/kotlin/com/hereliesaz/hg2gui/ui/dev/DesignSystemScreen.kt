package com.hereliesaz.hg2gui.ui.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.theme.AzphaltColors

/*
 * Dev-only specimen sheet: capsule anatomy, palette, type scale, and a jump list to every real
 * surface in the app. Not a shipped user screen — mirrors what the HG2Gui style-guide / surfaces
 * DCs show in the design tool, kept here as a single reference rather than four near-duplicate
 * screens. Gate this behind a debug build flag before shipping.
 */
@Composable
fun DesignSystemScreen(surfaces: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().background(AzphaltColors.page).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item { Eyebrow("01 — Palette") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AzphaltColors.hues.forEach { hue ->
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(50)).background(hue))
                }
            }
        }
        item { Eyebrow("02 — Capsule") }
        item {
            Row(Modifier.clip(RoundedCornerShape(50)).background(AzphaltColors.hues[2])
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
                Text("EXAMPLE", color = AzphaltColors.White, fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
            }
        }
        item { Eyebrow("03 — Type") }
        item { Text("DISPLAY 900", color = AzphaltColors.Ink, fontSize = 40.sp, fontWeight = FontWeight.Black) }
        item { Text("Body 600 sentence case, the only lowercase in the system.",
            color = AzphaltColors.Ink.copy(alpha = .78f), fontSize = 15.sp) }
        item { Eyebrow("04 — Surfaces") }
        items(surfaces) { (label, go) ->
            Text(label.uppercase(), color = AzphaltColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable(onClick = go).padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, color = AzphaltColors.Ink.copy(alpha = .72f), fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = 0.28.em)
}
