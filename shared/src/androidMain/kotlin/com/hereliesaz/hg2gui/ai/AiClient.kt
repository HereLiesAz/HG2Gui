package com.hereliesaz.hg2gui.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** [parts] is a flag-by-flag "what each part does" breakdown, present only for command replies. */
data class AiReply(
    val text: String,
    val command: String?,
    val parts: List<Pair<String, String>> = emptyList(),
    val intent: AiIntent? = command?.let(AiIntentAnalyzer::analyze)
)

private const val SYSTEM_PROMPT = """You are a command-line assistant inside HG2Gui, a Termux-based Android terminal app. The user's current working directory is {cwd}.

If the user's request can be satisfied by a single shell command, reply on the first line with ONLY that command, prefixed with "CMD:" - no explanation, no markdown fences on that line. If the command has more than one or two meaningful tokens/flags, follow it with a line reading exactly "PARTS:", then one line per meaningful token formatted as "<token>|<what it does, one short sentence>" - skip trivial or obvious tokens. Otherwise reply with a short plain-text answer, prefixed with "TEXT:", and no PARTS section.

Do not claim that a command has more authority than its syntax requests. HG2Gui independently classifies command intent and enforces package, isolation, ADB, and root policy after your reply."""

/**
 * The model every request names. `claude-opus-5-5` is a current, valid model id (Claude Opus 5.5,
 * listed in the Anthropic model table); it is deliberately not changed here. If the API ever stops
 * accepting it (retired, or an account without access), [AiClient.ask] raises
 * [AiModelRejectedException] with a message the UI can show as-is, rather than a raw SDK error.
 */
internal const val AI_MODEL_ID = "claude-opus-5-5"

/** The API refused [AI_MODEL_ID] (HTTP 404, or a 400 that blames the model). [message] is
 *  user-presentable; [statusCode] is the HTTP status the API answered with. */
class AiModelRejectedException(message: String, val statusCode: Int, cause: Throwable? = null) :
    Exception(message, cause)

/** A non-Anthropic provider answered with an error (or an unreadable reply); [message] is user-presentable. */
class AiHttpException(val statusCode: Int, val provider: AiProvider, detail: String?) :
    Exception("${provider.label} answered HTTP $statusCode" + (detail?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""))

/**
 * True when an API failure with HTTP [statusCode] and error text [detail] means "this model id is
 * not usable" rather than a problem with the request or the key: a 404 (the API's answer for an
 * unknown or unavailable model), or a 400 whose text names the model. Pure so it is unit-testable
 * without the SDK; see AiClientTest.
 */
internal fun isModelRejection(statusCode: Int, detail: String?): Boolean = when (statusCode) {
    404 -> true
    400 -> detail?.contains("model", ignoreCase = true) == true
    else -> false
}

/** The sentence shown when [model] is rejected; keeps the API's own [detail] for diagnosis. */
internal fun modelRejectionMessage(model: String, detail: String?, provider: AiProvider = AiProvider.ANTHROPIC): String =
    "The AI model \"$model\" was rejected by the ${provider.label} API (it may be retired or unavailable " +
        "to this API key). Change the model in AI settings, or check your account's model access." +
        (detail?.takeIf { it.isNotBlank() }?.let { " API said: $it" } ?: "")

/**
 * Single-turn natural-language -> shell-command suggestion (or plain-text reply), via the
 * official Anthropic Java SDK. Never executes anything itself: the suggested command is converted
 * to a typed local intention and handed back for user review.
 */
object AiClient {
    // One SDK client (and so one OkHttp connection pool / dispatcher) reused across requests,
    // rebuilt only when the API key changes; the replaced client is closed so its threads and
    // pooled connections don't leak.
    private var cachedKey: String? = null
    private var cachedClient: AnthropicClient? = null

