package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import kotlinx.coroutines.delay

/*
 * The Guide: a chapter index of real commands paired with invented, Hitchhiker's-Guide-style
 * definitions - reading material, not a command picker. Nested inside the existing command
 * picker (also historically called "the Guide") rather than replacing it, reachable through a
 * pill at that screen's top.
 *
 * Every screen change wipes on: one axis, reading order, no fade, never loops. Text and rules
 * reveal via a left-to-right clip; pills grow their actual width instead, so a rounded end grows
 * into being rather than getting guillotined by a hard clip partway through its curve. A faint,
 * oversized echo of the entry's own word drifts in behind everything else - depth is speed, not
 * blur (GuideWash).
 */

private val GUIDE_HUES = intArrayOf(6, 5, 4, 2, 9, 0, 7) // red, orange, amber, teal, blue, violet, magenta

private enum class GuideView { Index, Entry }

@Composable
fun GuideReaderScreen(
    fullscreen: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var view by remember { mutableStateOf(GuideView.Index) }
    var entryIndex by remember { mutableStateOf(0) }
    var wipeKey by remember { mutableStateOf(0) }

    Column(
        modifier
            .fillMaxSize()
            .background(Azphalt.currentGround.pageBrush())
            .then(if (fullscreen) Modifier else Modifier.windowInsetsPadding(WindowInsets.systemBars))
    ) {
        when (view) {
            GuideView.Index -> GuideIndex(
                onBack = onBack,
                onOpenEntry = { i -> entryIndex = i; wipeKey++; view = GuideView.Entry }
            )
            GuideView.Entry -> GuideEntryReader(
                entry = GuideBook.entries[entryIndex],
                number = entryIndex + 1,
                total = GuideBook.entries.size,
                wipeKey = wipeKey,
                onBackToIndex = { view = GuideView.Index },
                onPrev = {
                    entryIndex = (entryIndex - 1 + GuideBook.entries.size) % GuideBook.entries.size
                    wipeKey++
                },
                onNext = {
                    entryIndex = (entryIndex + 1) % GuideBook.entries.size
                    wipeKey++
                },
                onReplay = { wipeKey++ }
            )
        }
    }
}

@Composable
private fun ColumnScope.GuideIndex(onBack: () -> Unit, onOpenEntry: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip("‹ BACK", onClick = onBack)
        Chip("${GuideBook.entries.size} ENTRIES", filled = false, clickable = false)
    }

    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Text(
            "THE GUIDE",
            color = Azphalt.Ink,
            fontSize = 34.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.02).em
        )
        Text(
            "The galaxy's largest repository of half-truths and pseudo-knowledge required to " +
                "pocket a wild Linux environment. Proceed with a care-free sort of caution.",
            color = Azphalt.Ink.copy(alpha = .78f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    LazyColumn(
        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        GuideBook.chapters.forEach { chapter ->
            item(key = chapter.label) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        chapter.label.uppercase(),
                        color = Azphalt.Ink.copy(alpha = .45f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.18.em
                    )
                    Text(
                        chapter.title.uppercase(),
                        color = Azphalt.Ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.01).em
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .16f)))
                    chapter.entries.forEach { entry ->
                        val globalIndex = GuideBook.entries.indexOf(entry)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenEntry(globalIndex) }
                                .padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                entry.cmd,
                                color = Azphalt.Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.01.em,
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                entry.teaser,
                                color = Azphalt.Ink.copy(alpha = .55f),
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
    number: Int,
    total: Int,
    wipeKey: Int,
    onBackToIndex: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplay: () -> Unit
) {
    val hue = GUIDE_HUES[GuideBook.entries.indexOf(entry) % GUIDE_HUES.size]

    Box(Modifier.fillMaxSize().clipToBounds()) {
    GuideWash(entry.cmd.uppercase(), wipeKey)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WipeItem(0, wipeKey, wide = false) { Chip("GUIDE", onClick = onBackToIndex) }
            Chip("REPLAY", onClick = onReplay)
        }

        WipeItem(1, wipeKey, wide = false, modifier = Modifier.padding(top = 14.dp)) {
            Chip("ENTRY $number OF $total", filled = false, clickable = false)
        }

        WipeItem(2, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
            Text(
                entry.cmd.uppercase(),
                color = Azphalt.Ink,
                fontSize = 34.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.02).em
            )
        }

        WipeItem(3, wipeKey, wide = true, modifier = Modifier.padding(top = 12.dp)) {
            Box(
                Modifier
                    .width(220.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Azphalt.hues[hue])
            )
        }

        WipeItem(4, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
            Text(
                entry.blurb,
                color = Azphalt.Ink.copy(alpha = .78f),
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }

        WipeItem(5, wipeKey, wide = true, modifier = Modifier.padding(top = 10.dp)) {
            Text(
                "STANDS FOR “${entry.full.uppercase()}”",
                color = Azphalt.Ink.copy(alpha = .45f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.14.em
            )
        }

        WipeItem(6, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp)) {
            Column {
                FactRow("Chapter", entry.chapterTitle)
                FactRow("Actually does something", "Yes")
            }
        }

        WipeItem(7, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp)) {
            Column {
                Text(
                    "FROM THE CHAPTER",
                    color = Azphalt.Ink.copy(alpha = .45f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.18.em
                )
                Text(
                    entry.chapterIntro,
                    color = Azphalt.Ink.copy(alpha = .78f),
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth().padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WipeItem(8, wipeKey, wide = false) { Chip("PREV", onClick = onPrev) }
            WipeItem(
                9, wipeKey, wide = false,
                modifier = Modifier.weight(1f)
            ) {
                Chip("NEXT ENTRY", background = Azphalt.hues[6], foreground = Azphalt.White, onClick = onNext, fillWidth = true)
            }
        }
    }
    }
}

