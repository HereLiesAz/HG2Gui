package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hereliesaz.hg2gui.ui.DontPanicScreen
import com.hereliesaz.hg2gui.ui.theme.HG2GuiTheme

class DontPanicActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR = "error_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val error = intent.getStringExtra(EXTRA_ERROR)

        setContent {
            HG2GuiTheme {
                DontPanicScreen(
                    error = error,
                    onFinish = { finish() }
                )
            }
        }
    }
}
