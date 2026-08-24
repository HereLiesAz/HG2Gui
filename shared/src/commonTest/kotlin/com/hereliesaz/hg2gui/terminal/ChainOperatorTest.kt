package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ChainOperatorTest {

    @Test
    fun foldsASegmentOntoAnEmptyPrefixWithTheOperatorBetween() {
        assertEquals("ls | ", chainSegment("", "ls", ChainOperator.Pipe))
    }

    @Test
    fun foldsOntoAnAlreadyComposedPrefix() {
        assertEquals("ls | grep foo && ", chainSegment("ls | ", "grep foo", ChainOperator.And))
    }

    @Test
    fun trimsTheIncomingSegment() {
        assertEquals("ls | ", chainSegment("", "  ls  ", ChainOperator.Pipe))
    }

    @Test
    fun blankSegmentLeavesThePrefixUntouched() {
        assertEquals("ls | ", chainSegment("ls | ", "   ", ChainOperator.And))
        assertEquals("", chainSegment("", "", ChainOperator.Pipe))
    }

    @Test
    fun eachOperatorContributesItsOwnLiteralSymbol() {
        assertEquals("a && ", chainSegment("", "a", ChainOperator.And))
        assertEquals("a || ", chainSegment("", "a", ChainOperator.Or))
        assertEquals("a ; ", chainSegment("", "a", ChainOperator.Then))
        assertEquals("a > ", chainSegment("", "a", ChainOperator.Redirect))
        assertEquals("a >> ", chainSegment("", "a", ChainOperator.Append))
    }
}
