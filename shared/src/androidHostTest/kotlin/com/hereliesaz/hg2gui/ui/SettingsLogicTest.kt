package com.hereliesaz.hg2gui.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {

    @Test
    fun valuesBelowTheFloorAreLiftedToIt() {
        assertEquals(FONT_SCALE_MIN_PERCENT, clampFontScalePercent(50))
        assertEquals(FONT_SCALE_MIN_PERCENT, clampFontScalePercent(FONT_SCALE_MIN_PERCENT - 1))
    }

    @Test
    fun valuesInsideTheRangeAreUntouched() {
        assertEquals(100, clampFontScalePercent(100))
        assertEquals(FONT_SCALE_MIN_PERCENT, clampFontScalePercent(FONT_SCALE_MIN_PERCENT))
        assertEquals(FONT_SCALE_MAX_PERCENT, clampFontScalePercent(FONT_SCALE_MAX_PERCENT))
    }

    @Test
    fun valuesAboveTheCeilingAreCapped() {
        assertEquals(FONT_SCALE_MAX_PERCENT, clampFontScalePercent(500))
    }

    @Test
    fun steppingMovesByTenAndStaysInRange() {
        assertEquals(110, steppedFontScalePercent(100, +1))
        assertEquals(90, steppedFontScalePercent(100, -1))
        assertEquals(FONT_SCALE_MIN_PERCENT, steppedFontScalePercent(FONT_SCALE_MIN_PERCENT, -1))
        assertEquals(FONT_SCALE_MAX_PERCENT, steppedFontScalePercent(FONT_SCALE_MAX_PERCENT, +1))
    }

    @Test
    fun steppingUpFromALegacyValueBelowTheFloorLandsInsideTheRange() {
        // 50% was a legal stored value before the floor; "+" must not walk it through 60, 70, 80.
        assertEquals(FONT_SCALE_MIN_PERCENT, steppedFontScalePercent(50, +1))
    }

    @Test
    fun theDefaultSitsInsideTheAllowedRange() {
        assertTrue(FONT_SCALE_DEFAULT_PERCENT in FONT_SCALE_MIN_PERCENT..FONT_SCALE_MAX_PERCENT)
    }

    @Test
    fun apiKeyDraftIsTrimmedAndBlankBecomesNull() {
        assertEquals("sk-ant-abc", normalizedApiKey("  sk-ant-abc\n"))
        assertNull(normalizedApiKey("   "))
        assertNull(normalizedApiKey(""))
    }

    @Test
    fun unchangedDraftIsNotDirtyEvenWithTrailingWhitespace() {
        assertFalse(isApiKeyDraftDirty("sk-ant-abc", "sk-ant-abc "))
        assertFalse(isApiKeyDraftDirty(null, ""))
        assertFalse(isApiKeyDraftDirty(null, "   "))
    }

    @Test
    fun editedAddedOrClearedKeyIsDirty() {
        assertTrue(isApiKeyDraftDirty("sk-ant-abc", "sk-ant-abd"))
        assertTrue(isApiKeyDraftDirty(null, "sk-ant-abc"))
        assertTrue(isApiKeyDraftDirty("sk-ant-abc", ""))
    }
}
