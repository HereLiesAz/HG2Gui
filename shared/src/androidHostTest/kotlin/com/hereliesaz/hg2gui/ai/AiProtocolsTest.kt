package com.hereliesaz.hg2gui.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProtocolsTest {
    private fun obj(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    // -- OpenAI chat completions ---------------------------------------------------------------

    @Test
    fun openAiEndpoint_joinsWithoutDoubleSlash() {
        assertEquals("https://api.openai.com/v1/chat/completions", OpenAiChatProtocol.endpoint("https://api.openai.com/v1"))
        assertEquals("http://127.0.0.1:11434/v1/chat/completions", OpenAiChatProtocol.endpoint("http://127.0.0.1:11434/v1/"))
    }

    @Test
    fun openAiRequest_carriesModelAndBothMessages_andNoTokenLimit() {
        val body = obj(OpenAiChatProtocol.requestBody("m1", "be brief", "list files"))
        assertEquals("m1", body.getValue("model").jsonPrimitive.content)
        val messages = body.getValue("messages").jsonArray
        assertEquals(2, messages.size)
        assertEquals("system", messages[0].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals("be brief", messages[0].jsonObject.getValue("content").jsonPrimitive.content)
        assertEquals("user", messages[1].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals("list files", messages[1].jsonObject.getValue("content").jsonPrimitive.content)
        // Servers disagree on the name (max_tokens / max_completion_tokens), so neither is sent.
        assertFalse(body.containsKey("max_tokens"))
        assertFalse(body.containsKey("max_completion_tokens"))
    }

    @Test
    fun openAiRequest_escapesQuotesAndNewlinesInUserText() {
        val tricky = "say \"hi\"\nthen \\ done"
        val body = obj(OpenAiChatProtocol.requestBody("m", "s", tricky))
        assertEquals(tricky, body.getValue("messages").jsonArray[1].jsonObject.getValue("content").jsonPrimitive.content)
    }

    @Test
    fun openAiReply_readsStringContent() {
        val reply = """{"choices":[{"message":{"role":"assistant","content":"CMD:ls -la"}}]}"""
        assertEquals("CMD:ls -la", OpenAiChatProtocol.extractText(reply))
    }

    @Test
    fun openAiReply_readsContentPartArray() {
        val reply = """{"choices":[{"message":{"content":[{"type":"text","text":"CMD:"},{"type":"text","text":"pwd"}]}}]}"""
        assertEquals("CMD:pwd", OpenAiChatProtocol.extractText(reply))
    }

    @Test
    fun openAiReply_withoutTextIsNull() {
        assertNull(OpenAiChatProtocol.extractText("""{"choices":[]}"""))
        assertNull(OpenAiChatProtocol.extractText("""{"choices":[{"message":{"content":null}}]}"""))
        assertNull(OpenAiChatProtocol.extractText("not json"))
        assertNull(OpenAiChatProtocol.extractText("[]"))
    }

    @Test
    fun errorDetail_readsObjectAndStringForms() {
        assertEquals("bad key", OpenAiChatProtocol.errorDetail("""{"error":{"message":"bad key","type":"x"}}"""))
        assertEquals("rate limited", OpenAiChatProtocol.errorDetail("""{"error":"rate limited"}"""))
        assertNull(OpenAiChatProtocol.errorDetail("""{"error":{"message":"  "}}"""))
        assertNull(OpenAiChatProtocol.errorDetail("""{"ok":true}"""))
        assertNull(OpenAiChatProtocol.errorDetail("<html>502</html>"))
    }

    // -- Gemini --------------------------------------------------------------------------------

    @Test
    fun geminiEndpoint_namesTheModel_andNeverCarriesAKey() {
        val url = GeminiProtocol.endpoint("https://generativelanguage.googleapis.com/v1beta/", "gemini-2.5-pro")
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent", url)
        assertFalse(url.contains("key="))
    }

    @Test
    fun geminiRequest_hasSystemInstructionAndUserContent() {
        val body = obj(GeminiProtocol.requestBody("be brief", "list files"))
        val system = body.getValue("systemInstruction").jsonObject.getValue("parts").jsonArray[0].jsonObject
        assertEquals("be brief", system.getValue("text").jsonPrimitive.content)
        val content = body.getValue("contents").jsonArray[0].jsonObject
        assertEquals("user", content.getValue("role").jsonPrimitive.content)
        assertEquals("list files", content.getValue("parts").jsonArray[0].jsonObject.getValue("text").jsonPrimitive.content)
    }

    @Test
    fun geminiReply_joinsTextParts() {
        val reply = """{"candidates":[{"content":{"parts":[{"text":"CMD:"},{"text":"df -h"}]}}]}"""
        assertEquals("CMD:df -h", GeminiProtocol.extractText(reply))
    }

    @Test
    fun geminiReply_withoutTextIsNull() {
        assertNull(GeminiProtocol.extractText("""{"candidates":[]}"""))
        assertNull(GeminiProtocol.extractText("""{"candidates":[{"finishReason":"SAFETY"}]}"""))
        assertNull(GeminiProtocol.extractText("""{"candidates":[{"content":{"parts":[]}}]}"""))
        assertNull(GeminiProtocol.extractText("nope"))
    }

    @Test
    fun geminiErrorDetail_readsTheErrorMessage() {
        assertEquals("API key not valid", GeminiProtocol.errorDetail("""{"error":{"code":400,"message":"API key not valid"}}"""))
    }

    @Test
    fun parsedReplyFlowsIntoTheSameAiReplyShape() {
        val reply = AiClient.parse(OpenAiChatProtocol.extractText("""{"choices":[{"message":{"content":"CMD:ls"}}]}""").orEmpty())
        assertEquals("ls", reply.command)
        assertTrue(reply.parts.isEmpty())
    }
}
