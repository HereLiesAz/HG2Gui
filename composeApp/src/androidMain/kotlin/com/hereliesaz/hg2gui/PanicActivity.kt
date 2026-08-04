package com.hereliesaz.hg2gui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hereliesaz.hg2gui.ui.PanicScreen
import com.hereliesaz.hg2gui.ui.theme.HG2GuiTheme

class PanicActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)

        setContent {
            HG2GuiTheme {
                PanicScreen(
                    errorMessage = errorMessage,
                    onCalm = { finish() }
                )
            }
        }
    }
}
