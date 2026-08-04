package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hereliesaz.hg2gui.ui.GuideScreen
import com.hereliesaz.hg2gui.ui.theme.HG2GuiTheme

class GuideActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            HG2GuiTheme {
                GuideScreen(
                    onExit = {
                        finish()
                        overridePendingTransition(0, 0)
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(0, 0)
    }
}
