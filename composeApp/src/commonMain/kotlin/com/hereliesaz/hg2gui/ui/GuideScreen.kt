package com.hereliesaz.hg2gui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hereliesaz.hg2gui.ui.theme.*

@Composable
fun GuideScreen(onExit: () -> Unit) {
    var inputText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GuideBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE6111111))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SATELLITE",
                    color = GuidePrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.7f)
                )
                Text(
                    text = " • EARTH 42°N",
                    color = GuidePrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "BAT: 98%",
                    color = GuidePrimary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.alpha(0.7f)
                )
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Don't Panic Circle
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .background(GuideRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DON'T\nPANIC",
                        color = GuideRed, // The original XML had a circle drawable, this is a simplification
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Re-tinting the text to white or similar if needed, 
                // but the XML had guide_red for text. Wait.
                // Let's re-read: FrameLayout background shape_circle_panic, TextView color guide_red.
                // That means red text on a circle. I'll just use a Border for the circle.
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(GuidePrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENCYCLOPEDIA GALACTICA",
                        color = GuidePrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.alpha(0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "The Command Line",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(2.dp)
                        .background(GuidePrimary.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "A primitive yet effective method of conversing with silicon.\n\nImagine a conversation where you must be incredibly precise, or the other party simply stares blankly.\n\nThat is the Command Line. It is the art of typing 'sudo' and feeling like a wizard.",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GuideWarningBg)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "WARNING:",
                        color = GuidePrimary.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mostly harmless, unless you delete the wrong directory.",
                        color = GuidePrimary,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(96.dp))
            }

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE6101E22))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GuidePromptBg)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">",
                        color = GuidePrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.Center
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "QUERY GUIDE...",
                                color = GuidePromptHint,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it.uppercase() },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = GuidePrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(GuidePrimary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Text
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val text = inputText.lowercase().trim()
                                    if (text == "exit" || text == "quit" || text == "back") {
                                        onExit()
                                    } else {
                                        inputText = ""
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
        
        // Scanlines simplification: just a very low alpha overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.05f))
        )
    }
}
