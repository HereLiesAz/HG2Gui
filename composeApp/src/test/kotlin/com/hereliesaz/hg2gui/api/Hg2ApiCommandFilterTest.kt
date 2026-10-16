package com.hereliesaz.hg2gui.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Hg2ApiCommandFilterTest {

    @Test
    fun blocksDirectAndDisguisedInvocations() {
        listOf(
            "hg2auth root shell id",
            "bootstrap",
            "env hg2auth root shell id",
            "sh -c 'hg2auth root shell id'",
            "true; hg2auth root shell id",
            "/data/x/bin/hg2auth status",
            "X=1 hg2auth status",
            "echo \$(bootstrap)",
            "HG2AUTH status",
            "h\"g2\"auth status",
        ).forEach { assertTrue(it, containsForegroundOnlyCommand(it)) }
    }

    @Test
    fun allowsOrdinaryCommands() {
        assertFalse(containsForegroundOnlyCommand("ls -la"))
        assertFalse(containsForegroundOnlyCommand("pkg install git"))
        assertFalse(containsForegroundOnlyCommand("cat bootstrap.log"))
    }
}
