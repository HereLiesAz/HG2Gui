package com.hereliesaz.hg2gui.mcp

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The pure part of shell.exec's result: see [shellExecResult]. */
class McpToolsTest {

    private fun ToolCallResult.Success.text(): String = content.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content

    @Test
    fun zeroExit_isNotAnError_andReportsTheCode() {
        val result = shellExecResult("hello", 0, truncated = false)
        assertEquals("hello\nexit code: 0", result.text())
        assertFalse(result.isError)
    }

    @Test
    fun nonZeroExit_isAnError_withFullOutput() {
        val result = shellExecResult("line1\nline2\nline3", 2, truncated = false)
        assertEquals("line1\nline2\nline3\nexit code: 2", result.text())
        assertTrue(result.isError)
    }

    @Test
    fun unknownExit_isNotFlaggedAsAnError() {
        // Builtins report no exit code (null); that must not read as failure.
        val result = shellExecResult("ok", null, truncated = false)
        assertEquals("ok", result.text())
        assertFalse(result.isError)
    }

    @Test
    fun emptyOutput_saysSo_andTruncationIsAnnounced() {
        assertEquals("(no output)\nexit code: 0", shellExecResult("", 0, truncated = false).text())
        assertTrue(shellExecResult("x", 0, truncated = true).text().contains("[output truncated at $MAX_SHELL_OUTPUT_CHARS characters]"))
    }
}
