package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.AzphaltChip
import com.hereliesaz.hg2gui.ui.AzphaltDimens
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.StatusKind
import com.hereliesaz.hg2gui.ui.headingRole
import com.hereliesaz.hg2gui.ui.liveRegionPolite
import com.hereliesaz.hg2gui.ui.maxContentWidth
import com.hereliesaz.hg2gui.ui.minTouch
import com.hereliesaz.hg2gui.ui.minTouchSize
import com.hereliesaz.hg2gui.ui.readableOn
import com.hereliesaz.hg2gui.ui.rememberReducedMotion
import com.hereliesaz.hg2gui.ui.scaledDelayMs
import com.hereliesaz.hg2gui.ui.screenInsets
import com.hereliesaz.hg2gui.ui.statusColor
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType
import kotlinx.coroutines.delay

/*
 * The Guide: a chapter index of real commands paired with invented, Hitchhiker's-Guide-style
 * definitions. Every real command heading is also an input affordance: tapping it hands the
 * command back to the terminal's normal command-composition path for review, and the Guide shows
 * a brief "added to input" confirmation. Recognized command names in entry prose are links to that
 * same path; live runtime facts come from the terminal's current command tree rather than a second
 * hand-maintained inventory.
 *
 * Tapping a command never navigates: [onCommandSelected] is the host's cue to append to whatever is
 * being composed, and the reader stays where it is so reading and composing can interleave.
 */

private val GUIDE_HUES = intArrayOf(6, 5, 4, 2, 9, 0, 7)

private enum class GuideView { Index, Entry }

/** True once the reader has been tapped during an entry's wipe-in: every block then snaps to done. */
private val LocalWipeSkipped = compositionLocalOf { false }

/**
 * @param onCommandSelected a command was tapped (heading, prose link or live-option chip). The
 *   Guide stays open and shows an "added to input" chip itself; the host appends the command to the
 *   composition.
 * @param showProductionNotes show each entry's "ANIMATION CANDIDATE" block. That text is
 *   production-brief copy for the people animating the Guide (see docs/GUIDE.md), not reader
 *   content, so it is off unless a debug/dev build turns it on.
 */
@Composable
fun GuideReaderScreen(
    fullscreen: Boolean,
    onBack: () -> Unit,
    onCommandSelected: (String) -> Unit = {},
    backStep: BackStepState,
    runtimeIndex: GuideRuntimeIndex = GuideRuntimeIndex.Empty,
    modifier: Modifier = Modifier,
    showProductionNotes: Boolean = false
) {
    var view by remember { mutableStateOf(GuideView.Index) }
    var entryIndex by remember { mutableStateOf(0) }
    var wipeKey by remember { mutableStateOf(0) }
    var confirmation by remember { mutableStateOf<String?>(null) }
    var confirmationId by remember { mutableStateOf(0) }
    val entries = GuideCatalog.entries

    SideEffect {
        backStep.canStepBack = true
        backStep.stepBack = {
            if (view == GuideView.Entry) view = GuideView.Index else onBack()
        }
    }

    // Each tap restarts the timer (the id changes), so rapid taps keep the chip up for the last one.
    LaunchedEffect(confirmationId) {
        if (confirmationId > 0) {
            delay(GUIDE_CONFIRMATION_MS)
            confirmation = null
        }
    }
    val select: (String) -> Unit = { command ->
        onCommandSelected(command)
        confirmation = guideAddedMessage(command)
        confirmationId++
    }

    Box(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .screenInsets(fullscreen, ime = false)
    ) {
        Column(Modifier.fillMaxSize()) {
            when (view) {
                GuideView.Index -> GuideIndex(
                    onBack = onBack,
                    onOpenEntry = { i -> entryIndex = i; wipeKey++; view = GuideView.Entry }
                )
                GuideView.Entry -> {
                    val entry = entries[entryIndex]
                    GuideEntryReader(
                        entry = entry,
                        runtimeInfo = runtimeIndex.info(entry.cmd),
                        number = entryIndex + 1,
                        total = entries.size,
                        wipeKey = wipeKey,
                        showProductionNotes = showProductionNotes,
                        onCommandSelected = select,
                        onBackToIndex = { view = GuideView.Index },
                        onPrev = {
                            entryIndex = (entryIndex - 1 + entries.size) % entries.size
                            wipeKey++
                        },
                        onNext = {
                            entryIndex = (entryIndex + 1) % entries.size
                            wipeKey++
                        },
                        onReplay = { wipeKey++ }
                    )
                }
            }
        }
        // Sits just above the PREV / NEXT row (48dp chips + 12dp above, 22dp below).
        GuideConfirmation(
            message = confirmation,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 96.dp)
        )
    }
}