    @Synchronized
    private fun clientFor(apiKey: String): AnthropicClient {
        cachedClient?.takeIf { cachedKey == apiKey }?.let { return it }
        cachedClient?.let { old -> runCatching { old.close() } }
        return AnthropicOkHttpClient.builder().apiKey(apiKey).build().also {
            cachedClient = it
            cachedKey = apiKey
        }
    }

    suspend fun ask(
        apiKey: String,
        cwd: String,
        question: String,
        skills: List<Pair<String, String>> = emptyList(),
        config: AiConfig = AiConfig(),
    ): AiReply = withContext(Dispatchers.IO) {
        config.problem(apiKey)?.let { throw IllegalStateException(it) }
        var system = SYSTEM_PROMPT.replace("{cwd}", cwd)
        if (skills.isNotEmpty()) {
            system += "\n\nThe user has installed the following skills from the azphalt store. " +
                "Use them as reference when relevant:\n" +
                skills.joinToString("\n\n") { (id, text) -> "## Skill: $id\n$text" }
        }
        if (config.provider != AiProvider.ANTHROPIC) {
            return@withContext parse(askOverHttp(config, apiKey, system, question))
        }
        val model = config.effectiveModel
        val client = clientFor(apiKey)
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(2048L)
            .system(system)
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
            .addUserMessage(question)
            .build()

        val raw = try {
            client.messages().create(params).content()
                .joinToString("") { block -> block.text().map { it.text() }.orElse("") }
        } catch (e: AnthropicServiceException) {
            // A rejected model id is the one failure the user can act on and the raw SDK message
            // explains badly; every other API failure (401, 429, 5xx, ...) is rethrown unchanged.
            val status = e.statusCode()
            if (isModelRejection(status, e.message)) {
                throw AiModelRejectedException(modelRejectionMessage(model, e.message), status, e)
            }
            throw e
        }

        parse(raw)
    }

    // OpenAI, Gemini, Ollama and custom endpoints share one OkHttp client (and so one connection pool).
    // Redirects are never followed: an API endpoint has no reason to redirect, and following one
    // (above all an https -> http downgrade) would resend the key header to wherever it points.
    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .callTimeout(90, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private val JSON_MEDIA = "application/json".toMediaType()

    /** One request to a non-Anthropic provider; returns the model's raw text. [config] is already validated. */
    private fun askOverHttp(config: AiConfig, apiKey: String, system: String, question: String): String {
        val baseUrl = checkNotNull(config.effectiveBaseUrl)
        val model = config.effectiveModel
        val gemini = config.provider == AiProvider.GEMINI
        val url = if (gemini) GeminiProtocol.endpoint(baseUrl, model) else OpenAiChatProtocol.endpoint(baseUrl)
        val body = if (gemini) GeminiProtocol.requestBody(system, question) else OpenAiChatProtocol.requestBody(model, system, question)
        val request = Request.Builder().url(url).apply {
            // Keys travel in a header, never the URL, so they cannot end up in a logged request line.
            if (gemini) header("x-goog-api-key", apiKey)
            else if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
        }.post(body.toRequestBody(JSON_MEDIA)).build()

        http.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                val detail = (if (gemini) GeminiProtocol.errorDetail(text) else OpenAiChatProtocol.errorDetail(text))
                    ?: text.take(200).ifBlank { null }
                if (isModelRejection(response.code, detail)) {
                    throw AiModelRejectedException(modelRejectionMessage(model, detail, config.provider), response.code)
                }
                throw AiHttpException(response.code, config.provider, detail)
            }
            return (if (gemini) GeminiProtocol.extractText(text) else OpenAiChatProtocol.extractText(text))
                ?: throw AiHttpException(response.code, config.provider, "the reply had no text")
        }
    }

    internal fun parse(raw: String): AiReply {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("CMD:") -> {
                val lines = trimmed.lines()
                val command = lines.first().removePrefix("CMD:").trim()
                AiReply(
                    text = command,
                    command = command,
                    parts = parseParts(lines.drop(1)),
                    intent = AiIntentAnalyzer.analyze(command)
                )
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
