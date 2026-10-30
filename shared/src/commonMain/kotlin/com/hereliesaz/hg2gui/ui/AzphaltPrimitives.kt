package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.contrastingText
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import kotlin.math.roundToLong

/*
 * The shared building blocks every Azphalt screen should reach for instead of re-deriving them:
 * contrast maths and status colours, the one chip, touch-target and inset modifiers, motion
 * scaling, and the small semantics helpers. Nothing in here owns screen state.
 *
 * Platform note: this file is commonMain, so it must stay free of Android APIs. The only
 * platform-fed value (the system animator scale) arrives through [AzphaltMotion], which the
 * androidMain `HG2GuiTheme` keeps current.
 */

// ---------------------------------------------------------------------------------------------
// Dimensions and thresholds
// ---------------------------------------------------------------------------------------------

/** Sizes shared by every screen. All values are dp. */
object AzphaltDimens {
    /** Android's minimum interactive size. See [minTouch]. */
    val MinTouch: Dp = 48.dp

    /** Visible height of an [AzphaltChip] capsule (its hit box is [MinTouch]). */
    val ChipHeight: Dp = 32.dp

    /** Default cap for prose and form columns on wide windows - see [maxContentWidth]. */
    val ContentMaxWidth: Dp = 640.dp
}

/** WCAG 2.x contrast thresholds, so call sites name the rule instead of repeating a number. */
object AzphaltContrast {
    /** Normal-size text (SC 1.4.3). */
    const val TEXT = 4.5f

    /** Large text and non-text UI such as borders and state indicators (SC 1.4.11). */
    const val NON_TEXT = 3f
}

// ---------------------------------------------------------------------------------------------
// Contrast
// ---------------------------------------------------------------------------------------------

private const val LUMINANCE_OFFSET = 0.05f
private const val READABLE_SEARCH_STEPS = 12

/**
 * WCAG 2.x contrast ratio between [a] and [b], from 1 (identical) to 21 (black on white). Order
 * does not matter. Alpha is ignored: composite translucent colours over their real background
 * first (`color.compositeOver(background)`), or the number describes a colour nobody sees.
 */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    val lighter = if (la > lb) la else lb
    val darker = if (la > lb) lb else la
    return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
}

/**
 * This colour, adjusted just enough to read on [background] at [minRatio] (default
 * [AzphaltContrast.TEXT]). Returns the colour unchanged (made opaque by compositing over
 * [background]) when it already passes; otherwise slides it toward whichever of [Azphalt.Ink] /
 * [Azphalt.White] contrasts more with [background], stopping at the smallest step that passes, so
 * a red stays a red-ish colour as long as the ground allows it. If even pure Ink/White cannot reach
 * [minRatio] (impossible for ratios up to about 4.5 on any ground) the better of the two is
 * returned. The result is always opaque.
 */
fun Color.readableOn(background: Color, minRatio: Float = AzphaltContrast.TEXT): Color {
    val start = if (alpha < 1f) compositeOver(background) else this
    if (contrastRatio(start, background) >= minRatio) return start
    val ink = contrastRatio(Azphalt.Ink, background)
    val white = contrastRatio(Azphalt.White, background)
    val target = if (ink >= white) Azphalt.Ink else Azphalt.White
    if (maxOf(ink, white) < minRatio) return target
    var low = 0f
    var high = 1f
    repeat(READABLE_SEARCH_STEPS) {
        val mid = (low + high) / 2f
        if (contrastRatio(lerp(start, target, mid), background) >= minRatio) high = mid else low = mid
    }
    return lerp(start, target, high)
}

// ---------------------------------------------------------------------------------------------
// Status colours
// ---------------------------------------------------------------------------------------------

/**
 * What a piece of status text or a status glyph means. Colour is never the only carrier - the
 * caller still writes the word ("exit 127", "failed", "ok") - but when it does colour something,
 * it asks for a [statusColor] instead of reaching into `Azphalt.hues`, which are tuned to sit
 * behind white/ink text on a pill, not to be text on the rotating ground.
 */
enum class StatusKind {
    /** Failure, stderr, destructive. */
    Error,

    /** Caution, pending, "needs attention". */
    Warning,

    /** Done, trusted, installed. */
    Success,

    /** Neutral information, progress. */
    Info,

