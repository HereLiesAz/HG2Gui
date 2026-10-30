package com.hereliesaz.hg2gui.ui.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilesSupportTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100L * day

    @Test
    fun justDeletedSaysTodayAndFullRetention() {
        assertEquals("deleted today - purged in 30 days", trashAgeDescription(now, now))
        // Less than a whole day still counts as today.
        assertEquals("deleted today - purged in 30 days", trashAgeDescription(now - day + 1, now))
    }

    @Test
    fun ageAndTimeLeftAddUpToTheRetentionPeriod() {
        assertEquals("deleted 1 day ago - purged in 29 days", trashAgeDescription(now - day, now))
        assertEquals("deleted 5 days ago - purged in 25 days", trashAgeDescription(now - 5 * day, now))
        assertEquals("deleted 29 days ago - purged in 1 day", trashAgeDescription(now - 29 * day, now))
    }

    @Test
    fun anItemPastRetentionSaysItIsBeingPurged() {
        assertEquals("deleted 30 days ago - purging now", trashAgeDescription(now - 30 * day, now))
        assertEquals("deleted 45 days ago - purging now", trashAgeDescription(now - 45 * day, now))
    }

    @Test
    fun aClockBehindTheDeletionTimeNeverGoesNegative() {
        assertEquals("deleted today - purged in 30 days", trashAgeDescription(now + 3 * day, now))
    }

    @Test
    fun unknownTimestampsFallBackToThePlainRule() {
        val plain = "purged 30 days after it was deleted"
        assertEquals(plain, trashAgeDescription(0L, now))
        assertEquals(plain, trashAgeDescription(now, 0L))
    }

    @Test
    fun daysLabelIsSingularOnlyForOne() {
        assertEquals("1 day", daysLabel(1))
        assertEquals("0 days", daysLabel(0))
        assertEquals("2 days", daysLabel(2))
    }

    @Test
    fun trashConfirmTextsMentionTheAutoPurge() {
        val file = trashConfirmMessage("notes.txt", isDirectory = false)
        val folder = trashConfirmMessage("src", isDirectory = true)
        val batch = trashBatchConfirmMessage()
        assertTrue("30 days" in file, file)
        assertTrue("30 days" in folder, folder)
        assertTrue("30 days" in batch, batch)
        assertTrue("notes.txt" in file)
        assertTrue("everything inside it" in folder)
    }

    @Test
    fun longNamesSurviveIntoTheConfirmTextWhole() {
        // The dialog body wraps; it must not truncate the name the user is about to trash.
        val name = "x".repeat(60) + ".tar.gz"
        assertTrue(name in trashConfirmMessage(name, isDirectory = false))
    }
}
