package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class StaticCompletionDefinitionsTest {
    @Test
    fun parsesLiteralBashWordListsWithoutExpandingShellCode() {
        val candidates = StaticCompletionDefinitions.parseBash(
            """
            complete -W 'start stop status restart' widget
            complete -W '\$(dangerous) literal' other
            """.trimIndent(),
            prefix = "st"
        )

        assertEquals(listOf("start", "stop", "status"), candidates.map { it.value })
    }

    @Test
    fun parsesSimpleZshArgumentChoiceSets() {
        val candidates = StaticCompletionDefinitions.parseZsh(
            "_arguments '1:action:(install remove update)'",
            prefix = "u"
        )

        assertEquals(listOf("update"), candidates.map { it.value })
    }

    @Test
    fun ignoresDynamicZshChoiceExpressions() {
        val candidates = StaticCompletionDefinitions.parseZsh(
            "_arguments '1:branch:(${(f)\$(git branch)})'",
            prefix = ""
        )

        assertEquals(emptyList(), candidates)
    }
}
