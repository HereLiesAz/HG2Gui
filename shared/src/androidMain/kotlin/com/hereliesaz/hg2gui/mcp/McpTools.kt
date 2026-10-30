package com.hereliesaz.hg2gui.mcp

import android.content.Context
import com.hereliesaz.hg2gui.managers.VfsManager
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

/**
 * Same as [stringArg], but forces the result to be root-absolute. A path with no leading "/" would
 * otherwise be ambiguous about what it is relative to - an MCP client has no way to observe or
 * control any working directory - so every vfs.* tool call resolves paths against the sandbox
 * root (VfsSandbox itself also treats every path as root-absolute).
 */
private fun JsonObject.absPathArg(key: String): String? =
    stringArg(key)?.let { if (it.startsWith("/")) it else "/$it" }

private fun textContent(text: String): JsonArray = buildJsonArray {
    addJsonObject { put("type", "text"); put("text", text) }
}

/** Longest `shell.exec` output returned to the agent; the head is kept and the result says so. */
internal const val MAX_SHELL_OUTPUT_CHARS = 256 * 1024

/**
 * `shell.exec`'s tool result: [output], then a trailing `exit code: N` line when the exit code is
 * known. A non-zero code marks the result `isError` so the agent can tell failure from success
 * (it used to see only the last chunk of text and never the code). Pure, see McpToolsTest.
 */
internal fun shellExecResult(output: String, exitCode: Int?, truncated: Boolean): ToolCallResult.Success {
    val text = buildString {
        append(output.ifEmpty { "(no output)" })
        if (truncated) append("\n[output truncated at $MAX_SHELL_OUTPUT_CHARS characters]")
        if (exitCode != null) append("\nexit code: ").append(exitCode)
    }
    return ToolCallResult.Success(textContent(text), isError = exitCode != null && exitCode != 0)
}

