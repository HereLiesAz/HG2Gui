package com.hereliesaz.hg2gui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Two soft surfaces exist. Everything else is 999px - see the style guide's "09 - Surfaces"
// section, "There are no cards". Declared once so a screen reaching for a mid-range radius has
// a named shape to reach for instead of inventing one.
object AzphaltSurface {
    val recordTile = RoundedCornerShape(26.dp)
    val note = RoundedCornerShape(20.dp)
    val capsule = RoundedCornerShape(percent = 50)
}
