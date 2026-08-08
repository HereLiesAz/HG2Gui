package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.Eyebrow
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.MenuNode
import com.hereliesaz.hg2gui.ui.menu.PillMenu

private val PageYellow = Brush.linearGradient(
    0f to Color(0xFFE8C81E), 0.5f to Color(0xFFD9B615), 1f to Color(0xFFE8C81E)
)

/*
 * The guide is a read-through of the exact tree PillMenu itself runs on - picking a command
 * here writes it into the input instead of running it, so browsing and picking are the same
 * interaction the terminal already teaches, not a second UI to learn. Staying open across
 * picks (no auto-close) mirrors how the terminal's own PillMenu behaves: nothing closes it but
 * the user.
 */
@Composable
fun CommandGuideScreen(
    tree: List<MenuNode>,
    fullscreen: Boolean,
    onCommandSelected: (List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
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

        Eyebrow("Guide")

        Text(
            "Pick a command to drop it into the input — nothing here runs on its own.",
            color = Azphalt.Ink.copy(alpha = .6f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
        )

        PillMenu(
            roots = tree,
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp),
            // The guide only ever writes a pick into the input - see the doc comment above -
            // so isTerminal (the auto-run signal) is irrelevant here.
            onRun = { picked, _ -> onCommandSelected(picked) }
        )
    }
}
