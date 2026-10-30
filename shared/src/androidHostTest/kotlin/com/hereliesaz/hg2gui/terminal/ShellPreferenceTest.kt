package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShellPreferenceTest {

    @Test
    fun nothingInstalledOffersNoShellInsteadOfAPhantomBash() {
        assertEquals(emptyList<String>(), ShellPreference.availableFrom(bash = false, zsh = false, fish = false))
    }

    @Test
    fun installedShellsAreListedInPickerOrder() {
        assertEquals(
            listOf(ShellPreference.BASH, ShellPreference.ZSH, ShellPreference.FISH),
            ShellPreference.availableFrom(bash = true, zsh = true, fish = true)
        )
        assertEquals(
            listOf(ShellPreference.ZSH),
            ShellPreference.availableFrom(bash = false, zsh = true, fish = false)
        )
    }

    @Test
    fun storedShellIsKeptWhileItIsStillInstalled() {
        val available = listOf(ShellPreference.BASH, ShellPreference.ZSH)
        assertEquals(ShellPreference.ZSH, ShellPreference.effectiveSelection(ShellPreference.ZSH, available))
    }

    @Test
    fun removedStoredShellFallsBackToTheFirstAvailableOne() {
        val available = listOf(ShellPreference.BASH)
        assertEquals(ShellPreference.BASH, ShellPreference.effectiveSelection(ShellPreference.FISH, available))
    }

    @Test
    fun noInstalledShellMeansNoSelection() {
        assertNull(ShellPreference.effectiveSelection(ShellPreference.BASH, emptyList()))
    }
}