@Composable
private fun ColumnScope.GuideIndex(onBack: () -> Unit, onOpenEntry: (Int) -> Unit) {
    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)
    val type = LocalAzphaltType.current

    Row(
        Modifier.maxContentWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AzphaltChip("‹ BACK", onClick = onBack, contentDescription = "Back")
        GuideTag("${GuideCatalog.entries.size} ENTRIES")
    }

    Column(Modifier.maxContentWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Text(
            "THE GUIDE",
            color = onPage,
            fontSize = 34.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.02).em,
            modifier = Modifier.headingRole()
        )
        Text(
            "The galaxy's largest repository of half-truths and pseudo-knowledge required to " +
                "pocket a wild Linux environment. Proceed with a care-free sort of caution.",
            color = onPage,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    LazyColumn(
        Modifier.weight(1f).maxContentWidth().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 18.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        var globalOffset = 0
        GuideCatalog.chapters.forEach { chapter ->
            val chapterStart = globalOffset
            globalOffset += chapter.entries.size
            item(key = chapter.label) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        chapter.label.uppercase(),
                        color = muted,
                        style = type.micro
                    )
                    Text(
                        chapter.title.uppercase(),
                        color = onPage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.01).em,
                        modifier = Modifier.headingRole()
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .16f)))
                    if (chapter.entries.isEmpty()) {
                        Text(
                            chapter.intro,
                            color = onPage,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 11.dp)
                        )
                    }
                    chapter.entries.forEachIndexed { i, entry ->
                        val globalIndex = chapterStart + i
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClickLabel = "Open the ${entry.cmd} entry", role = Role.Button) {
                                    onOpenEntry(globalIndex)
                                }
                                .minTouch(minTouchSize())
                                .padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                entry.cmd,
                                color = onPage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.01.em,
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                entry.teaser,
                                color = muted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .16f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.GuideEntryReader(
    entry: GuideEntry,
    runtimeInfo: GuideRuntimeInfo,
    number: Int,
    total: Int,
    wipeKey: Int,
    showProductionNotes: Boolean,
    onCommandSelected: (String) -> Unit,
    onBackToIndex: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplay: () -> Unit
) {
    val hue = GUIDE_HUES[(number - 1) % GUIDE_HUES.size]
    val page = Azphalt.currentGround.page
    val onPage = Azphalt.currentGround.onPage
    val muted = statusColor(StatusKind.Muted)
    val type = LocalAzphaltType.current
    var seq = 4
    // A tap anywhere while the entry is wiping in finishes the wipe at once (the block under the
    // finger still gets its own tap: the observer below never consumes).
    var wipeSkipped by remember(wipeKey) { mutableStateOf(false) }

    CompositionLocalProvider(LocalWipeSkipped provides wipeSkipped) {
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(wipeKey) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        wipeSkipped = true
                    }
                }
        ) {
            GuideWash(entry.cmd.uppercase(), wipeKey)
            Column(Modifier.fillMaxHeight().maxContentWidth().padding(horizontal = 20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WipeItem(0, wipeKey, wide = false) {
                        AzphaltChip("GUIDE", onClick = onBackToIndex, contentDescription = "Back to the Guide index")
                    }
                    AzphaltChip("REPLAY", onClick = onReplay, contentDescription = "Replay the entry animation")
                }

                WipeItem(1, wipeKey, wide = false, modifier = Modifier.padding(top = 14.dp)) {
                    GuideTag("ENTRY $number OF $total")
                }

                WipeItem(2, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        entry.cmd.uppercase(),
                        color = onPage,
                        fontSize = 34.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.02).em,
                        modifier = Modifier
                            .headingRole()
                            .clickable(onClickLabel = guideAddActionLabel(entry.cmd), role = Role.Button) {
                                onCommandSelected(entry.cmd)
                            }
                            // Height only: a short name ("ls") must not be re-centred by a width floor.
                            .defaultMinSize(minHeight = minTouchSize())
                    )
                }

                Text(
                    "TAP COMMAND TO USE",
                    color = muted,
                    style = type.micro,
                    modifier = Modifier.padding(top = 6.dp)
                )

                WipeItem(3, wipeKey, wide = true, modifier = Modifier.padding(top = 12.dp)) {
                    Box(
                        Modifier
                            .width(220.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Azphalt.hues[hue])
                    )
                }

                val scrollState = remember(wipeKey) { ScrollState(0) }
                Column(Modifier.weight(1f).verticalScroll(scrollState)) {
                    WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
                        GuideLinkedText(
                            text = entry.blurb,
                            commands = GuideCatalog.commands,
                            color = onPage,
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            onCommandSelected = onCommandSelected
                        )
                    }

                    WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            "STANDS FOR “${entry.full.uppercase()}”",
                            color = muted,
                            style = type.micro
                        )
                    }

                    WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp)) {
                        Column {
                            FactRow("Chapter", entry.chapterTitle)
                            // Derived from the live command tree, not asserted: a guide entry for a command
                            // this build doesn't offer doesn't "actually do something" here.
                            FactRow("Actually does something", if (runtimeInfo.available) "Yes" else "Not in this build")
                            FactRow(
                                "Live runtime",
                                if (runtimeInfo.available) {
                                    runtimeInfo.cap?.let { "available · $it" } ?: "available"
                                } else {
                                    "not in current command tree"
                                }
                            )
                        }
                    }

                    if (runtimeInfo.hints.isNotEmpty()) {
                        WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "LIVE OPTIONS",
                                    color = muted,
                                    style = type.micro,
                                    modifier = Modifier.headingRole()
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    runtimeInfo.hints.forEach { hint ->
                                        val root = entry.cmd.substringBefore(' ')
                                        val command = if (hint.startsWith(root)) hint else "$root $hint"
                                        AzphaltChip(
                                            hint,
                                            onClick = { onCommandSelected(command) },
                                            contentDescription = guideAddActionLabel(command)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Production-brief copy for the animators, not for readers: dev builds only.
                    if (showProductionNotes) {
                        entry.animation?.let { anim ->
                            WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp)) {
                                // The note sits on a faint ink wash over the page; check links against that.
                                val noteBackground = Azphalt.Ink.copy(alpha = .06f).compositeOver(page)
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(AzphaltSurface.note)
                                        .background(Azphalt.Ink.copy(alpha = .06f))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        "ANIMATION CANDIDATE",
                                        color = muted,
                                        style = type.micro,
                                        modifier = Modifier.headingRole()
                                    )
                                    GuideLinkedText(
                                        text = anim,
                                        commands = GuideCatalog.commands,
                                        color = onPage,
                                        commandColor = statusColor(StatusKind.Link).readableOn(noteBackground),
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                        modifier = Modifier.padding(top = 8.dp),
                                        onCommandSelected = onCommandSelected
                                    )
                                }
                            }
                        }
                    }

                    entry.note?.let { note ->
                        WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
                            GuideLinkedText(
                                text = note,
                                commands = GuideCatalog.commands,
                                color = onPage,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                onCommandSelected = onCommandSelected
                            )
                        }
                    }

                    WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)) {
                        Column {
                            Text(
                                "FROM THE CHAPTER",
                                color = muted,
                                style = type.micro,
                                modifier = Modifier.headingRole()
                            )
                            GuideLinkedText(
                                text = entry.chapterIntro,
                                commands = GuideCatalog.commands,
                                color = onPage,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 6.dp),
                                onCommandSelected = onCommandSelected
                            )
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WipeItem(seq++, wipeKey, wide = false) {
                        AzphaltChip("PREV", onClick = onPrev, contentDescription = "Previous entry")
                    }
                    WipeItem(
                        seq++, wipeKey, wide = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        AzphaltChip(
                            "NEXT ENTRY",
                            onClick = onNext,
                            modifier = Modifier.fillMaxWidth(),
                            tint = Azphalt.hues[6],
                            contentDescription = "Next entry"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideWash(word: String, wipeKey: Int) {
    val instant = rememberReducedMotion()
    val skipped = LocalWipeSkipped.current
    val drift = remember(wipeKey) { Animatable(if (instant || skipped) 0f else 40f) }
    LaunchedEffect(wipeKey) {
        // The decorative drift is the first thing reduced motion drops.
        if (!instant && !skipped) {
            drift.animateTo(0f, tween(2400, easing = CubicBezierEasing(0f, .9f, .1f, 1f)))
        }
    }
    LaunchedEffect(skipped, instant) { if (skipped || instant) drift.snapTo(0f) }
    Text(
        word,
        color = Azphalt.currentGround.onPage.copy(alpha = .09f),
        fontSize = 100.sp,
        lineHeight = 84.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.04).em,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .padding(top = 64.dp, start = 4.dp)
            .graphicsLayer { translationX = drift.value.dp.toPx() }
            .clearAndSetSemantics {}
    )
}

@Composable
private fun FactRow(label: String, value: String) {
    val onPage = Azphalt.currentGround.onPage
    // One spoken unit ("CHAPTER, Standard Input") instead of two disconnected fragments.
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp).semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label.uppercase(), color = statusColor(StatusKind.Muted), style = LocalAzphaltType.current.micro)
        Text(value, color = onPage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .16f)))
}

