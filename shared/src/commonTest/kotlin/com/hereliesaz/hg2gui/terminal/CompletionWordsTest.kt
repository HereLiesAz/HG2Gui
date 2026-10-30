package com.hereliesaz.hg2gui.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Quoting, token boundaries and unquoting for completion; pure text, no shell is run. */
class CompletionWordsTest {

    // ---- quote -------------------------------------------------------------------------------

    @Test
    fun quote_leavesOrdinaryNamesAlone() {
        for (name in listOf("main", "feature/login", "README.md", "a-b_c.d", "user@host", "v1.2+3", "~/x", "k=v", "a:b,c%d")) {
            assertEquals(name, CompletionWords.quote(name), name)
        }
    }

    @Test
    fun quote_escapesEveryShellMetacharacter() {
        assertEquals("My\\ File", CompletionWords.quote("My File"))
        assertEquals("a\\;rm\\ -rf\\ ~x", CompletionWords.quote("a;rm -rf ~x"))
        assertEquals("\\\$\\(id\\)", CompletionWords.quote("\$(id)"))
        assertEquals("\\`id\\`", CompletionWords.quote("`id`"))
        assertEquals("a\\&b\\|c\\<d\\>e", CompletionWords.quote("a&b|c<d>e"))
        assertEquals("it\\'s", CompletionWords.quote("it's"))
        assertEquals("say\\\"hi\\\"", CompletionWords.quote("say\"hi\""))
        assertEquals("a\\\\b", CompletionWords.quote("a\\b"))
        assertEquals("\\*.txt\\?\\[x\\]", CompletionWords.quote("*.txt?[x]"))
        assertEquals("\\{a,b\\}", CompletionWords.quote("{a,b}"))
        assertEquals("\\#tag", CompletionWords.quote("#tag"))
        assertEquals("hi\\!", CompletionWords.quote("hi!"))
    }

    @Test
    fun quote_emptyValueIsAnEmptyWord() {
        assertEquals("''", CompletionWords.quote(""))
    }

    @Test
    fun quote_controlCharactersUseAnsiCQuoting() {
        assertEquals("\$'a\\nb'", CompletionWords.quote("a\nb"))
        assertEquals("\$'a\\tb c'", CompletionWords.quote("a\tb c"))
        assertEquals("\$'it\\'s\\\\\\x01'", CompletionWords.quote("it's\\\u0001"))
    }

    @Test
    fun quote_doesNotSplitSurrogatePairs() {
        val emoji = "😀.txt"
        assertEquals(emoji, CompletionWords.quote(emoji))
        assertEquals("😀\\ x", CompletionWords.quote("😀 x"))
    }

    @Test
    fun quote_unicodeLettersAreSafe() {
        assertEquals("café.txt", CompletionWords.quote("café.txt"))
    }

    @Test
    fun quote_thenUnquote_roundTrips() {
        val samples = listOf(
            "plain", "My File", "a;b", "\$(id)", "`id`", "it's", "say \"hi\"", "back\\slash", "tab\there",
            "line\nbreak", "ctl\u0001\u007f", "*?[x]{a,b}#!~", "😀 x", "café au lait", "-rf", "trailing ", " leading"
        )
        for (s in samples) assertEquals(s, CompletionWords.unquote(CompletionWords.quote(s)), s)
    }

    @Test
    fun quote_resultIsAlwaysOneToken() {
        val hostile = listOf("a b", "a;b", "\$(id)", "`id`", "a\nb", "a\tb", "x'y", "x\"y", "x\\ y", "a|b&c")
        for (s in hostile) {
            val q = CompletionWords.quote(s)
            assertEquals(0, CompletionWords.currentTokenStart(q), "single token: $q")
            assertEquals("cmd ".length, CompletionWords.currentTokenStart("cmd $q"), "after cmd: $q")
        }
    }

    // ---- currentTokenStart -------------------------------------------------------------------

    @Test
    fun tokenStart_plainWords() {
        assertEquals(0, CompletionWords.currentTokenStart(""))
        assertEquals(0, CompletionWords.currentTokenStart("gi"))
        assertEquals(4, CompletionWords.currentTokenStart("git "))
        assertEquals(4, CompletionWords.currentTokenStart("git sw"))
        assertEquals(5, CompletionWords.currentTokenStart("git  sw"))
    }

    @Test
    fun tokenStart_splitsOnAnyWhitespaceNotJustSpace() {
        assertEquals(3, CompletionWords.currentTokenStart("ls\tfo"))
        assertEquals(3, CompletionWords.currentTokenStart("ls\nfo"))
    }

    @Test
    fun tokenStart_escapedSpaceKeepsTheWordOpen() {
        assertEquals(3, CompletionWords.currentTokenStart("cd My\\ Fi"))
        assertEquals(3, CompletionWords.currentTokenStart("cd My\\ "))
    }

    @Test
    fun tokenStart_quotesKeepTheWordOpen() {
        assertEquals(3, CompletionWords.currentTokenStart("cd \"My Fi"))
        assertEquals(3, CompletionWords.currentTokenStart("cd 'My Fi"))
        assertEquals(3, CompletionWords.currentTokenStart("cd \$'My\\' Fi"))
        // A closed quote ends being special: the next space splits again.
        assertEquals(10, CompletionWords.currentTokenStart("cd 'My F' x"))
    }

    @Test
    fun tokenStart_escapedQuoteDoesNotOpenAQuote() {
        assertEquals(8, CompletionWords.currentTokenStart("echo \\' x"))
        assertEquals(8, CompletionWords.currentTokenStart("echo \\\" x"))
    }

    @Test
    fun tokenStart_escapedDollarThenQuoteIsPlainSingleQuote() {
        // \$ then ' : the quote is an ordinary single quote, so backslash is NOT an escape inside.
        assertEquals(10, CompletionWords.currentTokenStart("ls \\$'a\\' b"))
    }

    // ---- unquote -----------------------------------------------------------------------------

    @Test
    fun unquote_removesQuotingWithoutExpanding() {
        assertEquals("My File", CompletionWords.unquote("My\\ File"))
        assertEquals("My F", CompletionWords.unquote("\"My F"))
        assertEquals("My F", CompletionWords.unquote("'My F"))
        assertEquals("\$HOME", CompletionWords.unquote("'\$HOME'"))
        assertEquals("a\"b", CompletionWords.unquote("\"a\\\"b\""))
        assertEquals("a\\nb", CompletionWords.unquote("\"a\\nb\""))
        assertEquals("a\nb\tc", CompletionWords.unquote("\$'a\\nb\\tc'"))
        assertEquals("\u0001", CompletionWords.unquote("\$'\\x01'"))
        assertEquals("A", CompletionWords.unquote("\$'\\101'"))
        assertEquals("ab", CompletionWords.unquote("a''b"))
        assertEquals("", CompletionWords.unquote(""))
    }

    @Test
    fun unquote_danglingBackslashIsDropped() {
        assertEquals("abc", CompletionWords.unquote("abc\\"))
    }

    @Test
    fun unquote_dollarWithoutQuoteStaysLiteral() {
        assertEquals("\$x", CompletionWords.unquote("\$x"))
        assertEquals("\$", CompletionWords.unquote("\$"))
    }

    @Test
    fun isSafeChar_matchesTheQuoteWhitelist() {
        assertTrue(CompletionWords.isSafeChar('a'))
        assertTrue(CompletionWords.isSafeChar('/'))
        assertFalse(CompletionWords.isSafeChar(' '))
        assertFalse(CompletionWords.isSafeChar(';'))
    }
}
