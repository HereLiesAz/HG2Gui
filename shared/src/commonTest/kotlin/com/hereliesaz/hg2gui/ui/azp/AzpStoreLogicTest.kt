package com.hereliesaz.hg2gui.ui.azp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AzpStoreLogicTest {

    private fun listing(
        id: String,
        kind: String = "code",
        installed: Boolean = false,
        name: String = id,
        description: String = "",
        author: String = "someone"
    ) = AzpListing(
        id = id, name = name, author = author, description = description,
        version = "1.0", kind = kind, installed = installed
    )

    @Test
    fun installedPackagesAreShownWithoutAnySearch() {
        val installed = listOf(listing("a", installed = true), listing("b", installed = true))
        val shown = azpInstalledToShow(installed, results = emptyList(), query = "", kind = "all")
        assertEquals(listOf("a", "b"), shown.map { it.id })
    }

    @Test
    fun aPackageAlreadyInTheResultsIsNotListedTwice() {
        val installed = listOf(listing("a", installed = true), listing("b", installed = true))
        val results = listOf(listing("a", installed = true))
        assertEquals(listOf("b"), azpInstalledToShow(installed, results, "", "all").map { it.id })
    }

    @Test
    fun kindChipNarrowsTheInstalledList() {
        val installed = listOf(listing("a", kind = "skill", installed = true), listing("b", kind = "script", installed = true))
        assertEquals(listOf("b"), azpInstalledToShow(installed, emptyList(), "", "script").map { it.id })
    }

    @Test
    fun queryMatchesNameIdDescriptionAndAuthorIgnoringCase() {
        val installed = listOf(
            listing("one", name = "Git Helper", installed = true),
            listing("two", description = "Formats JSON", installed = true),
            listing("three", author = "Ada", installed = true),
            listing("four", installed = true)
        )
        assertEquals(listOf("one"), azpInstalledToShow(installed, emptyList(), "git", "all").map { it.id })
        assertEquals(listOf("two"), azpInstalledToShow(installed, emptyList(), " json ", "all").map { it.id })
        assertEquals(listOf("three"), azpInstalledToShow(installed, emptyList(), "ADA", "all").map { it.id })
        assertEquals(listOf("four"), azpInstalledToShow(installed, emptyList(), "FOUR", "all").map { it.id })
    }

    @Test
    fun entriesFromTheInstalledListAreMarkedInstalled() {
        val stale = listing("a", installed = false)
        assertTrue(azpInstalledToShow(listOf(stale), emptyList(), "", "all").single().installed)
    }

    @Test
    fun onlyNotYetInstalledSkillsAskForConfirmation() {
        assertTrue(azpNeedsInstallConfirmation(listing("s", kind = "skill")))
        assertFalse(azpNeedsInstallConfirmation(listing("s", kind = "skill", installed = true)))
        assertFalse(azpNeedsInstallConfirmation(listing("m", kind = "mcp")))
        assertFalse(azpNeedsInstallConfirmation(listing("x", kind = "script")))
    }

    @Test
    fun rowsSuppressEveryEmptyState() {
        assertEquals(AzpEmptyState.None, azpEmptyState(hasRows = true, busy = true, searched = true, hasError = true))
    }

    @Test
    fun aRunningSearchWithNoRowsSaysSearching() {
        assertEquals(AzpEmptyState.Searching, azpEmptyState(hasRows = false, busy = true, searched = true, hasError = false))
    }

    @Test
    fun anErrorBannerReplacesTheEmptyNote() {
        assertEquals(AzpEmptyState.None, azpEmptyState(hasRows = false, busy = false, searched = true, hasError = true))
    }

    @Test
    fun beforeAnySearchTheStoreShowsTheIdleHint() {
        assertEquals(AzpEmptyState.Idle, azpEmptyState(hasRows = false, busy = false, searched = false, hasError = false))
    }

    @Test
    fun anEmptyAnswerSaysNoResults() {
        assertEquals(AzpEmptyState.NoResults, azpEmptyState(hasRows = false, busy = false, searched = true, hasError = false))
    }
}
