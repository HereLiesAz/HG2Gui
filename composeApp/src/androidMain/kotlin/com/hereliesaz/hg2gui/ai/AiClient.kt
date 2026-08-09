package com.hereliesaz.hg2gui.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiReply(val text: String, val command: String?)

private const val SYSTEM_PROMPT = """You are a command-line assistant inside HG2Gui, a Termux-based Android terminal app. The user's current working directory is {cwd}.

If the user's request can be satisfied by a single shell command, reply with ONLY that command, prefixed with "CMD:" and nothing else - no explanation, no markdown fences. Otherwise reply with a short plain-text answer, prefixed with "TEXT:"."""

/**
 * Single-turn natural-language -> shell-command suggestion (or plain-text reply), via the
 * official Anthropic Java SDK (Kotlin uses the Java SDK, not a hand-rolled HTTP client). Never
 * executes anything itself - a suggested command is handed back to the caller to drop into the
 * terminal's input line, exactly like every wizard-produced command, for the user to review and
 * press Run. This is deliberately not a second way for an agent to run code unattended; that's
 * the MCP server's biometric-gated shell.exec tool, a separate and more guarded surface.
 */
object AiClient {
    suspend fun ask(apiKey: String, cwd: String, question: String): AiReply = withContext(Dispatchers.IO) {
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        val params = MessageCreateParams.builder()
            .model("claude-opus-5")
            .maxTokens(2048L)
            .system(SYSTEM_PROMPT.replace("{cwd}", cwd))
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            .addUserMessage(question)
            .build()

        val raw = client.messages().create(params).content()
            .joinToString("") { block -> block.text().map { it.text() }.orElse("") }

        parse(raw)
    }

    internal fun parse(raw: String): AiReply {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("CMD:") -> {
                val command = trimmed.removePrefix("CMD:").trim()
                AiReply(text = command, command = command)
            }
            trimmed.startsWith("TEXT:") -> AiReply(text = trimmed.removePrefix("TEXT:").trim(), command = null)
            else -> AiReply(text = trimmed, command = null)
        }
    }
}
