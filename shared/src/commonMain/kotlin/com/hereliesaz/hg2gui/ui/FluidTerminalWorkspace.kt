package com.hereliesaz.hg2gui.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.PillMetrics
import com.hereliesaz.hg2gui.ui.theme.LocalAzphaltType

/**
 * Shared terminal workspace with explicit gesture ownership.
 *
 * The buffer and command tree never occupy the same hit-test rectangle. A short pill stack owns a
 * bounded strip at the bottom while the buffer owns the clear space above it. The stack is
 * scrollable, so a long stack is capped (see [planWorkspace]) rather than allowed to squeeze the
 * buffer out. Only a window that is both wide and short - a landscape phone - switches atomically to
 * side-by-side regions: pills on the left, buffer on the right. A portrait phone never does, even
 * with the keyboard up: a column a hundred-odd dp wide is not a usable buffer, so the caller
 * collapses its other chrome instead. There is deliberately no animated intermediate geometry
 * because an interpolated overlap would make gesture ownership ambiguous for a few frames even if
 * the final layouts were correct.
 *
 * This is more than a drawing rule. PillMenu receives a Modifier whose layout bounds are exactly
 * the command region, so its vertical scroll detector cannot win a gesture that began in the
 * buffer. Conversely the buffer's LazyColumn exists only inside the buffer region and cannot steal
 * a drag that began over the pill stack. clipToBounds() also prevents either surface from drawing a
 * child into the other surface and creating a visual target whose touch target belongs elsewhere.
 *
 * [bufferExpanded] (an output card the user chose to expand) grows the buffer to fill the whole
 * workspace, along the axis the two regions share: vertically in the stacked layout, horizontally in
 * the side layout. The command region (pills and breadcrumb) is pushed offscreen by exactly the
 * distance the buffer grows, so the two still never overlap on any animation frame.
 */
@Suppress("LongMethod")
@Composable
internal fun FluidTerminalWorkspace(
    hasBuffer: Boolean,
    estimatedPillRows: Int,
    bufferExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    bufferContent: @Composable ColumnScope.() -> Unit,
    commandTreeContent: @Composable (Modifier) -> Unit
) {
    // The command tree appears at a different spot in the tree for each layout (none / side /
    // stacked). Without movableContentOf, crossing a layout boundary - e.g. the first command
    // output arriving, or rotating - disposes the whole PillMenu and its navigation state and
    // builds a new one. rememberUpdatedState lets the one movable instance follow the caller's
    // latest lambda instead of being rebuilt whenever that lambda is re-created.
    val latestCommandTree by rememberUpdatedState(commandTreeContent)
    val movableCommandTree = remember { movableContentOf { m: Modifier -> latestCommandTree(m) } }
    val expansion by animateFloatAsState(
        targetValue = if (bufferExpanded && hasBuffer) 1f else 0f,
        animationSpec = tween(FLOW_EXPAND_MS)
    )
    BoxWithConstraints(modifier.fillMaxWidth().clipToBounds()) {
        when (val plan = planWorkspace(maxWidth, maxHeight, estimatedPillRows, hasBuffer)) {
            WorkspacePlan.CommandOnly -> CommandRegion(
                modifier = Modifier.fillMaxSize(),
                commandTreeContent = movableCommandTree
            )

            is WorkspacePlan.Side -> {
                val commandWidth = plan.commandWidth
                val push = (commandWidth + FLOW_SEAM) * expansion
                val bufferWidth = (maxWidth - commandWidth - FLOW_SEAM) + push

                CommandRegion(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = -push)
                        .width(commandWidth)
                        .fillMaxHeight(),
                    commandTreeContent = movableCommandTree
                )

                BufferRegion(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .width(bufferWidth)
                        .fillMaxHeight(),
                    bufferContent = bufferContent
                )
            }

            is WorkspacePlan.Stacked -> {
                val push = (plan.commandHeight + FLOW_SEAM) * expansion
                val bufferHeight = (plan.bufferHeight + push).coerceAtMost(maxHeight)

                // A workspace squeezed below a readable buffer (keyboard up on a small phone) shows
                // the pill stack alone rather than a sliver of output; the buffer's scroll state
                // lives in the session, so nothing is lost when it comes back.
                if (bufferHeight >= FLOW_MIN_RENDERED_BUFFER_HEIGHT) {
                    BufferRegion(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = FLOW_OUTER_MARGIN)
                            .width((maxWidth - FLOW_OUTER_MARGIN * 2f).coerceAtLeast(FLOW_MIN_BUFFER_WIDTH))
                            .height(bufferHeight),
                        bufferContent = bufferContent
                    )
                }

                CommandRegion(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(y = push)
                        .fillMaxWidth()
                        .height(plan.commandHeight),
                    commandTreeContent = movableCommandTree
                )
            }
        }
    }
}

