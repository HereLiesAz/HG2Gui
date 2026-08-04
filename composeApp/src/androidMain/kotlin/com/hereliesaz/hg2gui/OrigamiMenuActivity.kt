package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hereliesaz.hg2gui.ui.OrigamiScreen
import com.hereliesaz.hg2gui.ui.theme.HG2GuiTheme

class OrigamiMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            HG2GuiTheme {
                OrigamiScreen()
            }
        }
    }
}
