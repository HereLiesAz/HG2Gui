package com.hereliesaz.hg2gui.ai

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [parts] is a flag-by-flag "what each part does" breakdown, present only for command replies
 *  where the model chose to include one - see HG2Gui_Redesign.dc.html's "Phase 4" concept. Empty
 *  for plain-text replies and for commands too simple to be worth breaking down. */
data class AiReply(val text: String, val command: String?, val parts: List<Pair<String, String>> = emptyList())

private const val SYSTEM_PROMPT = """You are a command-line assistant inside HG2Gui, a Termux-based Android terminal app. The user's current working directory is {cwd}.

If the user's request can be satisfied by a single shell command, reply on the first line with ONLY that command, prefixed with "CMD:" - no explanation, no markdown fences on that line. If the command has more than one or two meaningful tokens/flags, follow it with a line reading exactly "PARTS:", then one line per meaningful token formatted as "<token>|<what it does, one short sentence>" - skip trivial or obvious tokens. Otherwise reply with a short plain-text answer, prefixed with "TEXT:", and no PARTS section."""

/**
 * Single-turn natural-language -> shell-command suggestion (or plain-text reply), via the
 * official Anthropic Java SDK (Kotlin uses the Java SDK, not a hand-rolled HTTP client). Never
 * executes anything itself - a suggested command is handed back to the caller to drop into the
 * terminal's input line, exactly like every wizard-produced command, for the user to review and
 * press Run. This is deliberately not a second way for an agent to run code unattended; that's
 * the MCP server's biometric-gated shell.exec tool, a separate and more guarded surface.
 */
object AiClient {
    suspend fun ask(
        apiKey: String,
        cwd: String,
        question: String,
        skills: List<Pair<String, String>> = emptyList(),
    ): AiReply = withContext(Dispatchers.IO) {
        val client = AnthropicOkHttpClient.builder().apiKey(apiKey).build()
        var system = SYSTEM_PROMPT.replace("{cwd}", cwd)
        if (skills.isNotEmpty()) {
            system += "\n\nThe user has installed the following skills from the azphalt store. " +
                "Use them as reference when relevant:\n" +
                skills.joinToString("\n\n") { (id, text) -> "## Skill: $id\n$text" }
        }
        val params = MessageCreateParams.builder()
            .model("claude-opus-5")
            .maxTokens(2048L)
            .system(system)
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
                val lines = trimmed.lines()
                val command = lines.first().removePrefix("CMD:").trim()
                AiReply(text = command, command = command, parts = parseParts(lines.drop(1)))
            }
            trimmed.startsWith("TEXT:") -> AiReply(text = trimmed.removePrefix("TEXT:").trim(), command = null)
            else -> AiReply(text = trimmed, command = null)
        }
    }

    private fun parseParts(rest: List<String>): List<Pair<String, String>> {
        val start = rest.indexOfFirst { it.trim() == "PARTS:" }
        if (start < 0) return emptyList()
        return rest.drop(start + 1).mapNotNull { line ->
            val i = line.indexOf('|')
            if (i < 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
        }
    }
}
