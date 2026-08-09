package com.hereliesaz.hg2gui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientTest {

    @Test
    fun plainCommand_noParts() {
        val reply = AiClient.parse("CMD:ls -la")
        assertEquals("ls -la", reply.command)
        assertTrue(reply.parts.isEmpty())
    }

    @Test
    fun commandWithParts_isParsed() {
        val raw = """
            CMD:tar -xzf archive.tar.gz -C ~/out
            PARTS:
            -x|Extract.
            -z|Run it through gzip on the way out.
            -f|The next word is the file.
            -C|Change to this directory first.
        """.trimIndent()
        val reply = AiClient.parse(raw)
        assertEquals("tar -xzf archive.tar.gz -C ~/out", reply.command)
        assertEquals(4, reply.parts.size)
        assertEquals("-x" to "Extract.", reply.parts[0])
        assertEquals("-C" to "Change to this directory first.", reply.parts[3])
    }

    @Test
    fun textReply_noParts() {
        val reply = AiClient.parse("TEXT:tar bundles files, it doesn't compress on its own.")
        assertEquals(null, reply.command)
        assertTrue(reply.parts.isEmpty())
    }
}
