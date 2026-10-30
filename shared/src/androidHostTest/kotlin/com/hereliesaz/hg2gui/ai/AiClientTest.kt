package com.hereliesaz.hg2gui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun modelId_isTheCurrentOpusId() {
        // claude-opus-5-5 is a valid current model id; this pins that it is not changed by accident.
        assertEquals("claude-opus-5-5", AI_MODEL_ID)
    }

    @Test
    fun a404IsAlwaysAModelRejection() {
        assertTrue(isModelRejection(404, null))
        assertTrue(isModelRejection(404, "anything"))
    }

    @Test
    fun a400IsAModelRejectionOnlyWhenItBlamesTheModel() {
        assertTrue(isModelRejection(400, "model: claude-opus-5-5 is not supported"))
        assertTrue(isModelRejection(400, "The requested Model does not exist"))
        assertFalse(isModelRejection(400, "max_tokens must be positive"))
        assertFalse(isModelRejection(400, null))
    }

    @Test
    fun otherStatusesAreNotModelRejections() {
        for (status in listOf(401, 403, 408, 422, 429, 500, 529)) {
            assertFalse("status $status", isModelRejection(status, "model overloaded"))
        }
    }

    @Test
    fun rejectionMessageNamesTheModelAndKeepsTheApiDetail() {
        val message = modelRejectionMessage("claude-opus-5-5", "not_found_error: model")
        assertTrue(message.contains("claude-opus-5-5"))
        assertTrue(message.contains("not_found_error: model"))
        assertFalse(modelRejectionMessage("m", null).contains("API said"))
        assertFalse(modelRejectionMessage("m", "  ").contains("API said"))
    }
}
