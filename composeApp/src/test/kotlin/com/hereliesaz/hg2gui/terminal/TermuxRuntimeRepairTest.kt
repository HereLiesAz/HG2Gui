package com.hereliesaz.hg2gui.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxRuntimeRepairTest {

    @Test
    fun isShellShebang_acceptsShAndBash() {
        assertTrue(TermuxRuntimeRepair.isShellShebang("#!/data/data/com.termux/files/usr/bin/sh"))
        assertTrue(TermuxRuntimeRepair.isShellShebang("#! /usr/bin/bash -e"))
        assertTrue(TermuxRuntimeRepair.isShellShebang("#!/usr/bin/env bash"))
        assertTrue(TermuxRuntimeRepair.isShellShebang("#!/usr/bin/env -S sh"))
    }

    @Test
    fun isShellShebang_rejectsOtherInterpreters() {
        assertFalse(TermuxRuntimeRepair.isShellShebang("#!/usr/bin/python3"))
        assertFalse(TermuxRuntimeRepair.isShellShebang("#!/usr/bin/env perl"))
        assertFalse(TermuxRuntimeRepair.isShellShebang("#!/usr/bin/zsh"))
        assertFalse(TermuxRuntimeRepair.isShellShebang("echo not a script"))
        assertFalse(TermuxRuntimeRepair.isShellShebang(""))
    }
}
