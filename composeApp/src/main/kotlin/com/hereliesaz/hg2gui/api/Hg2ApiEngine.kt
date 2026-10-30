package com.hereliesaz.hg2gui.api

import android.content.Context
import com.hereliesaz.hg2gui.terminal.ExecutionPolicy
import com.hereliesaz.hg2gui.terminal.TerminalEngine

/*
 * THE one place the signature API constructs a TerminalEngine.
 *
 * Audit M-2: an external EXECUTE is subject to the engine's own headless policy
 * ([ExecutionPolicy.api]) - the engine, not a regex over the command string, is the component that
 * dispatches `hg2package` / `bootstrap` / `vfs mount` / `hg2auth` and so decides whether a caller
 * with no person at the keyboard may run them. The receiver-side string filters
 * (containsForegroundOnlyCommand, containsBackgroundDeniedCommand) remain as defence in depth only.
 */

/** Engine for API-originated commands with no one at the keyboard (EXECUTE / PACKAGE / LIFECYCLE). */
internal fun headlessEngine(context: Context, appInForeground: Boolean): TerminalEngine =
    TerminalEngine(context, policy = ExecutionPolicy.api(appInForeground))

/**
 * Engine for a root/ADB command the user explicitly approved on screen. It is only used through
 * `runApprovedAuthority`, which takes the approved string as one argument and applies no policy of
 * its own (the approval dialog is the consent); the API policy is still set so that nothing else
 * can be run through this instance with more rights than an API caller has.
 */
internal fun approvedAuthorityEngine(context: Context): TerminalEngine =
    TerminalEngine(context, policy = ExecutionPolicy.api(appInForeground = true))
