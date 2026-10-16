package com.hereliesaz.hg2gui.ui

import com.hereliesaz.hg2gui.ui.ssh.SshFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class FlowQuotingTest {

    @Test
    fun sshArgsQuoteValuesAndEndOptionsBeforeTarget() {
        assertEquals("-- 'me@example.com'", SshFlow.argsFor("me", "example.com", 22, null))
        assertEquals(
            "-p 2222 -i '/k ey' -- 'me@h'",
            SshFlow.argsFor("me", "h", 2222, "/k ey")
        )
        // An option-looking host stays a quoted operand after "--".
        assertEquals("-- '-oProxyCommand=id'", SshFlow.argsFor("", "-oProxyCommand=id", 22, null))
    }

    @Test
    fun workflowValuesAreQuotedAsSingleShellWords() {
        assertEquals(
            "git commit -m 'it'\\''s; rm -rf ~'",
            WorkflowFlow.render("git commit -m {message}", mapOf("message" to "it's; rm -rf ~"))
        )
        assertEquals("echo {missing}", WorkflowFlow.render("echo {missing}", emptyMap()))
    }
}