    /** De-emphasised text: hints, cwd, timestamps. Still legible. */
    Muted,

    /** An inline link. Keep it underlined - hue is not enough. */
    Link
}

private const val MUTED_ALPHA = 0.72f

/**
 * Non-composable form of [statusColor]: the [kind] colour for text drawn directly on [page].
 * Tries the pill hue, then its darker `caps` sibling, and keeps the first that clears
 * [AzphaltContrast.TEXT]; if neither does (amber on mustard, say) it takes the better one and
 * runs it through [readableOn], which darkens/lightens it to the minimum that passes.
 */
fun statusColorOn(kind: StatusKind, page: Color): Color {
    if (kind == StatusKind.Muted) {
        return page.contrastingText().copy(alpha = MUTED_ALPHA).readableOn(page)
    }
    val hue = when (kind) {
        StatusKind.Error -> HUE_RED
        StatusKind.Warning -> HUE_AMBER
        StatusKind.Success -> HUE_GREEN
        StatusKind.Info -> HUE_CYAN
        StatusKind.Link -> HUE_BLUE
        StatusKind.Muted -> HUE_BLUE // handled above
    }
    val candidates = listOf(Azphalt.hues[hue], Azphalt.caps[hue])
    candidates.firstOrNull { contrastRatio(it, page) >= AzphaltContrast.TEXT }?.let { return it }
    return candidates.maxByOrNull { contrastRatio(it, page) }!!.readableOn(page)
}

// Indices into Azphalt.hues / Azphalt.caps (see the order documented on Azphalt.hueNames).
private const val HUE_CYAN = 1
private const val HUE_GREEN = 3
private const val HUE_AMBER = 4
private const val HUE_RED = 6
private const val HUE_BLUE = 9

/**
 * The colour for [kind] text/glyphs on the *current* ground, guaranteed at least
 * [AzphaltContrast.TEXT] (4.5:1) against `Azphalt.currentGround.page`. Recomposes when the ground
 * is rerolled. For text on a card (ink wash over the page) the true ratio is a hair lower; for
 * text on an Ink chip use `Azphalt.Yellow`/`Azphalt.White` instead.
 */
@Composable
fun statusColor(kind: StatusKind): Color {
    val page = Azphalt.currentGround.page
    return remember(kind, page) { statusColorOn(kind, page) }
}

// ---------------------------------------------------------------------------------------------
// Touch targets and layout
// ---------------------------------------------------------------------------------------------

/**
 * Makes this element at least [size] x [size] (48dp by default) *as a hit box* while the drawn
 * content keeps its own size: the child is measured at its natural size and centred in the larger
 * box. The layout footprint does grow to [size], so neighbours must leave room; what it never
 * does is stretch the visible capsule.
 *
 * Order matters. Put the gesture modifier *before* it and the drawing modifiers *after* it:
 *
 *     Modifier.clickable(...).minTouch().clip(shape).background(fill).padding(...)
 *
 * so the clickable sees the 48dp box and the background paints only the capsule. (If the
 * background comes first it will be painted across the whole 48dp box.)
 *
 * At a Text-size setting below 100% the whole UI is drawn at a smaller density, so a literal 48dp
 * would be a smaller physical size; composables should pass [minTouchSize] instead of the default
 * to stay at 48dp on the glass.
 */
fun Modifier.minTouch(size: Dp = AzphaltDimens.MinTouch): Modifier = layout { measurable, constraints ->
    val minPx = size.roundToPx()
    val placeable = measurable.measure(constraints)
    val width = maxOf(placeable.width, minPx).coerceIn(constraints.minWidth, constraints.maxWidth)
    val height = maxOf(placeable.height, minPx).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(width, height) {
        placeable.placeRelative((width - placeable.width) / 2, (height - placeable.height) / 2)
    }
}

/**
 * The UI density multiplier chosen with the Text-size setting (1 = 100%). `HG2GuiTheme` provides
 * it; it is 1 everywhere else.
 */
val LocalAzphaltUiScale = staticCompositionLocalOf { 1f }

/**
 * 48dp *on the glass*: [AzphaltDimens.MinTouch] divided by the UI scale when the user has shrunk
 * the UI (scale < 1), unchanged otherwise. Feed it to [minTouch].
 */
