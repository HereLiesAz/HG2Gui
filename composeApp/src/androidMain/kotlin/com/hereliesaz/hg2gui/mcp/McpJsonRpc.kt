package com.hereliesaz.hg2gui.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

/** What a tool call resolved to - success carries its MCP `content`, failure a JSON-RPC error. */
sealed interface ToolCallResult {
    data class Success(val content: JsonElement) : ToolCallResult
    data class Failure(val code: Int, val message: String) : ToolCallResult
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

    /** Parses one line, dispatches it, and serializes the reply - never throws: a malformed
     *  line becomes a Parse error response, same as any other JSON-RPC failure mode. */
    suspend fun handleLine(line: String, tools: McpToolProvider): String {
        val request = try {
            json.decodeFromString(JsonRpcRequest.serializer(), line)
        } catch (e: Exception) {
            return json.encodeToString(
                JsonRpcResponse.serializer(),
                JsonRpcResponse(error = JsonRpcError(PARSE_ERROR, "Parse error: ${e.message}"))
            )
        }
        val response = dispatch(request, tools)
        return json.encodeToString(JsonRpcResponse.serializer(), response)
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
                    when (val result = tools.callTool(name, arguments)) {
                        is ToolCallResult.Success -> JsonRpcResponse(id = request.id, result = buildJsonObject {
                            put("content", result.content)
                            put("isError", false)
                        })
                        is ToolCallResult.Failure -> JsonRpcResponse(
                            id = request.id,
                            error = JsonRpcError(result.code, result.message)
                        )
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
