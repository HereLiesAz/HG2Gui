package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.BackStepState
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage
import com.hereliesaz.hg2gui.ui.menu.pageBrush
import com.hereliesaz.hg2gui.ui.theme.AzphaltSurface
import kotlinx.coroutines.delay

/*
 * The Guide: a chapter index of real commands paired with invented, Hitchhiker's-Guide-style
 * definitions. Every real command heading is also an input affordance: tapping it hands the
 * command back to the terminal's normal command-composition path for review. Recognized command
 * names in entry prose are links to that same path; live runtime facts come from the terminal's
 * current command tree rather than a second hand-maintained inventory.
 */

private val GUIDE_HUES = intArrayOf(6, 5, 4, 2, 9, 0, 7)

private enum class GuideView { Index, Entry }

@Composable
fun GuideReaderScreen(
    fullscreen: Boolean,
    onBack: () -> Unit,
    onCommandSelected: (String) -> Unit = {},
    backStep: BackStepState,
    runtimeIndex: GuideRuntimeIndex = GuideRuntimeIndex.Empty,
    modifier: Modifier = Modifier
) {
    var view by remember { mutableStateOf(GuideView.Index) }
    var entryIndex by remember { mutableStateOf(0) }
    var wipeKey by remember { mutableStateOf(0) }
    val entries = GuideCatalog.entries

    SideEffect {
        backStep.canStepBack = true
        backStep.stepBack = {
            if (view == GuideView.Entry) view = GuideView.Index else onBack()
        }
    }

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
            GuideView.Entry -> {
                val entry = entries[entryIndex]
                GuideEntryReader(
                    entry = entry,
                    runtimeInfo = runtimeIndex.info(entry.cmd),
                    number = entryIndex + 1,
                    total = entries.size,
                    wipeKey = wipeKey,
                    onCommandSelected = onCommandSelected,
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
}

@Composable
private fun ColumnScope.GuideIndex(onBack: () -> Unit, onOpenEntry: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Chip("‹ BACK", onClick = onBack)
        Chip("${GuideCatalog.entries.size} ENTRIES", filled = false, clickable = false)
    }

    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
        Text(
            "THE GUIDE",
            color = Azphalt.currentGround.onPage,
            fontSize = 34.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.02).em
        )
        Text(
            "The galaxy's largest repository of half-truths and pseudo-knowledge required to " +
                "pocket a wild Linux environment. Proceed with a care-free sort of caution.",
            color = Azphalt.currentGround.onPage.copy(alpha = .78f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    LazyColumn(
        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
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
                        color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.18.em
                    )
                    Text(
                        chapter.title.uppercase(),
                        color = Azphalt.currentGround.onPage,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.01).em
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Azphalt.Ink.copy(alpha = .16f)))
                    if (chapter.entries.isEmpty()) {
                        Text(
                            chapter.intro,
                            color = Azphalt.currentGround.onPage.copy(alpha = .7f),
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
                                .clickable { onOpenEntry(globalIndex) }
                                .padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                entry.cmd,
                                color = Azphalt.currentGround.onPage,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.01.em,
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                entry.teaser,
                                color = Azphalt.currentGround.onPage.copy(alpha = .55f),
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
    onCommandSelected: (String) -> Unit,
    onBackToIndex: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplay: () -> Unit
) {
    val hue = GUIDE_HUES[(number - 1) % GUIDE_HUES.size]
    var seq = 4

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
                    color = Azphalt.currentGround.onPage,
                    fontSize = 34.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.02).em,
                    modifier = Modifier.clickable { onCommandSelected(entry.cmd) }
                )
            }

            Text(
                "TAP COMMAND TO USE",
                color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.14.em,
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
                        color = Azphalt.currentGround.onPage.copy(alpha = .78f),
                        commandColor = Azphalt.hues[hue],
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        onCommandSelected = onCommandSelected
                    )
                }

                WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        "STANDS FOR “${entry.full.uppercase()}”",
                        color = Azphalt.currentGround.onPage.copy(alpha = .45f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.14.em
                    )
                }

                WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp)) {
                    Column {
                        FactRow("Chapter", entry.chapterTitle)
                        FactRow("Actually does something", "Yes")
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
                                color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.18.em
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                runtimeInfo.hints.forEach { hint ->
                                    val root = entry.cmd.substringBefore(' ')
                                    val command = if (hint.startsWith(root)) hint else "$root $hint"
                                    Chip(hint, onClick = { onCommandSelected(command) })
                                }
                            }
                        }
                    }
                }

                entry.animation?.let { anim ->
                    WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 20.dp)) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(AzphaltSurface.note)
                                .background(Azphalt.Ink.copy(alpha = .06f))
                                .padding(16.dp)
                        ) {
                            Text(
                                "ANIMATION CANDIDATE",
                                color = Azphalt.hues[hue],
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.18.em
                            )
                            GuideLinkedText(
                                text = anim,
                                commands = GuideCatalog.commands,
                                color = Azphalt.currentGround.onPage.copy(alpha = .78f),
                                commandColor = Azphalt.hues[hue],
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(top = 8.dp),
                                onCommandSelected = onCommandSelected
                            )
                        }
                    }
                }

                entry.note?.let { note ->
                    WipeItem(seq++, wipeKey, wide = true, modifier = Modifier.padding(top = 16.dp)) {
                        GuideLinkedText(
                            text = note,
                            commands = GuideCatalog.commands,
                            color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                            commandColor = Azphalt.hues[hue],
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
                            color = Azphalt.currentGround.onPage.copy(alpha = .55f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.18.em
                        )
                        GuideLinkedText(
                            text = entry.chapterIntro,
                            commands = GuideCatalog.commands,
                            color = Azphalt.currentGround.onPage.copy(alpha = .78f),
                            commandColor = Azphalt.hues[hue],
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
                WipeItem(seq++, wipeKey, wide = false) { Chip("PREV", onClick = onPrev) }
                WipeItem(
                    seq++, wipeKey, wide = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Chip(
                        "NEXT ENTRY",
                        background = Azphalt.hues[6],
                        foreground = Azphalt.White,
                        onClick = onNext,
                        fillWidth = true
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideWash(word: String, wipeKey: Int) {
    val drift = remember(wipeKey) { Animatable(40f) }
    LaunchedEffect(wipeKey) {
        drift.snapTo(40f)
        drift.animateTo(0f, tween(2400, easing = CubicBezierEasing(0f, .9f, .1f, 1f)))
    }
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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label.uppercase(), color = Azphalt.currentGround.onPage.copy(alpha = .55f),
            fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em
        )
        Text(value, color = Azphalt.currentGround.onPage, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    val fg = if (filled) foreground else Azphalt.currentGround.onPage.copy(alpha = .55f)
    Box(
        modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = 48.dp)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(bg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = fg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.09.em)
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
    .clipToBounds()