/** How [FluidTerminalWorkspace] divides its window; computed by [planWorkspace]. */
internal sealed interface WorkspacePlan {
    /** No output yet: the command tree takes everything. */
    object CommandOnly : WorkspacePlan

    /** Buffer above, command tree below. [bufferHeight] may be too small to draw. */
    data class Stacked(val commandHeight: Dp, val bufferHeight: Dp) : WorkspacePlan

    /** Command tree on the left at [commandWidth], buffer on the right. */
    data class Side(val commandWidth: Dp) : WorkspacePlan
}

/**
 * The layout decision as pure arithmetic over the window size. Each pill row needs
 * [PillMetrics.RowPitch] (its hit box), so a tall stack asks for more than a phone has - the stack
 * scrolls, so the command region is capped at [FLOW_MAX_COMMAND_FRACTION] of the height (never
 * below one anchor row) instead of pushing the buffer out or forcing the side layout.
 */
internal fun planWorkspace(width: Dp, height: Dp, estimatedPillRows: Int, hasBuffer: Boolean): WorkspacePlan {
    if (!hasBuffer) return WorkspacePlan.CommandOnly
    val rowCount = estimatedPillRows.coerceAtLeast(1)
    val requested = (FLOW_ANCHOR_HEIGHT + PillMetrics.RowPitch * (rowCount - 1).toFloat() + FLOW_TOUCH_ALLOWANCE)
        .coerceAtMost(height)

    val wideEnough = width >= FLOW_SIDE_MIN_WIDTH
    val shortEnough = height <= FLOW_SIDE_MAX_HEIGHT
    if (wideEnough && shortEnough) {
        val commandWidth = (width * FLOW_COMMAND_SIDE_FRACTION)
            .coerceIn(FLOW_MIN_COMMAND_WIDTH, FLOW_MAX_COMMAND_WIDTH)
            .coerceAtMost(width - FLOW_MIN_SIDE_BUFFER_WIDTH - FLOW_SEAM)
        return WorkspacePlan.Side(commandWidth)
    }

    val leaveBuffer = height - FLOW_MIN_BUFFER_HEIGHT - FLOW_SEAM
    val cap = maxOf(FLOW_ANCHOR_HEIGHT, minOf(height * FLOW_MAX_COMMAND_FRACTION, leaveBuffer))
    val commandHeight = requested.coerceAtMost(cap).coerceAtLeast(minOf(FLOW_ANCHOR_HEIGHT, height))
    val bufferHeight = (height - commandHeight - FLOW_SEAM).coerceAtLeast(0.dp)
    return WorkspacePlan.Stacked(commandHeight, bufferHeight)
}

@Composable
private fun CommandRegion(
    modifier: Modifier,
    commandTreeContent: @Composable (Modifier) -> Unit
) {
    Box(modifier.clipToBounds()) {
        Text(
            "01 — COMMAND TREE",
            style = LocalAzphaltType.current.eyebrow.copy(color = statusColor(StatusKind.Muted)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 4.dp)
                .semantics { contentDescription = "Command tree" }
        )
        commandTreeContent(Modifier.fillMaxSize())
    }
}

@Composable
private fun BufferRegion(
    modifier: Modifier,
    bufferContent: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(FLOW_CORNER_RADIUS))
            .clipToBounds()
            .background(Azphalt.Ink.copy(alpha = .055f))
    ) {
        Text(
            "00 — BUFFER",
            style = LocalAzphaltType.current.eyebrow.copy(color = statusColor(StatusKind.Muted)),
            modifier = Modifier
                .padding(start = 12.dp, top = 10.dp, bottom = 8.dp)
                .semantics { contentDescription = "Output buffer" }
        )
        bufferContent()
    }
}

// An anchor row is one 48dp hit box plus the 12dp above and below it that TerminalScreen's
// PillMenu modifier pads the region with.
private val FLOW_ANCHOR_HEIGHT = PillMetrics.TouchTarget + 24.dp
private val FLOW_TOUCH_ALLOWANCE = 8.dp
private val FLOW_SEAM = 8.dp
private val FLOW_OUTER_MARGIN = 20.dp
private val FLOW_MIN_BUFFER_HEIGHT = 96.dp
private val FLOW_MIN_RENDERED_BUFFER_HEIGHT = 48.dp
private val FLOW_MIN_BUFFER_WIDTH = 104.dp
private val FLOW_MIN_COMMAND_WIDTH = 240.dp
private val FLOW_MAX_COMMAND_WIDTH = 360.dp
private val FLOW_MIN_SIDE_BUFFER_WIDTH = 280.dp
private val FLOW_SIDE_MIN_WIDTH = 600.dp
private val FLOW_SIDE_MAX_HEIGHT = 300.dp
private val FLOW_CORNER_RADIUS = 28.dp
private const val FLOW_COMMAND_SIDE_FRACTION = .45f
private const val FLOW_MAX_COMMAND_FRACTION = .6f
private const val FLOW_EXPAND_MS = 320
