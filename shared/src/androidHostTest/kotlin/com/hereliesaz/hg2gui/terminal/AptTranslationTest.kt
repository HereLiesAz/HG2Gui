package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AptTranslationTest {

    private fun plan(line: String) = AptTranslation.plan(CommandLine.words(line)!!)

    private fun translated(line: String): String? = (plan(line) as? AptTranslation.Plan.Translate)?.pkgLine

    @Test
    fun supportedOperations_translateToPkg() {
        val cases = listOf(
            "apt install git" to "pkg install git",
            "apt-get install -y git curl" to "pkg install git curl",
            "apt -y install git" to "pkg install git",
            "apt install -yq --no-install-recommends git" to "pkg install git",
            "apt-get -qq remove git" to "pkg remove git",
            "apt purge git" to "pkg purge git",
            "apt update" to "pkg update",
            "apt-get update -y" to "pkg update",
            "apt upgrade" to "pkg upgrade",
            "apt dist-upgrade" to "pkg upgrade",
            "apt full-upgrade -y" to "pkg upgrade",
            "apt clean" to "pkg clean",
            "apt-get autoclean" to "pkg clean"
        )
        for ((line, expected) in cases) assertEquals(line, expected, translated(line))
    }

    @Test
    fun operationIsTheFirstNonOptionWord_notAnyWordThatLooksLikeOne() {
        // B-2: these used to become a real `pkg upgrade` / `pkg remove foo` / `pkg update`.
        val readOnly = listOf("apt search upgrade", "apt search remove foo", "apt show update", "apt list --installed install", "apt policy purge")
        for (line in readOnly) assertTrue(line, plan(line) is AptTranslation.Plan.PassThrough)
    }

    @Test
    fun dryRunIsRefusedNotExecuted() {
        val cases = listOf(
            "apt install -s foo", "apt-get --dry-run install foo", "apt -s install foo", "apt upgrade --simulate",
            "apt-get remove --just-print foo", "apt install --no-act foo"
        )
        for (line in cases) {
            val result = plan(line)
            assertTrue(line, result is AptTranslation.Plan.Refuse)
            assertTrue(line, (result as AptTranslation.Plan.Refuse).message.contains("Dry run"))
        }
    }

    @Test
    fun flagsThatWouldBeDroppedAreRefused() {
        val cases = listOf(
            "apt remove --purge foo", "apt install --reinstall foo", "apt install -f foo",
            "apt install --allow-downgrades foo", "apt upgrade --with-new-pkgs", "apt install -o Foo=bar foo"
        )
        for (line in cases) assertTrue(line, plan(line) is AptTranslation.Plan.Refuse)
    }

    @Test
    fun unsupportedMutatingOperationsAreRefused_includingAutoremove() {
        val cases = listOf(
            "apt autoremove", "apt-get autoremove -y", "apt autopurge", "apt reinstall foo",
            "apt build-dep foo", "apt satisfy foo"
        )
        for (line in cases) {
            val result = plan(line)
            assertTrue(line, result is AptTranslation.Plan.Refuse)
            assertTrue(line, (result as AptTranslation.Plan.Refuse).message.contains("not supported"))
        }
    }

    @Test
    fun badOperandsAreRefused() {
        val cases = listOf(
            "apt install", "apt remove", "apt install foo=1.2", "apt install foo/stable", "apt install ./x.deb",
            "apt install 'a;b'", "apt update foo", "apt upgrade foo", "apt clean foo"
        )
        for (line in cases) assertTrue(line, plan(line) is AptTranslation.Plan.Refuse)
    }

    @Test
    fun aptitudeNeverReachesTheRealTool() {
        assertTrue(plan("aptitude install foo") is AptTranslation.Plan.Refuse)
        assertTrue(plan("aptitude") is AptTranslation.Plan.Refuse)
        assertTrue(plan("aptitude search foo") is AptTranslation.Plan.PassThrough)
    }

    @Test
    fun unknownOperationIsPassedThroughUnlessAMutatingNameHidesAfterAnOptionValue() {
        assertTrue(plan("apt frobnicate") is AptTranslation.Plan.PassThrough)
        assertTrue(plan("apt --version") is AptTranslation.Plan.PassThrough)
        assertTrue(plan("apt") is AptTranslation.Plan.PassThrough)
        assertTrue(plan("apt --weird VALUE install foo") is AptTranslation.Plan.Refuse)
    }

    @Test
    fun nonAptCommandsAreNotApt() {
        assertTrue(plan("pkg install x") is AptTranslation.Plan.NotApt)
        assertTrue(plan("ls") is AptTranslation.Plan.NotApt)
        assertTrue(AptTranslation.plan(emptyList()) is AptTranslation.Plan.NotApt)
    }

    @Test
    fun pkgFlagsThatTheManagerWouldSilentlyDropAreRefused() {
        val refused = listOf("pkg install -s foo", "pkg install --reinstall foo", "pkg remove --purge foo", "hg2pkg install --dry-run foo")
        for (line in refused) assertTrue(line, AptTranslation.pkgFlagRefusal(CommandLine.words(line)!!) != null)
        val fine = listOf("pkg install -y git", "pkg install git curl", "pkg upgrade 'git'", "pkg search -x foo", "pkg list-installed", "pkg update", "ls -s")
        for (line in fine) assertNull(line, AptTranslation.pkgFlagRefusal(CommandLine.words(line)!!))
    }

    @Test
    fun isMutating_classifiesCommandsHg2GuiCouldNotRoute() {
        fun mutating(line: String) = AptTranslation.isMutating(CommandLine.words(line)!!)
        assertTrue(mutating("apt install foo"))
        assertTrue(mutating("/usr/bin/apt-get remove foo"))
        assertTrue(mutating("apt autoremove"))
        assertTrue(mutating("pkg install foo"))
        assertTrue(mutating("pkg upgrade"))
        assertTrue(mutating("aptitude"))
        assertFalse(mutating("apt search foo"))
        assertFalse(mutating("apt-get -s install foo"))
        assertFalse(mutating("pkg list-installed"))
        assertFalse(mutating("pkg search foo"))
        assertFalse(mutating("pkg"))
        assertFalse(mutating("ls install"))
    }
}
