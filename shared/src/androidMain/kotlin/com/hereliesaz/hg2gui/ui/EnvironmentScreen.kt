package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/**
 * W4 (docs/HG2Gui Termux Coverage.dc.html): "no environment/PATH visibility or editing" - a
 * read-only view of the same bootstrap env every command in the app actually runs under
 * (ShellSession.bootstrapBashEnv - the same map forAndroid() and FullScreenPtySession.launch both
 * build a session from), so at least the "visibility" half of that gap has an honest answer.
 *
 * Deliberately read-only: making this editable would mean feeding user-edited values back into
 * ShellSession's own env-construction path, which per-command already reconstructs from
 * DistroManager/PtyPreference rather than from any mutable app-level store - wiring an edit UI to
 * that safely is a separate, larger change than closing the visibility gap this screen closes.
 */
@Composable
fun EnvironmentScreen(
    installed: Boolean,
    env: Map<String, String>,
    onBack: () -> Unit,
    fullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
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

        Eyebrow("Environment")

        if (!installed) {
            Text(
                "No Termux bootstrap is installed, so there is no environment for a command to " +
                    "run under yet - install it first (the pill menu's own setup step).",
                color = Azphalt.currentGround.onPage.copy(alpha = .7f),
                fontSize = 12.sp, lineHeight = 18.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp)
            )
            return@Column
        }

        Text(
            "What every command in this app actually runs with - the same env ShellSession " +
                "builds fresh for each bootstrap-tier session.",
            color = Azphalt.currentGround.onPage.copy(alpha = .55f),
            fontSize = 11.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp)
        )

        val pathDirs = env["PATH"]?.split(":")?.filter { it.isNotBlank() }.orEmpty()
        val otherVars = env.entries.filter { it.key != "PATH" }.sortedBy { it.key }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    "PATH (${pathDirs.size})",
                    color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em,
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                )
            }
            items(pathDirs) { dir -> EnvRow(label = null, value = dir) }

            item {
                Text(
                    "VARIABLES (${otherVars.size})",
                    color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.18.em,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
                )
            }
            items(otherVars) { (key, value) -> EnvRow(label = key, value = value) }
        }
    }
}

@Composable
private fun EnvRow(label: String?, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (label != null) {
            Text(
                label, color = Azphalt.currentGround.onPage,
                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.01.em
            )
        }
        Text(
            value, color = Azphalt.currentGround.onPage.copy(alpha = .7f),
            fontSize = 12.sp, lineHeight = 16.sp,
            modifier = Modifier.padding(top = if (label != null) 2.dp else 0.dp)
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .12f)))
}
