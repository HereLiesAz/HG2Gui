package com.hereliesaz.hg2gui.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.onPage

/**
 * Shared terminal workspace with explicit gesture ownership.
 *
 * The buffer and command tree never occupy the same hit-test rectangle. A short pill stack owns a
 * bounded strip at the bottom while the buffer owns the clear space above it. If the stack becomes
 * too tall to leave a useful buffer above, the workspace switches atomically to side-by-side
 * regions: pills on the left, buffer on the right. There is deliberately no animated intermediate
 * geometry because an interpolated overlap would make gesture ownership ambiguous for a few
 * frames even if the final layouts were correct.
 *
 * This is more than a drawing rule. PillMenu receives a Modifier whose layout bounds are exactly
 * the command region, so its vertical scroll detector cannot win a gesture that began in the
 * buffer. Conversely the buffer's LazyColumn exists only inside the buffer region and cannot steal
 * a drag that began over the pill stack. clipToBounds() also prevents either surface from drawing a
 * child into the other surface and creating a visual target whose touch target belongs elsewhere.
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
        val rowCount = estimatedPillRows.coerceAtLeast(1)
        val requestedCommandHeight = (
            FLOW_ANCHOR_HEIGHT +
                FLOW_ROW_PITCH * (rowCount - 1).toFloat() +
                FLOW_TOUCH_ALLOWANCE
            ).coerceAtMost(maxHeight)

        if (!hasBuffer) {
            CommandRegion(
                modifier = Modifier.fillMaxSize(),
                commandTreeContent = commandTreeContent
            )
            return@BoxWithConstraints
        }

        val topBufferHeight = maxHeight - requestedCommandHeight - FLOW_SEAM
        val sideLayoutFits = maxWidth >= FLOW_MIN_COMMAND_WIDTH + FLOW_MIN_BUFFER_WIDTH + FLOW_SEAM
        val useSideLayout = topBufferHeight < FLOW_MIN_BUFFER_HEIGHT && sideLayoutFits

        if (useSideLayout) {
            val commandWidth = (maxWidth * FLOW_COMMAND_SIDE_FRACTION)
                .coerceAtLeast(FLOW_MIN_COMMAND_WIDTH)
                .coerceAtMost(maxWidth - FLOW_MIN_BUFFER_WIDTH - FLOW_SEAM)
            val bufferWidth = (maxWidth - commandWidth - FLOW_SEAM)
                .coerceAtLeast(FLOW_MIN_BUFFER_WIDTH)

            CommandRegion(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(commandWidth)
                    .fillMaxHeight(),
                commandTreeContent = commandTreeContent
            )

            BufferRegion(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(bufferWidth)
                    .fillMaxHeight(),
                bufferContent = bufferContent
            )
        } else {
            val maximumCommandHeight = (maxHeight - FLOW_MIN_BUFFER_HEIGHT - FLOW_SEAM)
                .coerceAtLeast(FLOW_ANCHOR_HEIGHT)
            val commandHeight = requestedCommandHeight.coerceAtMost(maximumCommandHeight)
            val bufferHeight = (maxHeight - commandHeight - FLOW_SEAM)
                .coerceAtLeast(FLOW_MIN_BUFFER_HEIGHT)
                .coerceAtMost(maxHeight)

            BufferRegion(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = FLOW_OUTER_MARGIN)
                    .width((maxWidth - FLOW_OUTER_MARGIN * 2f).coerceAtLeast(FLOW_MIN_BUFFER_WIDTH))
                    .height(bufferHeight),
                bufferContent = bufferContent
            )

            CommandRegion(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(commandHeight),
                commandTreeContent = commandTreeContent
            )
        }
    }
}

@Composable
private fun CommandRegion(
    modifier: Modifier,
    commandTreeContent: @Composable (Modifier) -> Unit
) {
    Box(modifier.clipToBounds()) {
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
            style = MaterialTheme.typography.labelSmall.copy(
                color = Azphalt.currentGround.onPage.copy(alpha = .58f),
                fontSize = 9.sp
            ),
            modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 8.dp)
        )
        bufferContent()
    }
}

private val FLOW_ROW_PITCH = 20.dp
private val FLOW_ANCHOR_HEIGHT = 36.dp
private val FLOW_TOUCH_ALLOWANCE = 24.dp
private val FLOW_SEAM = 8.dp
private val FLOW_OUTER_MARGIN = 20.dp
private val FLOW_MIN_BUFFER_HEIGHT = 78.dp
private val FLOW_MIN_BUFFER_WIDTH = 104.dp
private val FLOW_MIN_COMMAND_WIDTH = 180.dp
private val FLOW_CORNER_RADIUS = 28.dp
private const val FLOW_COMMAND_SIDE_FRACTION = .68f
