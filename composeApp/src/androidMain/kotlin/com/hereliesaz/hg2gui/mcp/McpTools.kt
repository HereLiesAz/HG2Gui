package com.hereliesaz.hg2gui.mcp

import android.content.Context
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private fun JsonObject.stringArg(key: String): String? = (this[key] as? JsonPrimitive)?.content

private fun textContent(text: String): JsonArray = buildJsonArray {
    addJsonObject { put("type", "text"); put("text", text) }
}

/**
 * The MCP tool registry: `vfs.*` wraps VfsManager 1:1 (sandbox-escape-proof for free, via
 * VfsManager.resolve's own containment check), `shell.*` wraps a Service-owned TerminalEngine
 * kept separate from the user's visible terminal tabs. The shell-exec gate is enforced here, in
 * callTool - the one place every "tools/call" request for a shell.* name actually passes through
 * - not just by what tools/list chooses to advertise (it always advertises both groups, for
 * honest discoverability).
 */
class McpTools(
    private val context: Context,
    private val shellExecEnabled: StateFlow<Boolean>,
    private val shellEngine: TerminalEngine
) : McpToolProvider {

    private data class ToolSpec(
        val name: String,
        val description: String,
        val inputSchema: JsonObject,
        val handler: suspend (JsonObject?) -> ToolCallResult
    )

    private fun schema(properties: JsonObject, required: List<String>): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", properties)
        putJsonArray("required") { required.forEach { add(it) } }
    }

    private fun stringProp(description: String): JsonObject = buildJsonObject {
        put("type", "string")
        put("description", description)
    }

    private fun props(vararg pairs: Pair<String, JsonObject>): JsonObject = buildJsonObject {
        pairs.forEach { (k, v) -> put(k, v) }
    }

    private val vfsTools: List<ToolSpec> = listOf(
        ToolSpec(
            "vfs.list",
            "List entries (name, type) in a directory of HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("Directory path, e.g. \"/\" or \"/Downloads\"")), listOf("path"))
        ) { args ->
            val path = args?.stringArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            val dir = VfsManager.resolve(context, path)
            if (dir == null || !dir.isDirectory) {
                ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Not a directory: $path")
            } else {
                val listing = (dir.listFiles() ?: emptyArray())
                    .sortedBy { it.name.lowercase() }
                    .joinToString("\n") { "${if (it.isDirectory) "d" else "f"} ${it.name}" }
                ToolCallResult.Success(textContent(listing.ifEmpty { "(empty)" }))
            }
        },
        ToolSpec(
            "vfs.read",
            "Read a text file's contents from HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("File path, e.g. \"/notes.txt\"")), listOf("path"))
        ) { args ->
            val path = args?.stringArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            val text = VfsManager.readText(context, path)
            if (text == null) ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Not a file: $path")
            else ToolCallResult.Success(textContent(text))
        },
        ToolSpec(
            "vfs.write",
            "Write (overwrite or create) a text file in HG2Gui's app-private sandboxed filesystem.",
            schema(
                props(
                    "path" to stringProp("File path, e.g. \"/notes.txt\""),
                    "content" to stringProp("Text to write")
                ),
                listOf("path", "content")
            )
        ) { args ->
            val path = args?.stringArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            val content = args.stringArg("content")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"content\"")
            if (VfsManager.writeText(context, path, content)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to write: $path")
        },
        ToolSpec(
            "vfs.mkdir",
            "Create a directory (and parents) in HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("Directory path to create")), listOf("path"))
        ) { args ->
            val path = args?.stringArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            if (VfsManager.mkdir(context, path)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to create: $path")
        },
        ToolSpec(
            "vfs.delete",
            "Delete a file or directory (recursively) in HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("Path to delete")), listOf("path"))
        ) { args ->
            val path = args?.stringArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            if (VfsManager.delete(context, path)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to delete: $path")
        },
        ToolSpec(
            "vfs.move",
            "Move/rename a file or directory within HG2Gui's app-private sandboxed filesystem.",
            schema(
                props("from" to stringProp("Source path"), "to" to stringProp("Destination path")),
                listOf("from", "to")
            )
        ) { args ->
            val from = args?.stringArg("from")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"from\"")
            val to = args.stringArg("to")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"to\"")
            if (VfsManager.move(context, from, to)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to move $from -> $to")
        },
        ToolSpec(
            "vfs.copy",
            "Copy a file or directory within HG2Gui's app-private sandboxed filesystem.",
            schema(
                props("from" to stringProp("Source path"), "to" to stringProp("Destination path")),
                listOf("from", "to")
            )
        ) { args ->
            val from = args?.stringArg("from")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"from\"")
            val to = args.stringArg("to")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"to\"")
            if (VfsManager.copy(context, from, to)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to copy $from -> $to")
        }
    )

    private val shellTools: List<ToolSpec> = listOf(
        ToolSpec(
            "shell.exec",
            "Run a shell command in HG2Gui's real Termux environment, on a session separate from " +
                "the user's own visible terminal tabs. Disabled unless the user has explicitly " +
                "enabled shell execution for the MCP server (a biometric-gated toggle in-app).",
            schema(props("command" to stringProp("The shell command line to run")), listOf("command"))
        ) { args ->
            val command = args?.stringArg("command")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"command\"")
            // No human is necessarily present to answer an interactive prompt an agent-issued
            // command triggers (ssh/sudo/etc.). TerminalEngine.run's onNeedInput must return a
            // String, not the null ShellSession.exec() uses inline to mean "leave it stalled" -
            // an empty answer is the closest equivalent this API can express; most confirmation
            // prompts will reject it, and the idle-gap timeout reclaims control either way rather
            // than hanging forever.
            val output = shellEngine.run(command) { "" }.toList().lastOrNull().orEmpty()
            ToolCallResult.Success(textContent(output))
        }
    )

    private val allTools: List<ToolSpec> = vfsTools + shellTools

    override fun listTools(): JsonArray = buildJsonArray {
        allTools.forEach { tool ->
            addJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("inputSchema", tool.inputSchema)
            }
        }
    }

    override suspend fun callTool(name: String, arguments: JsonObject?): ToolCallResult {
        val tool = allTools.firstOrNull { it.name == name }
            ?: return ToolCallResult.Failure(McpJsonRpc.METHOD_NOT_FOUND, "Unknown tool: $name")
        if (name.startsWith("shell.") && !shellExecEnabled.value) {
            return ToolCallResult.Failure(McpJsonRpc.SHELL_EXEC_DISABLED, "Shell execution is disabled")
        }
        return tool.handler(arguments)
    }
}
