package com.hereliesaz.hg2gui.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.menu.Azphalt
import com.hereliesaz.hg2gui.ui.menu.MenuNode

@Composable
fun PillReturnFormatter(nodes: List<MenuNode>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nodes.forEach { node ->
            PillReturnRow(node)
        }
    }
}

@Composable
private fun PillReturnRow(node: MenuNode) {
    val hue = Azphalt.hueOf(node.id)
    val bg = Azphalt.hues[hue]
    val fg = Azphalt.White
    val capBg = Azphalt.caps[hue]
    val capFg = Azphalt.White

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(Azphalt.Ink.copy(alpha = 0.05f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The main Pill
        Row(
            Modifier
                .height(24.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(bg)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End)
        ) {
            Text(
                text = node.label.uppercase(),
                color = fg,
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.09.em,
                maxLines = 1,
                textAlign = TextAlign.End
            )
            if (node.cap != null) {
                Box(
                    Modifier
                        .height(14.dp)
                        .defaultMinSize(minWidth = 16.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(capBg)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = node.cap.uppercase(),
                        color = capFg,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
        
        Spacer(Modifier.width(8.dp))
        
        // Dynamic nested description or child elements
        if (node.children.isNotEmpty()) {
            Text(
                text = node.children.joinToString(", ") { it.label },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Azphalt.Ink.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            )
        }
    }
}
