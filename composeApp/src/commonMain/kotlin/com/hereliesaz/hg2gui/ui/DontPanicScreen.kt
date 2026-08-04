package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DontPanicScreen(error: String?, onFinish: () -> Unit) {
    val bgDark = Color(0xFF101E23)
    val accentBlue = Color(0xFF0DB9F2)
    val textRed = Color(0xFFEF4444)
    val buttonBg = Color(0xFF182D34)
    val buttonSelectedBg = Color(0xFF253941)

    var selectedIndex by remember { mutableStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .clickable { onFinish() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DON'T PANIC",
                color = accentBlue,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
            )

            if (error != null) {
                Text(
                    text = "> ERROR: $error",
                    color = textRed,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                PanicButton(
                    label = "TOWEL",
                    icon = "🧺",
                    isSelected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    modifier = Modifier.weight(1f),
                    buttonBg = buttonBg,
                    selectedBg = buttonSelectedBg,
                    accentColor = accentBlue
                )
                Spacer(modifier = Modifier.width(16.dp))
                PanicButton(
                    label = "TEA",
                    icon = "☕",
                    isSelected = selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    modifier = Modifier.weight(1f),
                    buttonBg = buttonBg,
                    selectedBg = buttonSelectedBg,
                    accentColor = accentBlue
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                PanicButton(
                    label = "THUMB",
                    icon = "👍",
                    isSelected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    modifier = Modifier.weight(1f),
                    buttonBg = buttonBg,
                    selectedBg = buttonSelectedBg,
                    accentColor = accentBlue
                )
                Spacer(modifier = Modifier.width(16.dp))
                PanicButton(
                    label = "TIME",
                    icon = "⌛",
                    isSelected = selectedIndex == 3,
                    onClick = { selectedIndex = 3 },
                    modifier = Modifier.weight(1f),
                    buttonBg = buttonBg,
                    selectedBg = buttonSelectedBg,
                    accentColor = accentBlue
                )
            }
        }
    }
}

@Composable
private fun PanicButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonBg: Color,
    selectedBg: Color,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .background(if (isSelected) selectedBg else buttonBg, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icon, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
