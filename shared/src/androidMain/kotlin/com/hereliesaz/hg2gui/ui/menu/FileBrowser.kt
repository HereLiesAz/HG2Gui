package com.hereliesaz.hg2gui.ui.menu

/**
 * The "Select File or Folder" trigger pill. Picking it doesn't drill further into the pill stack
 * - it opens the graphical path picker via [PillPerimeterReveal], anchored to wherever this pick
 * settles into the trail (see [MenuNode.settleBeforeWizard]). [TerminalActivity] resolves the
 * wizard id back to the session's own live working directory as the browsing root.
 */
object FileBrowser {
    const val WIZARD_PREFIX = "pick-path:"

    // The node's own id rides along in the wizardId itself (rather than a bare constant) so the
    // caller can look up this exact crumb's on-screen rect once it's settled - PillMenu only ever
    // hands onWizard the wizardId string, not the node it came from.
    fun pickerNode(id: String): MenuNode = MenuNode(
        id = id,
        label = "file…",
        cap = "PICK",
        emitsToken = false,
        wizardId = "$WIZARD_PREFIX$id",
        settleBeforeWizard = true
    )
}
