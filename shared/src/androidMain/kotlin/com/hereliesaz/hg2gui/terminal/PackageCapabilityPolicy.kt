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
    /**
     * Where persistent ASK/DENY choices live. SharedPreferences in the app; a plain map in tests
     * so the policy table (including the one-shot grant lifecycle) is unit tested without a Context.
     */
    internal interface ModeStore {
        fun get(key: String): String?
        fun put(key: String, value: String)
        fun remove(key: String)
    }

    private class PrefsStore(private val context: Context) : ModeStore {
        private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        override fun get(key: String): String? = prefs.getString(key, null)
        override fun put(key: String, value: String) = prefs.edit { putString(key, value) }
        override fun remove(key: String) = prefs.edit { remove(key) }
    }

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

    /**
     * Whether HG2Gui can actually *withhold* [capability] from a package process. Only NETWORK has
     * a gate (the isolated launch refuses to start without a grant). ADB and root are masked inside
     * the sandbox and never inherited by anything else, and host read/write have no bridge at all,
     * so a "grant" of those has nothing to hand over: requests for them are refused rather than
     * answered with a grant that changes nothing.
     */
    fun isEnforceable(capability: Capability): Boolean = capability == Capability.NETWORK

    fun mode(context: Context, packageKey: String, capability: Capability): Mode =
        mode(PrefsStore(context), packageKey, capability)

    internal fun mode(store: ModeStore, packageKey: String, capability: Capability): Mode {
        val key = key(packageKey, capability)
        if (key in oneShot) return Mode.ALLOW_ONCE
        val stored = store.get(key) ?: Mode.ASK.name
        return runCatching { Mode.valueOf(stored) }.getOrDefault(Mode.ASK)
    }

    fun set(context: Context, packageKey: String, capability: Capability, mode: Mode) =
        set(PrefsStore(context), packageKey, capability, mode)

    internal fun set(store: ModeStore, packageKey: String, capability: Capability, mode: Mode) {
        val key = key(packageKey, capability)
        when (mode) {
            Mode.ALLOW_ONCE -> oneShot += key
            Mode.ASK, Mode.DENY -> {
                oneShot.remove(key)
                store.put(key, mode.name)
            }
        }
    }

    fun clear(context: Context, packageKey: String, capability: Capability) =
        clear(PrefsStore(context), packageKey, capability)

    internal fun clear(store: ModeStore, packageKey: String, capability: Capability) {
        val key = key(packageKey, capability)
        oneShot.remove(key)
        store.remove(key)
    }

    /**
     * Returns a broker decision without granting any authority by itself. ASK deliberately fails
     * closed until an interactive caller obtains a human decision and records ALLOW_ONCE.
     * A one-shot grant is *consumed* here; use [peek] to ask without spending it.
     */
    fun decide(context: Context, packageKey: String, capability: Capability): Decision =
        decide(PrefsStore(context), packageKey, capability)

    internal fun decide(store: ModeStore, packageKey: String, capability: Capability): Decision {
        val key = key(packageKey, capability)
        if (oneShot.remove(key)) {
            return Decision(capability, Mode.ALLOW_ONCE, allowed = true, consumedOneShot = true, reason = "one-shot human grant")
        }
        return when (mode(store, packageKey, capability)) {
            Mode.DENY -> Decision(capability, Mode.DENY, allowed = false, reason = "package policy denies capability")
            Mode.ASK -> Decision(capability, Mode.ASK, allowed = false, reason = "human approval required")
            Mode.ALLOW_ONCE -> Decision(capability, Mode.ASK, allowed = false, reason = "one-shot grant was not available")
        }
    }

    /**
     * The decision [decide] would return, without consuming a one-shot grant. Lets a caller refuse
     * early (before an expensive step such as seeding an isolated runtime) and still leave the
     * grant for the launch that spends it.
     */
    fun peek(context: Context, packageKey: String, capability: Capability): Decision =
        peek(PrefsStore(context), packageKey, capability)

    internal fun peek(store: ModeStore, packageKey: String, capability: Capability): Decision {
        val key = key(packageKey, capability)
        if (key in oneShot) {
            return Decision(capability, Mode.ALLOW_ONCE, allowed = true, reason = "one-shot human grant")
        }
        return when (mode(store, packageKey, capability)) {
            Mode.DENY -> Decision(capability, Mode.DENY, allowed = false, reason = "package policy denies capability")
            else -> Decision(capability, Mode.ASK, allowed = false, reason = "human approval required")
        }
    }

    fun summary(context: Context, packageKey: String): Map<Capability, Mode> =
        Capability.entries.associateWith { mode(context, packageKey, it) }

    /** Why a request for [capability] is refused when HG2Gui has no way to enforce a grant of it. */
    fun unenforceableMessage(packageName: String, capability: Capability): String =
        "Capability denied: $packageName → ${capability.wireName}. HG2Gui cannot hand ${capability.wireName} " +
            "to a package process (it is masked inside isolation and never inherited), so a grant would change nothing. " +
            "Run a one-off elevated command yourself with hg2auth instead."

    /** The refusal for an isolated launch without a network grant; shared by the early check and the launch gate. */
    fun networkRefusal(packageName: String, reason: String): String =
        "Network policy blocked $packageName's isolated launch ($reason). " +
            "This Android/PRoot boundary cannot run a process in a truthful offline network namespace, " +
            "so HG2Gui fails closed. Arm Authority → Package policies → $packageName → network → Allow once, then run again."

    /** Test hook: forget every in-memory one-shot grant. */
    internal fun clearOneShotsForTest() = oneShot.clear()

    internal fun key(packageKey: String, capability: Capability): String =
        packageKey.replace(Regex("[^A-Za-z0-9._:-]"), "_") + "." + capability.wireName
}
