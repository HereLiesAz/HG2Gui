package com.hereliesaz.hg2gui.managers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSettingsSaveResultTest {

    @Test
    fun unavailableStoreIsReportedEvenWhenTheKeyIsBlank() {
        assertEquals(
            AiSettings.SaveResult.StoreUnavailable,
            AiSettings.SaveResult.of(storeOpen = false, committed = false, blank = false)
        )
        assertEquals(
            AiSettings.SaveResult.StoreUnavailable,
            AiSettings.SaveResult.of(storeOpen = false, committed = false, blank = true)
        )
    }

    @Test
    fun failedCommitIsAWriteFailure() {
        assertEquals(
            AiSettings.SaveResult.WriteFailed,
            AiSettings.SaveResult.of(storeOpen = true, committed = false, blank = false)
        )
    }

    @Test
    fun successfulWritesDistinguishSavingFromClearing() {
        assertEquals(AiSettings.SaveResult.Saved, AiSettings.SaveResult.of(storeOpen = true, committed = true, blank = false))
        assertEquals(AiSettings.SaveResult.Cleared, AiSettings.SaveResult.of(storeOpen = true, committed = true, blank = true))
    }

    @Test
    fun onlyTheTwoSuccessOutcomesCountAsPersisted() {
        assertTrue(AiSettings.SaveResult.Saved.persisted)
        assertTrue(AiSettings.SaveResult.Cleared.persisted)
        assertFalse(AiSettings.SaveResult.StoreUnavailable.persisted)
        assertFalse(AiSettings.SaveResult.WriteFailed.persisted)
    }
}