/** A non-interactive capsule label ("12 ENTRIES", "ENTRY 3 OF 12"): same look as a chip, no click. */
@Composable
private fun GuideTag(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = AzphaltDimens.ChipHeight)
            .clip(AzphaltSurface.capsule)
            .background(Azphalt.Ink.copy(alpha = .14f))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Azphalt.currentGround.onPage, style = LocalAzphaltType.current.chip, maxLines = 1)
    }
}

/**
 * The transient "added to input" chip. The container is always present and is a polite live
 * region, so a screen reader announces the message when it appears without moving focus.
 */
@Composable
private fun GuideConfirmation(message: String?, modifier: Modifier = Modifier) {
    Box(modifier.liveRegionPolite()) {
        if (message != null) {
            Box(
                Modifier
                    .heightIn(min = AzphaltDimens.ChipHeight)
                    .clip(AzphaltSurface.capsule)
                    .background(Azphalt.Ink)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(message, color = Azphalt.Yellow, style = LocalAzphaltType.current.chip, maxLines = 2)
            }
        }
    }
}

@Composable
private fun WipeItem(
    seq: Int,
    wipeKey: Int,
    wide: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Reduced motion shows the finished entry; a tap anywhere during the wipe does the same.
    val instant = rememberReducedMotion()
    val skipped = LocalWipeSkipped.current
    val progress = remember(wipeKey) { Animatable(if (instant || skipped) 1f else 0f) }
    LaunchedEffect(wipeKey) {
        if (!instant && !skipped) {
            delay(scaledDelayMs(guideWipeDelayMs(seq)))
            progress.animateTo(1f, tween(guideWipeDurationMs(wide), easing = CubicBezierEasing(0f, .9f, .1f, 1f)))
        }
    }
    // snapTo cancels an animation in flight, so a skip mid-wipe lands on the end state immediately.
    LaunchedEffect(skipped, instant) { if (skipped || instant) progress.snapTo(1f) }
    Box(modifier.then(if (wide) Modifier.wipeClip(progress.value) else Modifier.wipeGrow(progress.value))) {
        content()
    }
}

private fun Modifier.wipeClip(progress: Float): Modifier = this
    .drawWithContent {
        clipRect(right = size.width * progress) { this@drawWithContent.drawContent() }
    }
    .graphicsLayer { translationX = -14.dp.toPx() * (1f - progress) }

private fun Modifier.wipeGrow(progress: Float): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val w = (placeable.width * progress).toInt().coerceIn(0, placeable.width)
        layout(w, placeable.height) { placeable.placeRelative(0, 0) }
    }
    .clipToBounds()
