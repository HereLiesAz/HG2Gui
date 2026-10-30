package com.hereliesaz.hg2gui.azp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validation of manifest-declared `apt` dependency names before `pkg install -y`. */
class ScriptDependenciesTest {

    @Test
    fun acceptsRealPackageNames() {
        for (name in listOf("git", "python", "python-pip", "libc++", "g++", "openssl-tool", "gnupg2", "libx11", "tree", "a1", "lib.so+x")) {
            assertTrue(name, isValidPackageName(name))
        }
    }

    @Test
    fun rejectsOptionsAndInjection() {
        for (name in listOf("-y", "--allow-unauthenticated", "-oAPT::Get::AllowUnauthenticated=1", "", " ", "git foo", "git;rm", "\$(id)", "`id`", "a|b", "a&b", "a>b", "a'b", "a\"b", "a\nb", "../x", "a/b", ".hidden", "+x", "Git", "PYTHON", "é", "a*", "a?")) {
            assertFalse("should reject '$name'", isValidPackageName(name))
        }
    }

    @Test
    fun rejectsOverlongNames() {
        assertTrue(isValidPackageName("a".repeat(100)))
        assertFalse(isValidPackageName("a".repeat(101)))
    }

    @Test
    fun firstInvalidDependency_nullForCleanLists() {
        assertNull(firstInvalidDependency(emptyList()))
        assertNull(firstInvalidDependency(listOf("git", "curl")))
    }

    @Test
    fun firstInvalidDependency_reportsTheFirstOffender() {
        assertEquals("--force-yes", firstInvalidDependency(listOf("git", "--force-yes", "-x")))
    }

    @Test
    fun firstInvalidDependency_limitsListLength() {
        val many = (1..33).map { "pkg$it" }
        assertTrue(firstInvalidDependency(many)!!.contains("32"))
        assertNull(firstInvalidDependency(many.take(32)))
    }
}
