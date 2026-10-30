package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.contrastingText
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType

/*
 * The one confirmation primitive in the app - every destructive/irreversible action (delete,
 * overwrite-on-rename/move/copy, closing a session mid-command, discarding unsaved edits, and a
 * running command's own yes/no question) routes through this rather than each call site rolling
 * its own prompt. Same capsule language as the rest of Azphalt: an ink panel at the "record tile"
 * 26dp radius (DESIGN.md §7), two capsule pills, no shadow/blur/icon - `usePlatformDefaultWidth =
 * false` so the panel is sized and shaped by us rather than the platform dialog window's own
 * default card chrome.
 */

/** How loudly a [ConfirmDialog] announces itself. */
enum class ConfirmTone {
    /** The ordinary ink panel. */
    Normal,

    /**
     * Authority elevation (root, ADB shell): the panel gets a heavy outline in addition to the
     * [badge][ConfirmDialog] pill, so it cannot be mistaken for a routine yes/no.
     */
    Elevated
}

private val PANEL_MAX_WIDTH = 480.dp
private const val PANEL_MAX_HEIGHT_FRACTION = 0.9f
private val PANEL_FALLBACK_MAX_HEIGHT = 560.dp
private val ELEVATED_BORDER = 3.dp
private val CANCEL_BORDER = 1.5.dp
private const val CANCEL_BORDER_ALPHA = 0.65f
private const val MESSAGE_ALPHA = 0.88f
private const val CANCEL_TEXT_ALPHA = 0.92f

/**
 * A modal confirmation. [message] scrolls when it is longer than the screen, the panel never grows
 * wider than a phone-sized dialog on a tablet, and system Back / tapping outside is [onDismiss]
 * (the safe answer: confirming always takes a deliberate tap on the confirm pill).
 *
 * - [tone] / [badge]: an [ConfirmTone.Elevated] dialog is outlined and carries [badge] (for
 *   example "RUNS AS ROOT") in a bright pill above the title, which is how root/ADB prompts are
 *   told apart from a routine one.
 * - [monospaceMessage]: show [message] in a monospace face (commands, paths).
 * - [confirmTint]: the confirm pill's fill; the cancel pill is always an outline, so the two are
 *   told apart by shape as well as colour.
 * - [extraContent]: optional content under the message (the rename dialog's text field).
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = "CANCEL",
    tone: ConfirmTone = ConfirmTone.Normal,
    badge: String? = null,
    monospaceMessage: Boolean = false,
    confirmTint: Color = Azphalt.hues[6],
    extraContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val type = LocalAzphaltType.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            val panelMaxHeight = if (constraints.hasBoundedHeight) maxHeight * PANEL_MAX_HEIGHT_FRACTION else PANEL_FALLBACK_MAX_HEIGHT
            Column(
                Modifier
                    .widthIn(max = PANEL_MAX_WIDTH)
                    .fillMaxWidth()
                    .heightIn(max = panelMaxHeight)
                    .clip(AzphaltSurface.recordTile)
                    .background(Azphalt.Ink)
                    .then(
                        if (tone == ConfirmTone.Elevated) {
                            Modifier.border(ELEVATED_BORDER, Azphalt.Yellow, AzphaltSurface.recordTile)
                        } else {
                            Modifier
                        }
                    )
                    .padding(22.dp)
            ) {
                if (badge != null) {
                    Box(
                        Modifier
                            .clip(AzphaltSurface.capsule)
                            .background(Azphalt.Yellow)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(badge.uppercase(), color = Azphalt.Ink, style = type.chip.copy(fontSize = 11.sp))
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    title.uppercase(),
                    color = Azphalt.Yellow,
                    style = type.capsule.copy(fontSize = 15.sp, lineHeight = 20.sp),
                    modifier = Modifier.headingRole()
                )
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    if (message.isNotBlank()) {
                        val base = type.body.copy(fontSize = 14.sp, lineHeight = 20.sp)
                        Text(
                            message,
                            color = Azphalt.White.copy(alpha = MESSAGE_ALPHA),
                            style = if (monospaceMessage) base.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp) else base
                        )
                    }
                    extraContent?.invoke(this)
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            // 48dp minimum touch target: the button already fills half the row via
                            // weight(1f), so growing its own height (rather than wrapping it in a
                            // second invisible box) doesn't distort its visual proportions.
                            .defaultMinSize(minHeight = AzphaltDimens.MinTouch)
                            .clip(AzphaltSurface.capsule)
                            .border(CANCEL_BORDER, Azphalt.White.copy(alpha = CANCEL_BORDER_ALPHA), AzphaltSurface.capsule)
                            .clickable(role = Role.Button, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            cancelLabel.uppercase(),
                            color = Azphalt.White.copy(alpha = CANCEL_TEXT_ALPHA),
                            style = type.chip,
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = AzphaltDimens.MinTouch)
                            .clip(AzphaltSurface.capsule)
                            // hues[6] is the same "red - primary / run" acquire hue AzphaltColors
                            // marks as the destructive/primary-action tone elsewhere in the app.
                            .background(confirmTint)
                            .clickable(role = Role.Button, onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            confirmLabel.uppercase(),
                            color = confirmTint.contrastingText(),
                            style = type.chip,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** What a yes/no question from a running command is asking, so the dialog can word and style it. */
