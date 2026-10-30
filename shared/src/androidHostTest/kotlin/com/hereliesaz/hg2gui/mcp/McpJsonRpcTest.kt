package com.hereliesaz.hg2gui.mcp

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Envelope behaviour of [McpJsonRpc]: notifications are never answered or executed, and tool
 * failures reach the agent as `isError: true` results rather than JSON-RPC errors.
 * The handler is suspend but never actually suspends here, so it is driven with plain stdlib
 * coroutine primitives (no kotlinx-coroutines test dependency).
 */
class McpJsonRpcTest {

    private class FakeTools(var behaviour: () -> ToolCallResult) : McpToolProvider {
        var calls = 0
        override fun listTools(): JsonArray = buildJsonArray { }
        override suspend fun callTool(name: String, arguments: JsonObject?): ToolCallResult {
            calls++
            return behaviour()
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { outcome = it })
        return outcome!!.getOrThrow()
    }

    private fun handle(line: String, tools: McpToolProvider): JsonObject? =
        runSuspend { McpJsonRpc.handleLine(line, tools) }?.let { McpJsonRpc.json.parseToJsonElement(it).jsonObject }

    private fun call(name: String = "any.tool") =
        """{"jsonrpc":"2.0","id":7,"method":"tools/call","params":{"name":"$name","arguments":{}}}"""

    private fun JsonObject.errorCode(): Int? = this["error"]?.takeIf { it !is JsonNull }?.jsonObject?.get("code")?.jsonPrimitive?.int

    private fun JsonObject.result(): JsonObject = this["result"]!!.jsonObject

    private fun JsonObject.firstText(): String = result()["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content

    // --- notifications (m-7) -----------------------------------------------------------------

    @Test
    fun notification_getsNoResponse() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        assertNull(handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}""", tools))
        assertNull(handle("""{"jsonrpc":"2.0","method":"some/unknown/notification","params":{}}""", tools))
    }

    @Test
    fun notification_neverRunsATool() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        assertNull(handle("""{"jsonrpc":"2.0","method":"tools/call","params":{"name":"shell.exec","arguments":{}}}""", tools))
        assertEquals(0, tools.calls)
    }

    @Test
    fun requestWithAnId_isAnswered_evenWhenTheMethodIsUnknown() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        val response = handle("""{"jsonrpc":"2.0","id":3,"method":"nope"}""", tools)
        assertNotNull(response)
        assertEquals(McpJsonRpc.METHOD_NOT_FOUND, response!!.errorCode())
        assertEquals(3, response["id"]!!.jsonPrimitive.int)
    }

    @Test
    fun initialize_isAnswered() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        val response = handle("""{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}""", tools)!!
        assertEquals("hg2gui", response.result()["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    // --- malformed input ---------------------------------------------------------------------

    @Test
    fun garbage_isAParseError() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        assertEquals(McpJsonRpc.PARSE_ERROR, handle("{not json", tools)!!.errorCode())
    }

    @Test
    fun nonObject_isAnInvalidRequest() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        assertEquals(McpJsonRpc.INVALID_REQUEST, handle("[1,2,3]", tools)!!.errorCode())
    }

    // --- tools/call results (m-7) ------------------------------------------------------------

    @Test
    fun toolFailure_isAnIsErrorResult_notAJsonRpcError() {
        val tools = FakeTools { ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"") }
        val response = handle(call(), tools)!!
        assertNull(response.errorCode())
        assertTrue(response.result()["isError"]!!.jsonPrimitive.boolean)
        assertEquals("Missing \"path\"", response.firstText())
    }

    @Test
    fun shellGate_reachesTheAgentAsReadableToolError() {
        val tools = FakeTools { ToolCallResult.Failure(McpJsonRpc.SHELL_EXEC_DISABLED, "Shell execution is disabled") }
        val response = handle(call("shell.exec"), tools)!!
        assertNull(response.errorCode())
        assertTrue(response.result()["isError"]!!.jsonPrimitive.boolean)
        assertEquals("Shell execution is disabled", response.firstText())
    }

    @Test
    fun unknownTool_staysAProtocolError() {
        val tools = FakeTools { ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Unknown tool: x", protocolError = true) }
        val response = handle(call("x"), tools)!!
        assertEquals(McpJsonRpc.INVALID_PARAMS, response.errorCode())
        assertNull(response["result"]?.takeIf { it !is JsonNull })
    }

    @Test
    fun toolSuccess_carriesItsIsErrorFlag() {
        val content = buildJsonArray { }
        assertFalse(handle(call(), FakeTools { ToolCallResult.Success(content) })!!.result()["isError"]!!.jsonPrimitive.boolean)
        assertTrue(handle(call(), FakeTools { ToolCallResult.Success(content, isError = true) })!!.result()["isError"]!!.jsonPrimitive.boolean)
    }

    @Test
    fun thrownToolException_becomesAnIsErrorResult() {
        val tools = FakeTools { throw IllegalStateException("boom") }
        val response = handle(call(), tools)!!
        assertNull(response.errorCode())
        assertTrue(response.result()["isError"]!!.jsonPrimitive.boolean)
        assertTrue(response.firstText().contains("boom"))
    }

    @Test
    fun missingToolName_isInvalidParams() {
        val tools = FakeTools { ToolCallResult.Success(buildJsonArray { }) }
        val response = handle("""{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{}}""", tools)!!
        assertEquals(McpJsonRpc.INVALID_PARAMS, response.errorCode())
        assertEquals(0, tools.calls)
    }
}