/**
 * The faint oversized echo of the entry's own word, drifting in from the right at a fraction of
 * the entry's own pace - "depth is speed," not blur or dimming. Plays once per [wipeKey], same as
 * every WipeItem, so Replay restarts it too.
 */
@Composable
private fun GuideWash(word: String, wipeKey: Int) {
    val drift = remember(wipeKey) { Animatable(40f) }
    LaunchedEffect(wipeKey) {
        drift.snapTo(40f)
        drift.animateTo(0f, tween(2400, easing = CubicBezierEasing(0f, .9f, .1f, 1f)))
    }
    Text(
        word,
        color = Azphalt.Ink.copy(alpha = .09f),
        fontSize = 100.sp,
        lineHeight = 84.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.04).em,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .padding(top = 64.dp, start = 4.dp)
            .graphicsLayer { translationX = drift.value.dp.toPx() }
    )
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label.uppercase(), color = Azphalt.Ink.copy(alpha = .55f),
            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
        )
        Text(value, color = Azphalt.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .16f)))
}

@Composable
private fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    background: Color = Azphalt.Ink,
    foreground: Color = Azphalt.Yellow,
    filled: Boolean = true,
    clickable: Boolean = true,
    fillWidth: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bg = if (filled) background else Azphalt.Ink.copy(alpha = .14f)
    val fg = if (filled) foreground else Azphalt.Ink.copy(alpha = .55f)
    Box(
        modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
    }
}

/**
 * One element of the wipe-on sequence: `wide` elements (text/rules) reveal via a left-to-right
 * clip with a slight slide, since the content is already full width - only visibility grows.
 * Non-wide elements are pills/chips, whose actual layout width is animated instead of just
 * clip-revealing a static full-width shape, so a chip's *slot* in its Row grows along with it
 * rather than the Row reserving full width for it from the first frame.
 */
@Composable
private fun WipeItem(seq: Int, wipeKey: Int, wide: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val duration = if (wide) 420 else 300
    val progress = remember(wipeKey) { Animatable(0f) }
    LaunchedEffect(wipeKey) {
        progress.snapTo(0f)
        delay((120 + seq * 110).toLong())
        progress.animateTo(1f, tween(duration, easing = CubicBezierEasing(0f, .9f, .1f, 1f)))
    }
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
    // The layout above only shrinks the reported *size* - Compose never clips a child to that
    // reported size on its own, so without this the full-size pill still drew at every progress
    // value and overlapped whatever the Row placed next to it. clipToBounds costs the rounded
    // corner's leading curve mid-animation (a flat edge for a few frames instead), which is a far
    // smaller cost than two chips rendering on top of each other.
    .clipToBounds()
