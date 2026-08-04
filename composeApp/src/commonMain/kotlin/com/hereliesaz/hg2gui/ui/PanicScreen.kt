package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PanicScreen(errorMessage: String?, onCalm: () -> Unit) {
    val panicBg = Color(0xFF221010)
    val panicCardBg = Color(0x33F20D0D)
    val textPrimary = Color(0xFFFEE2E2)
    val textSecondary = Color(0xFFF87171)
    val textWarning = Color(0xFFFBBF24)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(panicBg)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xE64A0000))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(panicCardBg, RoundedCornerShape(4.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SYSTEM CRITICAL",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "CODE: RED-42",
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(panicCardBg, RoundedCornerShape(4.dp))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Headline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DON'T",
                    color = textWarning,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.rotate(-2f)
                )
                Text(
                    text = "PANIC",
                    color = textWarning,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .rotate(3f)
                        .offset(y = (-10).dp)
                )
                Text(
                    text = "Guide Mk.II Panic Protocols Engaged",
                    color = Color(0xFFFECACA),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(panicCardBg, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            }

            // Visual Sensors
            Text(
                text = "Visual Sensors // Hazard Detection",
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                SensorItem(name = "OBJ: TOWEL", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                SensorItem(name = "OBJ: POT", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                SensorItem(name = "OBJ: WHALE", modifier = Modifier.weight(1f))
            }

            // Terminal Output
            Text(
                text = "Terminal Output",
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "> ERR: ${errorMessage ?: "Unknown system failure."}",
                    color = Color(0xFFEF4444),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "> WARN: Tea supplies exhausted.",
                    color = textWarning,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "> SYS: Deep Thought calculating...",
                    color = Color(0xFF0DB9F2),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }

        // Bottom Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onCalm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFBBF24)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "INITIATE CALM",
                    color = Color(0xFF101E23),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "PRESS TO RESTORE FACTORY SERENITY",
                color = textSecondary.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SensorItem(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0x33F20D0D), RoundedCornerShape(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder for icon
            Text(text = "📷", fontSize = 24.sp)
        }
        Text(
            text = name,
            color = Color(0xFFFECACA),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp),
            textAlign = TextAlign.Center
        )
    }
}