/**
 * The MCP tool registry: `vfs.*` goes through a [com.hereliesaz.hg2gui.managers.VfsSandbox] pinned
 * to the app-private sandbox (so containment holds by construction and the Files UI's
 * device-storage mode can never widen it), `shell.*` wraps a Service-owned TerminalEngine
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

    /**
     * Every vfs.* call resolves through this view, pinned to the app-private sandbox at construction.
     * It deliberately does NOT use VfsManager's global root: the Files UI can switch that to real
     * device storage (setDeviceStorageMode), and a paired agent must not follow it there.
     */
    private val sandbox = VfsManager.sandbox(context)

    /**
     * Non-null (a refusal) when [path] names a protected location an MCP agent may not mutate -
     * see [isMcpProtectedVfsPath]. Checked on both the normalized request path and the canonical
     * (symlink-resolved) target, so a symlink in $HOME pointing at ~/.bashrc can't launder a write.
     */
    private fun protectedPathRefusal(path: String): ToolCallResult.Failure? {
        val canonical = sandbox.resolve(path)?.let { sandbox.pathOf(it) }
        return if (isMcpProtectedVfsPath(path) || (canonical != null && isMcpProtectedVfsPath(canonical))) {
            ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Refused: $path is a protected dotfile/config location")
        } else {
            null
        }
    }

    /**
     * Same as [protectedPathRefusal] for reads and listings ([isMcpReadProtectedVfsPath]): the
     * secrets live in exactly the locations writes are barred from (`~/.ssh`, shell history, the
     * app's `.hg2gui-*` files), and reading them needs no shell. The sandbox root itself is readable.
     */
    private fun protectedReadRefusal(path: String): ToolCallResult.Failure? {
        val canonical = sandbox.resolve(path)?.let { sandbox.pathOf(it) }
        return if (isMcpReadProtectedVfsPath(path) || (canonical != null && isMcpReadProtectedVfsPath(canonical))) {
            ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Refused: $path is a protected dotfile/config location")
        } else {
            null
        }
    }

    /** Serializes shell.exec: several paired clients share ONE engine/shell session, and two commands interleaved in it corrupt each other's output. */
    private val shellMutex = Mutex()

    private val vfsTools: List<ToolSpec> = listOf(
        ToolSpec(
            "vfs.list",
            "List entries (name, type) in a directory of HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("Directory path, e.g. \"/\" or \"/Downloads\"")), listOf("path"))
        ) { args ->
            val path = args?.absPathArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            protectedReadRefusal(path)?.let { return@ToolSpec it }
            val dir = sandbox.resolve(path)
            if (dir == null || !dir.isDirectory) {
                ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Not a directory: $path")
            } else {
                // TRASH-2: raw dir.listFiles() bypassed VfsManager's own trash-directory filter,
                // the same gap vfsListDir (the GUI's own listing path) had - a paired agent could
                // vfs.list the sandbox root, see ".trash", and vfs.write a stray file into it with
                // no meta.json, which the next purgeExpiredTrash() sweep would silently delete.
                val dirPath = sandbox.pathOf(dir)
                val listing = sandbox.listChildren(dir)
                    // Hidden / .ssh entries are not listed (they could not be read anyway); checked on
                    // the child's own path and its symlink-resolved path.
                    .filterNot { child ->
                        isMcpReadProtectedVfsPath(dirPath.trimEnd('/') + "/" + child.name) ||
                            isMcpReadProtectedVfsPath(sandbox.pathOf(child))
                    }
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
            val path = args?.absPathArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            protectedReadRefusal(path)?.let { return@ToolSpec it }
            val text = sandbox.readText(path)
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
            val path = args?.absPathArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            val content = args.stringArg("content")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"content\"")
            protectedPathRefusal(path)?.let { return@ToolSpec it }
            if (sandbox.writeText(path, content)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to write: $path")
        },
        ToolSpec(
            "vfs.mkdir",
            "Create a directory (and parents) in HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("Directory path to create")), listOf("path"))
        ) { args ->
            val path = args?.absPathArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            protectedPathRefusal(path)?.let { return@ToolSpec it }
            if (sandbox.mkdir(path)) ToolCallResult.Success(textContent("ok"))
            else ToolCallResult.Failure(McpJsonRpc.INTERNAL_ERROR, "Failed to create: $path")
        },
        ToolSpec(
            "vfs.delete",
            "Delete a file or directory (recursively) in HG2Gui's app-private sandboxed filesystem.",
            schema(props("path" to stringProp("Path to delete")), listOf("path"))
        ) { args ->
            val path = args?.absPathArg("path")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"path\"")
            protectedPathRefusal(path)?.let { return@ToolSpec it }
            if (sandbox.delete(path)) ToolCallResult.Success(textContent("ok"))
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
            val from = args?.absPathArg("from")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"from\"")
            val to = args.absPathArg("to")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"to\"")
            (protectedPathRefusal(from) ?: protectedPathRefusal(to))?.let { return@ToolSpec it }
            if (sandbox.move(from, to)) ToolCallResult.Success(textContent("ok"))
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
            val from = args?.absPathArg("from")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"from\"")
            val to = args.absPathArg("to")
                ?: return@ToolSpec ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Missing \"to\"")
            (protectedPathRefusal(from) ?: protectedPathRefusal(to))?.let { return@ToolSpec it }
            if (sandbox.copy(from, to)) ToolCallResult.Success(textContent("ok"))
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
            // The engine's flow has two shapes - the whole cumulative terminal transcript per
            // emission (shell commands) or one new line per emission (pkg, download, builtins) -
            // and CommandOutputAccumulator reassembles both into the full output. The exit code is
            // returned too, so a failed command no longer looks like a success. Serialized because
            // paired clients share this one engine.
            shellMutex.withLock {
                val output = CommandOutputAccumulator(MAX_SHELL_OUTPUT_CHARS + 1)
                var exitCode: Int? = null
                shellEngine.run(
                    command,
                    onNeedInput = { "" },
                    onExit = { exitCode = it },
                    onStyledOutput = { output.markFinalSnapshot() }
                ).collect { chunk -> output.add(chunk) }
                val text = output.text()
                shellExecResult(text.take(MAX_SHELL_OUTPUT_CHARS), exitCode, truncated = text.length > MAX_SHELL_OUTPUT_CHARS)
            }
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
            ?: return ToolCallResult.Failure(McpJsonRpc.INVALID_PARAMS, "Unknown tool: $name", protocolError = true)
        if (isShellToolGated(name, shellExecEnabled.value)) {
            return ToolCallResult.Failure(McpJsonRpc.SHELL_EXEC_DISABLED, "Shell execution is disabled")
        }
        return tool.handler(arguments)
    }
}
