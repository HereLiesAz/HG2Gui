package com.hereliesaz.hg2gui.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage

/**
 * One shared physical workspace for terminal scrollback and the pill command tree.
 *
 * The command tree owns no rectangular pane: it is free to fan, scroll and animate through the
 * whole workspace. The terminal buffer behaves more like a trapped air pocket. While the pill
 * stack is low it occupies the full clear width above the stack. As the stack rises into that
 * space, the buffer continuously deforms into the free column beside it. The deformation is
 * driven by the command tree's consumed scroll distance as well as its current root-row count, so
 * scrolling a tall stack upward makes the buffer yield sideways instead of letting the two panes
 * collide.
 */
@Composable
internal fun FluidTerminalWorkspace(
    hasBuffer: Boolean,
    estimatedPillRows: Int,
    modifier: Modifier = Modifier,
    bufferContent: @Composable ColumnScope.() -> Unit,
    commandTreeContent: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        var pillScrollOffsetPx by remember(estimatedPillRows) { mutableStateOf(0f) }
        val scrollConnection = remember(estimatedPillRows) {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    pillScrollOffsetPx = (pillScrollOffsetPx + consumed.y).coerceAtLeast(0f)
                    return Offset.Zero
                }
            }
        }

        val rowCount = estimatedPillRows.coerceAtLeast(1)
        val baseStackHeight = FLOW_ANCHOR_HEIGHT + FLOW_ROW_PITCH * (rowCount - 1).toFloat()
        val scrolledDown = with(density) { pillScrollOffsetPx.toDp() }
        val visibleStackHeight = (baseStackHeight - scrolledDown).coerceAtLeast(FLOW_ANCHOR_HEIGHT)
        val topClearance = (maxHeight - visibleStackHeight).coerceAtLeast(0.dp)
        val sideTarget = ((FLOW_TOP_COMFORT - topClearance).value / FLOW_PRESSURE_RANGE.value)
            .coerceIn(0f, 1f)
        val morph by animateFloatAsState(
            targetValue = sideTarget,
            animationSpec = tween(
                durationMillis = FLOW_MORPH_MS,
                easing = CubicBezierEasing(0f, .9f, .1f, 1f)
            )
        )

        // A watermark, not a boundary. The pills are the command tree; they are deliberately free
        // to cross this label and every other imaginary pane boundary in the shared workspace.
        Text(
            "01 — COMMAND TREE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Azphalt.currentGround.onPage.copy(alpha = .34f),
                fontSize = 9.sp
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 4.dp)
        )

        commandTreeContent(
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollConnection)
        )

        if (hasBuffer) {
            val outerMargin = 20.dp
            val topWidth = (maxWidth - outerMargin * 2f).coerceAtLeast(FLOW_MIN_BUFFER_WIDTH)
            val topHeight = (topClearance - 8.dp)
                .coerceAtLeast(FLOW_MIN_BUFFER_HEIGHT)
                .coerceAtMost(maxHeight)

            // Root pills structurally stop at 66% of the screen and child bands stop slightly
            // earlier. A 32% side pocket therefore follows the command tree's real geometry while
            // leaving a small breathing seam between the two systems.
            val sideWidth = (maxWidth * FLOW_SIDE_FRACTION - 12.dp)
                .coerceAtLeast(FLOW_MIN_BUFFER_WIDTH)
                .coerceAtMost(topWidth)
            val sideX = (maxWidth - sideWidth - 12.dp).coerceAtLeast(outerMargin)
            val sideHeight = (maxHeight - FLOW_ANCHOR_HEIGHT)
                .coerceAtLeast(FLOW_MIN_BUFFER_HEIGHT)
                .coerceAtMost(maxHeight)

            val bubbleX = lerp(outerMargin, sideX, morph)
            val bubbleWidth = lerp(topWidth, sideWidth, morph)
            val bubbleHeight = lerp(topHeight, sideHeight, morph)

            Column(
                Modifier
                    .offset(x = bubbleX)
                    .width(bubbleWidth)
                    .height(bubbleHeight)
                    .clip(RoundedCornerShape(FLOW_CORNER_RADIUS))
                    .background(Azphalt.Ink.copy(alpha = .055f))
            ) {
                Text(
                    "00 — BUFFER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Azphalt.currentGround.onPage.copy(alpha = .58f),
                        fontSize = 9.sp
                    ),
                    modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 8.dp)
                )
                bufferContent()
            }
        }
    }
}

private fun lerp(start: Dp, end: Dp, fraction: Float): Dp =
    start + (end - start) * fraction

private val FLOW_ROW_PITCH = 20.dp
private val FLOW_ANCHOR_HEIGHT = 36.dp
private val FLOW_TOP_COMFORT = 220.dp
private val FLOW_PRESSURE_RANGE = 120.dp
private val FLOW_MIN_BUFFER_HEIGHT = 78.dp
private val FLOW_MIN_BUFFER_WIDTH = 104.dp
private val FLOW_CORNER_RADIUS = 28.dp
private const val FLOW_SIDE_FRACTION = .32f
private const val FLOW_MORPH_MS = 420
