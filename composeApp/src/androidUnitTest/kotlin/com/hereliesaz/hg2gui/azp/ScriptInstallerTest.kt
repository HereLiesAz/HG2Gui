package com.hereliesaz.hg2gui.azp

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [commandNameFor] and [shellQuoted] are the pure pieces [ScriptInstaller.install] builds a
 * package's wrapper script from - see ScriptInstaller.kt's class doc. Everything else in
 * `install()` needs a real Context/Termux prefix, so it's exercised manually rather than here.
 */
class ScriptInstallerTest {

    @Test
    fun commandNameFor_stripsDirectoryAndExtension() {
        assertEquals("git-sync", commandNameFor("script/git-sync.sh"))
    }

    @Test
    fun commandNameFor_handlesNoExtension() {
        assertEquals("git-sync", commandNameFor("git-sync"))
    }

    @Test
    fun commandNameFor_fallsBackWhenBlank() {
        // An entry that's just a bare extension (or empty) has no usable filename stem.
        assertEquals("script", commandNameFor(".sh"))
    }

    @Test
    fun shellQuoted_wrapsPlainStringInSingleQuotes() {
        assertEquals("'git'", "git".shellQuoted())
    }

    @Test
    fun shellQuoted_escapesEmbeddedSingleQuote() {
        // The POSIX way to embed a literal ' inside a single-quoted string: close the quote,
        // escape a literal ', reopen the quote.
        assertEquals("'it'\\''s'", "it's".shellQuoted())
    }

    @Test
    fun shellQuoted_leavesSpacesAndGlobsInert() {
        // The whole point of quoting: a value with spaces or shell metacharacters must survive
        // as one literal argument, not be re-split or glob-expanded by the wrapper's shell.
        assertEquals("'--flag *.txt'", "--flag *.txt".shellQuoted())
    }
}
