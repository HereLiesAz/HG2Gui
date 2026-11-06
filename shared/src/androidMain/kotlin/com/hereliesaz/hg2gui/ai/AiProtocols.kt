package com.hereliesaz.hg2gui.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Pure request building and response reading for the non-Anthropic providers, so it is unit
 * tested without a network. No max-token field is sent: the system prompt already asks for a
 * one-line answer, and the OpenAI-compatible servers disagree on its name (`max_tokens` vs
 * `max_completion_tokens`), which would turn a working request into a 400 on one of them.
 */
internal object OpenAiChatProtocol {
    fun endpoint(baseUrl: String): String = baseUrl.trimEnd('/') + "/chat/completions"

    fun requestBody(model: String, system: String, user: String): String = buildJsonObject {
        put("model", model)
        putJsonArray("messages") {
            add(buildJsonObject { put("role", "system"); put("content", system) })
            add(buildJsonObject { put("role", "user"); put("content", user) })
        }
    }.toString()

    /** The assistant text of the first choice, or null when [body] is not a chat-completions reply. */
    fun extractText(body: String): String? {
        val root = parseObject(body) ?: return null
        val message = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
        val content = (message?.get("message") as? JsonObject)?.get("content")
        return when (content) {
            is JsonPrimitive -> content.contentOrNull
            is JsonArray -> content.mapNotNull { ((it as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull }
                .joinToString("")
            else -> null
        }
    }

    /** The server's own explanation from an error body (`{"error":{"message":..}}` or `{"error":".."}`). */
    fun errorDetail(body: String): String? = jsonErrorDetail(body)
}

internal object GeminiProtocol {
    fun endpoint(baseUrl: String, model: String): String = baseUrl.trimEnd('/') + "/models/$model:generateContent"

    fun requestBody(system: String, user: String): String = buildJsonObject {
        putJsonObject("systemInstruction") {
            putJsonArray("parts") { add(buildJsonObject { put("text", system) }) }
        }
        putJsonArray("contents") {
            add(buildJsonObject {
                put("role", "user")
                putJsonArray("parts") { add(buildJsonObject { put("text", user) }) }
            })
        }
    }.toString()

    /** All text parts of the first candidate, joined, or null when [body] holds none. */
    fun extractText(body: String): String? {
        val root = parseObject(body) ?: return null
        val candidate = (root["candidates"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        val parts = ((candidate["content"] as? JsonObject)?.get("parts") as? JsonArray) ?: return null
        val text = parts.mapNotNull { ((it as? JsonObject)?.get("text") as? JsonPrimitive)?.contentOrNull }.joinToString("")
        return text.ifEmpty { null }
    }

    fun errorDetail(body: String): String? = jsonErrorDetail(body)
}

private val lenientJson = Json { ignoreUnknownKeys = true }

private fun parseObject(body: String): JsonObject? =
    runCatching { lenientJson.parseToJsonElement(body) }.getOrNull() as? JsonObject

private fun jsonErrorDetail(body: String): String? {
    val error: JsonElement = parseObject(body)?.get("error") ?: return null
    return when (error) {
        is JsonObject -> (error["message"] as? JsonPrimitive)?.contentOrNull
        is JsonPrimitive -> error.contentOrNull
        else -> null
    }?.takeIf { it.isNotBlank() }
}
