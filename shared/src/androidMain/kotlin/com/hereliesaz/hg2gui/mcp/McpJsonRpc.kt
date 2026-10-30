package com.hereliesaz.hg2gui.mcp

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/*
 * Newline-delimited JSON-RPC 2.0 - the same per-line framing MCP's own stdio transport uses, so
 * a thin adb-forward + stdio<->TCP relay could speak to a stock stdio MCP client later. Kept
 * fully protocol-only and testable on its own: nothing here knows about VfsManager, ShellSession
 * or the pairing token - McpServerService owns the socket/auth, McpTools owns what the tools
 * actually do, this just owns the envelope and the method switch.
 */

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val method: String = "",
    val params: JsonElement? = null
)

@Serializable
data class JsonRpcError(val code: Int, val message: String)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: JsonElement? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

/**
 * What a tool call resolved to. A [Success] carries its MCP `content` ([isError] marks a tool that
 * ran but reports failure, e.g. a command that exited non-zero). A [Failure] is a tool-level
 * error: MCP wants those delivered as a normal `tools/call` result with `isError: true` so the
 * model can read the message and recover; only [protocolError] failures (an unknown tool) are
 * returned as JSON-RPC errors.
 */
sealed interface ToolCallResult {
    data class Success(val content: JsonElement, val isError: Boolean = false) : ToolCallResult
    data class Failure(val code: Int, val message: String, val protocolError: Boolean = false) : ToolCallResult
}

/** Implemented by McpTools; kept as an interface so McpJsonRpc doesn't need to know about
 *  VfsManager/ShellSession/the shell-exec gate to be unit-tested. */
interface McpToolProvider {
    fun listTools(): JsonArray
    suspend fun callTool(name: String, arguments: JsonObject?): ToolCallResult
}

object McpJsonRpc {
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603
    /** App-specific: a shell.* tool was called while the shell-exec toggle is off. Enforced here
     *  in the request-handling path (McpTools.callTool), not just by what tools/list omits. */
    const val SHELL_EXEC_DISABLED = -32001

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** One input line, classified: a request to answer, a notification to swallow, or an immediate error reply. */
    internal sealed interface ParsedLine {
        data class Call(val request: JsonRpcRequest) : ParsedLine
        /** A request with no `id` member: JSON-RPC forbids any response to it. */
        data object Notification : ParsedLine
        data class Invalid(val response: JsonRpcResponse) : ParsedLine
    }

    internal fun parseLine(line: String): ParsedLine {
        val element = try {
            json.parseToJsonElement(line)
        } catch (e: Exception) {
            return ParsedLine.Invalid(JsonRpcResponse(error = JsonRpcError(PARSE_ERROR, "Parse error: ${e.message}")))
        }
        val obj = element as? JsonObject
            ?: return ParsedLine.Invalid(JsonRpcResponse(error = JsonRpcError(INVALID_REQUEST, "Invalid Request: expected a JSON object")))
        val request = try {
            json.decodeFromJsonElement(JsonRpcRequest.serializer(), obj)
        } catch (e: Exception) {
            return ParsedLine.Invalid(JsonRpcResponse(error = JsonRpcError(INVALID_REQUEST, "Invalid Request: ${e.message}")))
        }
        // `notifications/initialized` and friends arrive with no id. Answering them (the old code sent
        // a "method not found" error) confuses strict clients; they must also never trigger work.
        return if (!obj.containsKey("id")) ParsedLine.Notification else ParsedLine.Call(request)
    }

    /** Parses one line, dispatches it, and serializes the reply - never throws (other than
     *  coroutine cancellation): a malformed line becomes a Parse error response and a tool that
     *  throws becomes an Internal error response, same as any other JSON-RPC failure mode.
     *  Returns null when the line was a notification and must get no response at all. */
    suspend fun handleLine(line: String, tools: McpToolProvider): String? {
        val response = when (val parsed = parseLine(line)) {
            is ParsedLine.Invalid -> parsed.response
            ParsedLine.Notification -> return null
            is ParsedLine.Call -> dispatch(parsed.request, tools)
        }
        return json.encodeToString(JsonRpcResponse.serializer(), response)
    }

    /** The `tools/call` result object for [result]: `content` + `isError`. */
    internal fun toolResult(content: JsonElement, isError: Boolean): JsonObject = buildJsonObject {
        put("content", content)
        put("isError", isError)
    }

    private fun errorText(message: String): JsonElement = buildJsonArray {
        addJsonObject { put("type", "text"); put("text", message) }
    }

    suspend fun dispatch(request: JsonRpcRequest, tools: McpToolProvider): JsonRpcResponse {
        val response = when (request.method) {
            "initialize" -> JsonRpcResponse(id = request.id, result = buildJsonObject {
                put("protocolVersion", "2025-06-18")
                putJsonObject("capabilities") { putJsonObject("tools") {} }
                putJsonObject("serverInfo") {
                    put("name", "hg2gui")
                    put("version", "1.0.0")
                }
            })

            "tools/list" -> JsonRpcResponse(id = request.id, result = buildJsonObject {
                put("tools", tools.listTools())
            })

            "tools/call" -> {
                val params = request.params as? JsonObject
                val name = (params?.get("name") as? JsonPrimitive)?.content
                if (name == null) {
                    JsonRpcResponse(id = request.id, error = JsonRpcError(INVALID_PARAMS, "Missing tool name"))
                } else {
                    val arguments = params["arguments"] as? JsonObject
                    val result = try {
                        tools.callTool(name, arguments)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ToolCallResult.Failure(INTERNAL_ERROR, "Internal error: ${e.message ?: e.javaClass.simpleName}")
                    }
                    when (result) {
                        is ToolCallResult.Success -> JsonRpcResponse(id = request.id, result = toolResult(result.content, result.isError))
                        is ToolCallResult.Failure ->
                            if (result.protocolError) {
                                JsonRpcResponse(id = request.id, error = JsonRpcError(result.code, result.message))
                            } else {
                                JsonRpcResponse(id = request.id, result = toolResult(errorText(result.message), isError = true))
                            }
                    }
                }
            }

            else -> JsonRpcResponse(
                id = request.id,
                error = JsonRpcError(METHOD_NOT_FOUND, "Unknown method: ${request.method}")
            )
        }
        return response
    }
}
