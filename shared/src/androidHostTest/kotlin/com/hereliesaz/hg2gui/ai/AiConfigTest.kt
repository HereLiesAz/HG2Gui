package com.hereliesaz.hg2gui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConfigTest {
    @Test
    fun defaultConfig_isTheOriginalAnthropicBackend() {
        val config = AiConfig()
        assertEquals(AiProvider.ANTHROPIC, config.provider)
        assertEquals(AI_MODEL_ID, config.effectiveModel)
        assertNull(config.problem("sk-ant-x"))
    }

    @Test
    fun unknownOrMissingProviderIdFallsBackToAnthropic() {
        assertEquals(AiProvider.ANTHROPIC, AiProvider.fromId(null))
        assertEquals(AiProvider.ANTHROPIC, AiProvider.fromId("nonsense"))
        for (provider in AiProvider.entries) assertEquals(provider, AiProvider.fromId(provider.id))
    }

    @Test
    fun providerIds_areUnique() {
        assertEquals(AiProvider.entries.size, AiProvider.entries.map { it.id }.toSet().size)
    }

    @Test
    fun blankModelAndUrl_meanTheProviderDefaults() {
        val config = AiConfig(AiProvider.OPENAI, model = "  ", baseUrl = " ")
        assertEquals("gpt-5", config.effectiveModel)
        assertEquals("https://api.openai.com/v1", config.effectiveBaseUrl)
        val custom = AiConfig(AiProvider.OPENAI, model = " my-model ", baseUrl = " https://x.test/v1 ")
        assertEquals("my-model", custom.effectiveModel)
        assertEquals("https://x.test/v1", custom.effectiveBaseUrl)
    }

    @Test
    fun aKeyedProviderWithoutAKeyIsNotReady() {
        for (provider in AiProvider.entries.filter { it.needsKey }) {
            val config = AiConfig(provider, model = "m", baseUrl = "https://x.test/v1")
            assertNotNull(provider.id, config.problem(null))
            assertNotNull(provider.id, config.problem("   "))
            assertTrue(provider.id, config.problem(null)!!.contains(provider.label))
        }
    }

    @Test
    fun ollama_needsNoKey_andDefaultsToThisDevice() {
        val config = AiConfig(AiProvider.OLLAMA)
        assertFalse(AiProvider.OLLAMA.needsKey)
        assertEquals("http://127.0.0.1:11434/v1", config.effectiveBaseUrl)
        assertNull(config.problem(null))
        assertTrue(config.isReady(null))
    }

    @Test
    fun custom_needsAModelAndAnEndpoint() {
        assertNotNull(AiConfig(AiProvider.CUSTOM).problem("k"))
        assertNotNull(AiConfig(AiProvider.CUSTOM, model = "m").problem("k"))
        assertNotNull(AiConfig(AiProvider.CUSTOM, baseUrl = "https://x.test/v1").problem("k"))
        assertNull(AiConfig(AiProvider.CUSTOM, model = "m", baseUrl = "https://x.test/v1").problem("k"))
    }

    @Test
    fun aKeyIsNeverSentOverCleartextToARemoteHost() {
        val remote = AiConfig(AiProvider.CUSTOM, model = "m", baseUrl = "http://api.example.com/v1")
        val problem = remote.problem("secret")
        assertNotNull(problem)
        assertTrue(problem!!.contains("api.example.com"))
        assertFalse("the message must not echo the key", problem.contains("secret"))
    }

    @Test
    fun cleartextIsFineOnThisDeviceAndPrivateNetworks() {
        for (host in listOf("127.0.0.1", "localhost", "10.0.0.5", "192.168.1.20", "172.16.0.1", "172.31.255.1", "[::1]")) {
            val config = AiConfig(AiProvider.CUSTOM, model = "m", baseUrl = "http://$host:8080/v1")
            assertNull(host, config.problem("k"))
        }
    }

    @Test
    fun cleartextIsRefusedOutsidePrivateRanges() {
        for (host in listOf("172.15.0.1", "172.32.0.1", "11.0.0.1", "192.169.0.1", "example.com", "10.example.com")) {
            val config = AiConfig(AiProvider.CUSTOM, model = "m", baseUrl = "http://$host/v1")
            assertNotNull(host, config.problem("k"))
        }
    }

    @Test
    fun malformedOrNonHttpEndpointsAreRefused() {
        for (url in listOf("not a url", "ftp://x.test/v1", "file:///etc/passwd", "https://", "javascript:alert(1)")) {
            val config = AiConfig(AiProvider.CUSTOM, model = "m", baseUrl = url)
            assertNotNull(url, config.problem("k"))
        }
    }

    @Test
    fun httpsToARemoteHostIsAccepted() {
        assertNull(AiConfig(AiProvider.OPENAI).problem("k"))
        assertNull(AiConfig(AiProvider.GEMINI).problem("k"))
    }
}