enum class ConfirmPromptKind {
    /** An ordinary question. */
    General,

    /** The command would run as root. */
    Root,

    /** The command would run through the ADB shell. */
    Adb
}

/** A yes/no prompt split into the parts of a [ConfirmDialog]. */
data class ParsedConfirmPrompt(
    val kind: ConfirmPromptKind,
    val title: String,
    val body: String,
    /** The pill text for an elevated prompt ("RUNS AS ROOT"), or null for a routine one. */
    val badge: String?
)

/**
 * Pure helpers that turn the raw text of a "[y/N]" prompt into a dialog: a real title (the
 * question itself, not a generic "Confirm"), the whole multi-line body, and the root/ADB flag.
 */
object ConfirmPrompts {
    private const val MAX_TITLE_CHARS = 48
    private const val MAX_DETAIL_CHARS = 4_000
    private const val MAX_GENERAL_DETAIL_LINES = 12
    private const val FALLBACK_TITLE = "Confirm"
    private val ROOT_PROMPT = Regex("^\\s*(run as root|request root access)", RegexOption.IGNORE_CASE)
    private val ADB_PROMPT = Regex("^\\s*run through adb shell", RegexOption.IGNORE_CASE)

    /** Which kind of authority [prompt] asks for, judged from the wording the engine uses. */
    fun kindOf(prompt: String): ConfirmPromptKind = when {
        ROOT_PROMPT.containsMatchIn(prompt) -> ConfirmPromptKind.Root
        ADB_PROMPT.containsMatchIn(prompt) -> ConfirmPromptKind.Adb
        else -> ConfirmPromptKind.General
    }

    /**
     * [prompt] bounded for display. An elevated prompt keeps its head (the command being
     * authorised is what the user must read); an ordinary prompt keeps its tail, because a PTY can
     * deliver a whole transcript ahead of the question.
     */
    fun detailOf(prompt: String): String {
        val trimmed = prompt.trim()
        if (kindOf(trimmed) != ConfirmPromptKind.General) return trimmed.take(MAX_DETAIL_CHARS)
        return trimmed.lines().takeLast(MAX_GENERAL_DETAIL_LINES).joinToString("\n").takeLast(MAX_DETAIL_CHARS)
    }

    /** Splits [prompt] (typically [detailOf]'s output) into dialog parts. */
    fun parse(prompt: String): ParsedConfirmPrompt {
        val text = prompt.trim()
        val kind = kindOf(text)
        val questionEnd = text.indexOf('?')
        val hasQuestionTitle = questionEnd in 1..MAX_TITLE_CHARS && '\n' !in text.substring(0, questionEnd)
        val title = if (hasQuestionTitle) text.substring(0, questionEnd + 1) else FALLBACK_TITLE
        val rest = if (hasQuestionTitle) text.substring(questionEnd + 1).trim() else text
        val badge = when (kind) {
            ConfirmPromptKind.Root -> "Elevated - runs as root"
            ConfirmPromptKind.Adb -> "Elevated - runs through ADB"
            ConfirmPromptKind.General -> null
        }
        return ParsedConfirmPrompt(kind = kind, title = title, body = rest.ifBlank { text }, badge = badge)
    }
}
