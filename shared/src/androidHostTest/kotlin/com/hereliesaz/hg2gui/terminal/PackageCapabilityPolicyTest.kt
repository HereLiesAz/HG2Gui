package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.terminal.PackageCapabilityPolicy.Capability
import com.hereliesaz.hg2gui.terminal.PackageCapabilityPolicy.Mode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PackageCapabilityPolicyTest {
    private class MapStore : PackageCapabilityPolicy.ModeStore {
        val map = mutableMapOf<String, String>()
        override fun get(key: String) = map[key]
        override fun put(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
    }

    private val store = MapStore()
    private val key = "pkg:git"

    @Before fun reset() = PackageCapabilityPolicy.clearOneShotsForTest()
    @After fun cleanup() = PackageCapabilityPolicy.clearOneShotsForTest()

    @Test
    fun defaultIsAskAndAskFailsClosed() {
        for (capability in Capability.entries) {
            assertEquals(Mode.ASK, PackageCapabilityPolicy.mode(store, key, capability))
            val d = PackageCapabilityPolicy.decide(store, key, capability)
            assertFalse(d.allowed)
            assertEquals(Mode.ASK, d.mode)
        }
    }

    @Test
    fun denyIsPersistentAndNeverAllows() {
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.DENY)
        repeat(3) { assertFalse(PackageCapabilityPolicy.decide(store, key, Capability.NETWORK).allowed) }
        assertEquals(Mode.DENY, PackageCapabilityPolicy.mode(store, key, Capability.NETWORK))
    }

    @Test
    fun allowOnce_isConsumedByExactlyOneDecision_andNeverPersisted() {
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.ALLOW_ONCE)
        assertTrue(store.map.isEmpty())
        val first = PackageCapabilityPolicy.decide(store, key, Capability.NETWORK)
        assertTrue(first.allowed)
        assertTrue(first.consumedOneShot)
        assertFalse(PackageCapabilityPolicy.decide(store, key, Capability.NETWORK).allowed)
        assertEquals(Mode.ASK, PackageCapabilityPolicy.mode(store, key, Capability.NETWORK))
    }

    @Test
    fun peek_reportsTheGrantWithoutSpendingIt() {
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.ALLOW_ONCE)
        repeat(3) { assertTrue(PackageCapabilityPolicy.peek(store, key, Capability.NETWORK).allowed) }
        // ...so the launch that really consumes it still finds it (m-1: check before the seed copy).
        assertTrue(PackageCapabilityPolicy.decide(store, key, Capability.NETWORK).allowed)
        assertFalse(PackageCapabilityPolicy.peek(store, key, Capability.NETWORK).allowed)
    }

    @Test
    fun peek_respectsDenyAndAsk() {
        assertFalse(PackageCapabilityPolicy.peek(store, key, Capability.NETWORK).allowed)
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.DENY)
        val d = PackageCapabilityPolicy.peek(store, key, Capability.NETWORK)
        assertFalse(d.allowed)
        assertEquals(Mode.DENY, d.mode)
    }

    @Test
    fun settingAskOrDenyDiscardsAPendingOneShot() {
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.ALLOW_ONCE)
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.DENY)
        assertFalse(PackageCapabilityPolicy.decide(store, key, Capability.NETWORK).allowed)
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.ALLOW_ONCE)
        PackageCapabilityPolicy.clear(store, key, Capability.NETWORK)
        assertFalse(PackageCapabilityPolicy.decide(store, key, Capability.NETWORK).allowed)
        assertEquals(Mode.ASK, PackageCapabilityPolicy.mode(store, key, Capability.NETWORK))
    }

    @Test
    fun capabilitiesAndPackagesAreIndependent() {
        PackageCapabilityPolicy.set(store, key, Capability.NETWORK, Mode.ALLOW_ONCE)
        assertFalse(PackageCapabilityPolicy.decide(store, key, Capability.ROOT).allowed)
        assertFalse(PackageCapabilityPolicy.decide(store, "pkg:curl", Capability.NETWORK).allowed)
        assertTrue(PackageCapabilityPolicy.decide(store, key, Capability.NETWORK).allowed)
    }

    @Test
    fun corruptStoredModeFallsBackToAsk() {
        store.put(PackageCapabilityPolicy.key(key, Capability.ADB), "BOGUS")
        assertEquals(Mode.ASK, PackageCapabilityPolicy.mode(store, key, Capability.ADB))
    }

    @Test
    fun onlyNetworkIsEnforceable() {
        // m-1: nothing can withhold adb/root/host access from a package process, so those are never "granted".
        assertEquals(listOf(Capability.NETWORK), Capability.entries.filter { PackageCapabilityPolicy.isEnforceable(it) })
        for (capability in Capability.entries.filter { !PackageCapabilityPolicy.isEnforceable(it) }) {
            val message = PackageCapabilityPolicy.unenforceableMessage("git", capability)
            assertTrue(message.contains("denied"))
            assertTrue(message.contains(capability.wireName))
        }
    }

    @Test
    fun networkRefusalMessageNamesThePackageAndTheRemedy() {
        val m = PackageCapabilityPolicy.networkRefusal("git", "human approval required")
        assertTrue(m.contains("git"))
        assertTrue(m.contains("human approval required"))
        assertTrue(m.contains("Allow once"))
    }
}
