package com.hereliesaz.hg2gui.ui.menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileBrowserTest {

    @Test
    fun pickerNode_isAWizardThatSettlesFirst() {
        val node = FileBrowser.pickerNode("cat/file")
        assertTrue(node.settleBeforeWizard)
        assertTrue(node.wizardId!!.startsWith(FileBrowser.WIZARD_PREFIX))
        assertEquals(false, node.emitsToken)
    }

    @Test
    fun wizardId_roundTripsTheNodesOwnId() {
        // TerminalActivity.onWizard only ever receives the wizardId string, not the MenuNode
        // itself - it recovers the crumb id (to look up that crumb's captured rect) by stripping
        // WIZARD_PREFIX back off. This locks that round trip in.
        val id = "sh/apt-get/file"
        val node = FileBrowser.pickerNode(id)
        val recovered = node.wizardId!!.removePrefix(FileBrowser.WIZARD_PREFIX)
        assertEquals(id, recovered)
    }
}
