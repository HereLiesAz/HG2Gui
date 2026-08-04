package com.hereliesaz.hg2gui.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OrigamiScreen() {
    val origamiPrimary = Color(0xFF13EC6A)
    val origamiBg = Color(0xFF102217)
    val unfoldProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        unfoldProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(origamiBg)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xE6102217))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(origamiPrimary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text("📁", color = origamiPrimary, textAlign = TextAlign.Center)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = "SYSTEM_READY // VARIANT_03",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ORIGAMI_MODULE_ACTIVE",
                    color = origamiPrimary.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }

            Text(
                text = "HG2GUI",
                color = origamiPrimary,
                modifier = Modifier
                    .background(origamiPrimary.copy(alpha = 0.1f))
                    .padding(4.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Headline
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "> ", color = origamiPrimary)
                    Text(
                        text = "EXECUTE_UNFOLD.EXE_",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(1.dp)
                        .background(origamiPrimary.copy(alpha = 0.5f))
                )
            }

            // Unfold Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .graphicsLayer {
                        scaleY = unfoldProgress.value
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .border(1.dp, origamiPrimary)
                    .background(origamiPrimary.copy(alpha = 0.05f))
                    .padding(bottom = 24.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📁", fontSize = 48.sp, color = origamiPrimary)
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(origamiPrimary.copy(alpha = 0.05f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATUS: SEQUENCE_COMPLETE",
                        color = origamiPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(14.dp).background(origamiPrimary))
                }
                
                // Corner decors (simplified)
                Box(modifier = Modifier.size(16.dp).align(Alignment.TopStart).drawCorner(origamiPrimary, true, true))
                Box(modifier = Modifier.size(16.dp).align(Alignment.TopEnd).drawCorner(origamiPrimary, true, false))
                Box(modifier = Modifier.size(16.dp).align(Alignment.BottomStart).drawCorner(origamiPrimary, false, true))
                Box(modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).drawCorner(origamiPrimary, false, false))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu List
            MenuItem(text = "[ NAVIGATION ]", primaryColor = origamiPrimary)
            MenuItem(text = "[ SENSOR_ARRAY ]", primaryColor = origamiPrimary)
            MenuItem(text = "[ COMM_UPLINK ]", primaryColor = origamiPrimary)
        }

        // Footer
        Column(
            modifier = Modifier
                .background(Color(0xCC102217))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("MEM:", color = origamiPrimary, modifier = Modifier.weight(1f))
                Text("64KB", color = Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("UPTIME:", color = origamiPrimary, modifier = Modifier.weight(1f))
                Text("04:22:19", color = Color.White)
            }
            Text(
                text = "HG2GUI_V3.0 // ORIGAMI_MODULE",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = TextAlign.Center,
                color = origamiPrimary.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MenuItem(text: String, primaryColor: Color) {
    Button(
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )
    }
}

private fun Modifier.drawCorner(color: Color, top: Boolean, left: Boolean): Modifier = this.graphicsLayer {
    // This is a simplification. For real corners, use DrawScope
}