@Composable
fun minTouchSize(): Dp {
    val scale = LocalAzphaltUiScale.current
    return if (scale > 0f && scale < 1f) AzphaltDimens.MinTouch / scale else AzphaltDimens.MinTouch
}

/**
 * Keeps a column readable on wide windows (tablets, foldables, landscape): at most [max] wide,
 * centred, and filling the width up to that cap. On a phone it is a no-op. Apply to the screen's
 * content column, not to the full-bleed background.
 */
fun Modifier.maxContentWidth(max: Dp = AzphaltDimens.ContentMaxWidth): Modifier =
    this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max).fillMaxWidth()

/**
 * Pads a screen's root by the system bars and the display cutout, and (by default) the on-screen
 * keyboard. This is the one inset policy for every Activity and full-screen composable; use it
 * instead of hand-rolling `windowInsetsPadding(WindowInsets.systemBars)`.
 *
 * - [fullscreen]: the user chose the immersive "fullscreen" preference, so system bars are not
 *   padded (they are hidden); the cutout and keyboard still are.
 * - [ime]: also pad for the keyboard. The IME padding is appended *last* on purpose: it is applied
 *   after the system-bar padding has consumed its share of the bottom edge, so the nav bar is not
 *   counted twice when the keyboard opens. If a child also calls `imePadding()` it must be a child
 *   of this modifier's node, never a sibling chain ahead of it. Pass `ime = false` for a parent
 *   whose children place their own input rows with `Modifier.imePadding()`.
 *
 * Call it once per screen root. Nesting it double-pads, because nested `windowInsetsPadding`
 * consumes insets only for descendants, not for modifiers declared before it on the same node.
 */
@Composable
fun Modifier.screenInsets(fullscreen: Boolean = false, ime: Boolean = true): Modifier {
    val insets = if (fullscreen) {
        WindowInsets.displayCutout
    } else {
        WindowInsets.systemBars.union(WindowInsets.displayCutout)
    }
    val padded = this.windowInsetsPadding(insets)
    return if (ime) padded.imePadding() else padded
}

// ---------------------------------------------------------------------------------------------
// Motion
// ---------------------------------------------------------------------------------------------

private const val MAX_DURATION_SCALE = 10f

/**
 * The system's animation preference as the UI sees it. Compose already scales `tween`/`spring`
 * durations by the platform animator scale; what it cannot scale are `delay()` calls (entrance
 * staggers, hand-off pauses) and the decision to skip a flourish entirely. Those read this.
 *
 * Fed by the androidMain `HG2GuiTheme` from `Settings.Global.ANIMATOR_DURATION_SCALE` (which
 * "Remove animations" sets to 0). On a target with no feed, it stays at 1.
 */
object AzphaltMotion {
    /** 1 = normal, 0 = animations off, 2 = twice as slow. */
    var durationScale: Float by mutableStateOf(1f)
        private set

    /** True when the user has turned animations off (animator scale 0). */
    val reduced: Boolean get() = durationScale <= 0f

    /** Called by the platform theme. Out-of-range or NaN input falls back to normal speed. */
    internal fun update(scale: Float) {
        durationScale = if (scale.isNaN() || scale < 0f) 1f else scale.coerceAtMost(MAX_DURATION_SCALE)
    }
}

/**
 * [ms] stretched or shrunk by the system animator scale (0 when animations are off). Use around
 * every `delay()` that choreographs motion: `delay(scaledDelayMs(120))`.
 */
fun scaledDelayMs(ms: Long): Long = (ms * AzphaltMotion.durationScale).roundToLong()

/** Int overload of [scaledDelayMs] for the many millisecond constants that are Ints. */
fun scaledDelayMs(ms: Int): Long = scaledDelayMs(ms.toLong())

/**
 * True when the user turned animations off. Recomposes if the setting changes. When it is true,
 * skip entrance/hand-off flourishes outright (show the end state) rather than playing them at
 * zero length; decorative loops should not start at all.
 */
@Composable
fun rememberReducedMotion(): Boolean = AzphaltMotion.reduced

// ---------------------------------------------------------------------------------------------
// Semantics helpers
// ---------------------------------------------------------------------------------------------

/**
 * Marks this node as a live region so a screen reader announces changes to its text without moving
 * focus (error banners, status strips, "copied"). Polite: it waits for the current announcement.
 */
