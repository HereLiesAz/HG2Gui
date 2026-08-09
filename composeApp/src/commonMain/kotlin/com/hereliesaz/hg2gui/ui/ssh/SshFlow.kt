package com.hereliesaz.hg2gui.ui.ssh

import com.hereliesaz.hg2gui.ui.SessionUiState

data class SshPreset(
    val name: String,
    val user: String,
    val host: String,
    val port: Int = 22,
    val keyPath: String? = null
)

/**
 * Drives the ssh "new…" pill: a short sequence of prompts reusing SessionUiState's existing
 * awaitPromptAnswer/answerPrompt machinery (the same CompletableDeferred bridge ShellSession's
 * own interactive stdin prompts use) as pure UI-state orchestration - no shell process involved
 * until the assembled command is actually run.
 */
object SshFlow {
    fun argsFor(user: String, host: String, port: Int, keyPath: String?): String {
        val target = if (user.isNotBlank()) "$user@$host" else host
        val parts = mutableListOf(target)
        if (port != 22) parts += listOf("-p", port.toString())
        if (keyPath != null) parts += listOf("-i", keyPath)
        return parts.joinToString(" ")
    }

    fun buildCommand(user: String, host: String, port: Int, keyPath: String?): String =
        "ssh " + argsFor(user, host, port, keyPath)

    /**
     * Collects host/user/port/key through sequential prompts, optionally saves the result as a
     * preset, then writes the assembled command into the session's input field for the user to
     * review and run themselves - there's no single well-defined "connection succeeded" moment
     * to hook a save-preset step to after the fact, given how ShellSession's prompt detection
     * works, so presets are offered up front instead.
     */
    suspend fun runNewConnectionWizard(
        session: SessionUiState,
        savePreset: suspend (SshPreset) -> Unit
    ) {
        val hostInput = session.awaitPromptAnswer("ssh — user@host (or just host)").trim()
        if (hostInput.isEmpty()) return

        val user: String
        val host: String
        if ("@" in hostInput) {
            val (u, h) = hostInput.split("@", limit = 2)
            user = u
            host = h
        } else {
            host = hostInput
            user = session.awaitPromptAnswer("ssh — username (blank = don't specify)").trim()
        }

        val port = session.awaitPromptAnswer("ssh — port (blank = 22)").trim().toIntOrNull() ?: 22
        val keyPath = session.awaitPromptAnswer("ssh — private key path (blank = password auth)")
            .trim().ifBlank { null }

        val saveName = session.awaitPromptAnswer("Save as preset? (name, blank to skip)").trim()
        if (saveName.isNotEmpty()) {
            savePreset(SshPreset(saveName, user, host, port, keyPath))
        }

        session.tokens = emptyList()
        session.inputText = buildCommand(user, host, port, keyPath)
    }
}
