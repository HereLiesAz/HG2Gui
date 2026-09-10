package com.hereliesaz.hg2gui.terminal

import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

/**
 * In-process cooperative metadata published through HG2Gui's signature-protected Android API.
 * Entries are intentionally short-lived: terminal state stays authoritative when a producer stops
 * refreshing its description.
 */
object CooperativeMetadata {
    private const val MAX_CHANNEL_LENGTH = 96
    private const val MAX_PAYLOAD_BYTES = 128 * 1024
    private const val MAX_ENTRIES = 64
    private const val DEFAULT_TTL_MILLIS = 30_000L
    private val channelPattern = Regex("[A-Za-z0-9._:/-]{1,$MAX_CHANNEL_LENGTH}")

    data class Entry(
        val channel: String,
        val payload: String,
        val updatedAtMillis: Long,
        val expiresAtMillis: Long
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun publish(channel: String, payload: String, ttlMillis: Long = DEFAULT_TTL_MILLIS): Entry {
        require(channelPattern.matches(channel)) { "invalid metadata channel" }
        require(payload.toByteArray(Charsets.UTF_8).size <= MAX_PAYLOAD_BYTES) { "metadata payload exceeds 128 KiB" }
        require(ttlMillis in 1_000L..300_000L) { "metadata ttl must be between 1s and 5m" }
        prune()
        if (!entries.containsKey(channel) && entries.size >= MAX_ENTRIES) {
            val oldest = entries.values.minByOrNull { it.updatedAtMillis }
            oldest?.let { entries.remove(it.channel, it) }
        }
        val now = System.currentTimeMillis()
        return Entry(channel, payload, now, now + ttlMillis).also { entries[channel] = it }
    }

    fun read(channel: String): Entry? {
        val entry = entries[channel] ?: return null
        if (entry.expiresAtMillis <= System.currentTimeMillis()) {
            entries.remove(channel, entry)
            return null
        }
        return entry
    }

    fun clear(channel: String): Boolean = entries.remove(channel) != null

    fun list(prefix: String? = null): List<Entry> {
        prune()
        return entries.values
            .filter { prefix.isNullOrEmpty() || it.channel.startsWith(prefix) }
            .sortedBy { it.channel }
    }

    fun asJson(entry: Entry): String = JSONObject()
        .put("channel", entry.channel)
        .put("payload", entry.payload)
        .put("updatedAtMillis", entry.updatedAtMillis)
        .put("expiresAtMillis", entry.expiresAtMillis)
        .toString()

    fun listJson(prefix: String? = null): String = JSONArray().apply {
        list(prefix).forEach { entry ->
            put(
                JSONObject()
                    .put("channel", entry.channel)
                    .put("updatedAtMillis", entry.updatedAtMillis)
                    .put("expiresAtMillis", entry.expiresAtMillis)
            )
        }
    }.toString()

    private fun prune() {
        val now = System.currentTimeMillis()
        entries.forEach { (channel, entry) ->
            if (entry.expiresAtMillis <= now) entries.remove(channel, entry)
        }
    }
}

/** Parser for a cooperative `tui/<command>` snapshot payload. */
object CooperativeTuiSnapshot {
    fun forCommand(commandLine: String): TuiSnapshot? {
        val command = commandLine.trim().substringBefore(' ').substringAfterLast('/').takeIf { it.isNotBlank() } ?: return null
        val entry = CooperativeMetadata.read("tui/$command") ?: return null
        return runCatching { parse(JSONObject(entry.payload)) }.getOrNull()
    }

    private fun parse(root: JSONObject): TuiSnapshot {
        val layers = root.optJSONArray("layers").toObjects().mapIndexed { depth, layer ->
            val items = layer.optJSONArray("items").toObjects().mapIndexed { index, item ->
                TuiMenuItem(
                    row = item.optInt("row", index),
                    label = item.getString("label"),
                    selected = item.optBoolean("selected", false),
                    enabled = item.optBoolean("enabled", true),
                    control = enumValue<TuiControlKind>(item.optString("control"), TuiControlKind.ACTION),
                    checked = if (item.has("checked") && !item.isNull("checked")) item.getBoolean("checked") else null
                )
            }
            TuiLayer(
                heading = layer.optNullableString("heading"),
                items = items,
                activeIndex = layer.optIntOrNull("activeIndex"),
                depth = layer.optInt("depth", depth),
                modal = layer.optBoolean("modal", false),
                scrollable = layer.optBoolean("scrollable", false)
            )
        }
        val prompt = root.optJSONObject("prompt")?.let { prompt ->
            TuiPrompt(
                row = prompt.optInt("row", 0),
                label = prompt.optString("label"),
                kind = enumValue<TuiPromptKind>(prompt.optString("kind"), TuiPromptKind.UNKNOWN)
            )
        }
        val tabs = root.optJSONArray("tabs").toObjects().map { tab ->
            TuiTab(tab.getString("label"), tab.optBoolean("active", false), tab.optInt("row", 0))
        }
        val regions = root.optJSONArray("regions").toObjects().map { region ->
            TuiRegion(
                kind = enumValue<TuiRegionKind>(region.optString("kind"), TuiRegionKind.STATUS),
                startRow = region.optInt("startRow", 0),
                endRow = region.optInt("endRow", region.optInt("startRow", 0)),
                lines = region.optJSONArray("lines")?.let { array ->
                    List(array.length()) { index -> array.optString(index) }
                }.orEmpty(),
                progress = if (region.has("progress") && !region.isNull("progress")) region.optDouble("progress").toFloat().coerceIn(0f, 1f) else null
            )
        }
        return TuiSnapshot(
            title = root.optNullableString("title"),
            layers = layers,
            prompt = prompt,
            regions = regions,
            tabs = tabs,
            alternateScreen = root.optBoolean("alternateScreen", true),
            mouseAware = root.optBoolean("mouseAware", false),
            profile = enumValue(root.optString("profile"), TuiAdapterProfile.COOPERATIVE)
        )
    }

    private fun JSONArray?.toObjects(): List<JSONObject> = if (this == null) emptyList() else buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(::add)
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (!has(name) || isNull(name)) null else optInt(name)

    private inline fun <reified T : Enum<T>> enumValue(raw: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback
}