fun Modifier.liveRegionPolite(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }

/** Marks this node as a heading so screen-reader users can jump between sections. */
fun Modifier.headingRole(): Modifier = semantics { heading() }

/**
 * A screen title: the [LocalAzphaltType] heading style (22sp, much larger than the 11sp Eyebrow),
 * upper-cased like the rest of the system, exposed to accessibility as a heading. [color] defaults
 * to the ground's own text colour.
 */
@Composable
fun AzphaltHeading(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Azphalt.currentGround.onPage
) {
    Text(
        text = text.uppercase(),
        color = color,
        style = LocalAzphaltType.current.heading,
        modifier = modifier.headingRole()
    )
}

// ---------------------------------------------------------------------------------------------
// The chip
// ---------------------------------------------------------------------------------------------

private const val MIN_TEXT_SP = 11f
private const val WASH_ALPHA = 0.14f
private const val BORDER_ALPHA = 0.75f
private const val DISABLED_ALPHA = 0.5f
private val CHIP_BORDER_WIDTH = 1.5.dp
private val CHIP_HORIZONTAL_PADDING = 14.dp

/**
 * The one small button of the app - the replacement for the private `Chip` / `PickerChip` /
 * `PickerPill` copies. A capsule with a >= 11sp label, a 48dp hit box around a [AzphaltDimens.ChipHeight]
 * visible capsule (see [minTouch]), contrast-safe text and screen-reader semantics.
 *
 * - [selected]: `null` for a plain button; `true`/`false` for a toggle or one option of a group.
 *   A selected chip is filled with [tint]; an unselected one is a light wash of [tint] with a
 *   visible outline and page-coloured text, so the difference is a shape and weight change as well
 *   as colour. The state is also exposed as the `selected` semantic. For a group, pass
 *   `role = Role.RadioButton` or `Role.Tab`.
 * - [tint]: the fill. With the default [Azphalt.Ink] the text is [Azphalt.Yellow] (the house
 *   pairing); with any other tint the text is whichever of Ink/White reads better on it.
 * - [contentDescription]: spoken instead of the label. Required for glyph-only labels ("+", "x",
 *   a lone arrow) and for chips whose label is shared between rows ("OPEN").
 * - [textSize]: defaults to the `chip` type token; values below 11sp are raised to 11sp.
 * - [enabled]: a disabled chip is dimmed and does not respond.
 *
 * The chip takes only the width of its label by default; pass `Modifier.fillMaxWidth()` to stretch.
 */
@Suppress("LongParameterList")
@Composable
fun AzphaltChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    enabled: Boolean = true,
    tint: Color = Azphalt.Ink,
    contentDescription: String? = null,
    role: Role = Role.Button,
    textSize: TextUnit = LocalAzphaltType.current.chip.fontSize
) {
    val ground = Azphalt.currentGround
    val filled = selected != false
    val fill = if (filled) tint else tint.copy(alpha = WASH_ALPHA)
    val textColor = when {
        !filled -> ground.onPage
        tint == Azphalt.Ink -> Azphalt.Yellow
        else -> tint.contrastingText()
    }
    val size = if (textSize.type == TextUnitType.Sp && textSize.value < MIN_TEXT_SP) MIN_TEXT_SP.sp else textSize
    // Local copies: inside the semantics block below, bare `selected` / `contentDescription` would
    // resolve to the SemanticsPropertyReceiver extension properties of the same name.
    val selectedState = selected
    val description = contentDescription
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .semantics {
                if (description != null) this.contentDescription = description
                if (selectedState != null) this.selected = selectedState
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick
            )
            .minTouch(minTouchSize())
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .heightIn(min = AzphaltDimens.ChipHeight)
                .clip(AzphaltSurface.capsule)
                .background(fill)
                .then(
                    if (filled) {
                        Modifier
                    } else {
                        Modifier.border(CHIP_BORDER_WIDTH, ground.onPage.copy(alpha = BORDER_ALPHA), AzphaltSurface.capsule)
                    }
                )
                .indication(interaction, LocalIndication.current)
                .padding(horizontal = CHIP_HORIZONTAL_PADDING),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.uppercase(),
                color = textColor,
                style = LocalAzphaltType.current.chip.copy(fontSize = size),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
