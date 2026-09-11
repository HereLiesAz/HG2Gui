package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageLifecycleStoreTest {
    @Test
    fun initialization_doesNotCompileAndroidIncompatibleRegexes() {
        val pkg = PackageLifecycleStore.InstalledPackage(
            manager = "pkg",
            managerLabel = "Termux / pkg",
            name = "example",
            version = "1.0",
            binaries = listOf("example"),
            disabled = false,
            isolated = false
        )

        assertEquals("pkg install 'example'", PackageLifecycleStore.updateCommand(pkg))
    }
}
