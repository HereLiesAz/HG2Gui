package com.hereliesaz.hg2gui.terminal

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap

/**
 * Explicit per-package capability policy. Persistent choices are ASK or DENY; ALLOW_ONCE is held
 * only in memory and consumed by a single broker decision so an app restart can never turn a
 * transient grant into durable ambient authority.
 */
object PackageCapabilityPolicy {
    enum class Capability(val wireName: String) {
        ADB("adb"),
        ROOT("root"),
        NETWORK("network"),
        HOST_READ("host-read"),
        HOST_WRITE("host-write")
    }

    enum class Mode { ASK, DENY, ALLOW_ONCE }

    data class Decision(
        val capability: Capability,
        val mode: Mode,
        val allowed: Boolean,
        val consumedOneShot: Boolean = false,
        val reason: String
    )

    private const val PREFS = "hg2gui_package_capability_policy"
    private val oneShot = ConcurrentHashMap.newKeySet<String>()

    fun mode(context: Context, packageKey: String, capability: Capability): Mode {
        val key = key(packageKey, capability)
        if (key in oneShot) return Mode.ALLOW_ONCE
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, Mode.ASK.name)
        return runCatching { Mode.valueOf(stored ?: Mode.ASK.name) }.getOrDefault(Mode.ASK)
    }

    fun set(context: Context, packageKey: String, capability: Capability, mode: Mode) {
        val key = key(packageKey, capability)
        when (mode) {
            Mode.ALLOW_ONCE -> oneShot += key
            Mode.ASK, Mode.DENY -> {
                oneShot.remove(key)
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(key, mode.name) }
            }
        }
    }

    fun clear(context: Context, packageKey: String, capability: Capability) {
        val key = key(packageKey, capability)
        oneShot.remove(key)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { remove(key) }
    }

    /**
     * Returns a broker decision without granting any authority by itself. ASK deliberately fails
     * closed until an interactive caller obtains a human decision and records ALLOW_ONCE.
     */
    fun decide(context: Context, packageKey: String, capability: Capability): Decision {
        val key = key(packageKey, capability)
        if (oneShot.remove(key)) {
            return Decision(capability, Mode.ALLOW_ONCE, allowed = true, consumedOneShot = true, reason = "one-shot human grant")
        }
        return when (mode(context, packageKey, capability)) {
            Mode.DENY -> Decision(capability, Mode.DENY, allowed = false, reason = "package policy denies capability")
            Mode.ASK -> Decision(capability, Mode.ASK, allowed = false, reason = "human approval required")
            Mode.ALLOW_ONCE -> Decision(capability, Mode.ASK, allowed = false, reason = "one-shot grant was not available")
        }
    }

    fun summary(context: Context, packageKey: String): Map<Capability, Mode> =
        Capability.entries.associateWith { mode(context, packageKey, it) }

    private fun key(packageKey: String, capability: Capability): String =
        packageKey.replace(Regex("[^A-Za-z0-9._:-]"), "_") + "." + capability.wireName
}
