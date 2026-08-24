package com.hereliesaz.hg2gui.ui

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
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush

/** Bounds match the range the reporter cared about testing: half size to double. */
const val FONT_SCALE_MIN_PERCENT = 50
const val FONT_SCALE_MAX_PERCENT = 200
private const val FONT_SCALE_STEP_PERCENT = 10

@Composable
fun SettingsScreen(
    fullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    fontScalePercent: Int,
    onFontScalePercentChange: (Int) -> Unit,
    usePty: Boolean,
    onUsePtyChange: (Boolean) -> Unit,
    onOpenMcpServer: () -> Unit,
    onOpenAiSettings: () -> Unit,
    onOpenEnvironment: () -> Unit,
    onOpenHistory: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
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

        Eyebrow("Settings")

        SettingRow(
            title = "Fullscreen",
            description = if (fullscreen)
                "Status and navigation bars are hidden. Swipe from an edge to reveal them."
            else
                "Status and navigation bars stay visible; content is kept clear of them."
        ) {
            SegmentedToggle(
                leftLabel = "SAFE AREA",
                rightLabel = "FULLSCREEN",
                rightSelected = fullscreen,
                onSelectRight = onFullscreenChange
            )
        }

        SettingRow(
            title = "Text size",
            description = "Scales the whole terminal UI — pills included — not just the text."
        ) {
            Stepper(
                value = fontScalePercent,
                min = FONT_SCALE_MIN_PERCENT,
                max = FONT_SCALE_MAX_PERCENT,
                step = FONT_SCALE_STEP_PERCENT,
                label = { "$it%" },
                onChange = onFontScalePercentChange
            )
        }

        SettingRow(
            title = "Real pseudoterminal",
            description = "Experimental. Runs commands over the app's own native pty bridge " +
                "instead of a plain pipe, so full-screen tools (vim, top, less, an interactive " +
                "REPL) can actually draw a screen instead of breaking. Unverified on real " +
                "hardware — off is the safe, always-worked default. Takes effect for a new tab " +
                "or the next app launch, not the one you're in."
        ) {
            SegmentedToggle(
                leftLabel = "PIPE",
                rightLabel = "PTY",
                rightSelected = usePty,
                onSelectRight = onUsePtyChange
            )
        }

        SettingRow(
            title = "Environment",
            description = "The PATH and env vars every command actually runs under - the same " +
                "map ShellSession builds fresh for each bootstrap-tier session. Read-only."
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onOpenEnvironment)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "VIEW ENVIRONMENT ›", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        SettingRow(
            title = "History",
            description = "Every command run in any tab, searchable - not just the current " +
                "tab's own up/down-arrow recall."
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onOpenHistory)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "SEARCH HISTORY ›", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        SettingRow(
            title = "AI",
            description = "The API key AI command search/chat uses to suggest shell commands. " +
                "Off until a key is set."
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onOpenAiSettings)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "AI SETTINGS ›", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }

        SettingRow(
            title = "Developer",
            description = "A loopback-only MCP server a paired AI agent can connect to, and the " +
                "biometric-gated switch that lets it run real shell commands."
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.Ink)
                    .clickable(onClick = onOpenMcpServer)
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    "MCP SERVER ›", color = Azphalt.Yellow,
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, description: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            title.uppercase(), color = Azphalt.Ink,
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
        )
        Text(
            // .55f, the style guide's own "text-muted" token - not .6f, which no other caption
            // in the app uses for this role.
            description, color = Azphalt.Ink.copy(alpha = .55f),
            fontSize = 11.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        control()
    }
}

@Composable
private fun SegmentedToggle(
    leftLabel: String,
    rightLabel: String,
    rightSelected: Boolean,
    onSelectRight: (Boolean) -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink.copy(alpha = .14f))
            .padding(3.dp)
    ) {
        listOf(leftLabel to false, rightLabel to true).forEach { (label, selected) ->
            val on = selected == rightSelected
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (on) Azphalt.Ink else Color.Transparent)
                    .clickable { onSelectRight(selected) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    label, color = if (on) Azphalt.Yellow else Azphalt.Ink.copy(alpha = .55f),
                    fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
                )
            }
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    step: Int,
    label: (Int) -> String,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepperButton("−", enabled = value > min) { onChange((value - step).coerceAtLeast(min)) }
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(Azphalt.Ink)
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(
                label(value), color = Azphalt.Yellow,
                fontSize = 12.sp, fontWeight = FontWeight.Black
            )
        }
        StepperButton("+", enabled = value < max) { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepperButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    // UI-7: the visible circle is a deliberately compact 34dp - well under the 48dp minimum
    // touch target. Rather than growing the circle itself, the clickable region is a separate,
    // invisible 48dp box it's centered inside, the same pattern GuideReaderScreen's own Chip
    // helper uses for the identical problem.
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(percent = 50))
            // A capsule is never tinted, faded, or given alpha (style guide "03 -
            // Transparency") - idle uses the same ink-14% wash every other disabled capsule in
            // the app does, not a faded copy of its own hue.
            .background(if (enabled) Azphalt.hues[6] else Azphalt.Ink.copy(alpha = .14f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol,
            color = if (enabled) Azphalt.White else Azphalt.currentGround.onPage.copy(alpha = .55f),
            fontSize = 16.sp, fontWeight = FontWeight.Black
        )
    }
}
