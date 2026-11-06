package com.hereliesaz.hg2gui.ai

import java.net.URI

/**
 * Who answers the AI command-search chat. [defaultModel] and [defaultBaseUrl] are only defaults:
 * the user can change both in AI settings, so a retired model id never needs an app update.
 */
enum class AiProvider(
    val id: String,
    val label: String,
    val defaultModel: String,
    val defaultBaseUrl: String?,
    /** False for a local server that takes no key (Ollama). */
    val needsKey: Boolean,
    /** True when the endpoint URL is the user's to set (a local or self-hosted server). */
    val editableEndpoint: Boolean
) {
    ANTHROPIC("anthropic", "Anthropic", AI_MODEL_ID, null, needsKey = true, editableEndpoint = false),
    OPENAI("openai", "OpenAI", "gpt-5", "https://api.openai.com/v1", needsKey = true, editableEndpoint = false),
    GEMINI(
        "gemini", "Gemini", "gemini-2.5-pro",
        "https://generativelanguage.googleapis.com/v1beta", needsKey = true, editableEndpoint = false
    ),
    OLLAMA("ollama", "Ollama (local)", "llama3.2", "http://127.0.0.1:11434/v1", needsKey = false, editableEndpoint = true),
    CUSTOM("custom", "Custom (OpenAI-compatible)", "", null, needsKey = true, editableEndpoint = true);

    /** True for the providers spoken to through the OpenAI chat-completions protocol. */
    val usesOpenAiProtocol: Boolean get() = this == OPENAI || this == OLLAMA || this == CUSTOM

    companion object {
        /** The provider stored as [id]; anything unknown (or null) is the original Anthropic backend. */
        fun fromId(id: String?): AiProvider = entries.firstOrNull { it.id == id } ?: ANTHROPIC
    }
}

/** The provider, model and endpoint a request goes to. Blank [model]/[baseUrl] mean "the provider's default". */
data class AiConfig(
    val provider: AiProvider = AiProvider.ANTHROPIC,
    val model: String = "",
    val baseUrl: String = ""
) {
    val effectiveModel: String get() = model.trim().ifEmpty { provider.defaultModel }

    val effectiveBaseUrl: String?
        get() = baseUrl.trim().ifEmpty { provider.defaultBaseUrl.orEmpty() }.ifEmpty { null }

    /**
     * Null when a request may be sent; otherwise a sentence saying what is missing or unsafe, for
     * the UI to show as it is. A key is never sent over cleartext HTTP to a host that is not on
     * this device or private network.
     */
    fun problem(apiKey: String?): String? {
        if (provider.needsKey && apiKey.isNullOrBlank()) {
            return "No ${provider.label} API key is set - add one in AI settings first."
        }
        if (effectiveModel.isEmpty()) return "No model name is set - add one in AI settings first."
        if (provider == AiProvider.ANTHROPIC) return null
        val url = effectiveBaseUrl ?: return "No endpoint URL is set - add one in AI settings first."
        val uri = runCatching { URI(url) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        val host = uri?.host
        if (host.isNullOrEmpty() || (scheme != "https" && scheme != "http")) {
            return "The endpoint URL \"$url\" is not a valid http(s) address."
        }
        if (scheme == "http" && !isLocalHost(host)) {
            return "Refusing to send an API key over plain http to $host. Use an https endpoint."
        }
        return null
    }

    fun isReady(apiKey: String?): Boolean = problem(apiKey) == null

    internal companion object {
        /** Loopback, localhost and RFC 1918 addresses: traffic that does not leave this device or LAN. */
        fun isLocalHost(host: String): Boolean {
            val h = host.lowercase().removePrefix("[").removeSuffix("]")
            if (h == "localhost" || h == "::1") return true
            // Only a literal dotted-quad counts: "10.example.com" is a public DNS name, not 10.0.0.0/8.
            val octets = ipv4Octets(h) ?: return false
            val (a, b) = octets
            return a == 127 || a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31)
        }

        /** The four octets of a literal IPv4 address, or null for anything else (names, short forms, out-of-range). */
        private fun ipv4Octets(text: String): List<Int>? {
            val parts = text.split('.')
            if (parts.size != 4) return null
            val octets = parts.map { part ->
                if (part.isEmpty() || part.length > 3 || !part.all { it in '0'..'9' }) return null
                part.toInt().takeIf { it in 0..255 } ?: return null
            }
            return octets
        }
    }
}
