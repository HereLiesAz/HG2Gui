package com.hereliesaz.hg2gui.api

import android.content.Context
import com.hereliesaz.hg2gui.terminal.TerminalEngine

/*
 * THE one place the signature API constructs a TerminalEngine.
 *
 * Audit M-2: an external EXECUTE must be subject to the engine's own headless/background policy
 * (deny hg2package enable/disable/isolate/update, bootstrap, vfs mount, ... when no user is
 * present), because the engine - not a regex over the command string - is the component that
 * actually dispatches those verbs. The receiver-side string filters (containsForegroundOnlyCommand,
 * containsBackgroundDeniedCommand) stay as defence in depth only.
 *
 * HOOKUP (the single line to change once TerminalEngine exposes its policy parameter):
 * replace `TerminalEngine(context)` in [headlessEngine] with
 * `TerminalEngine(context, <policy>)` using the engine's "headless" policy value (pass
 * `appInForeground` through if the policy distinguishes a present user). [interactiveEngine]
 * deliberately keeps the engine's default (full) policy: it only ever runs a root/ADB command the
 * user has just approved in Hg2ApiAuthorityActivity, which is the one path that must be allowed
 * to execute `hg2auth`.
 */

/** Engine for API-originated commands with no user at the keyboard (EXECUTE / PACKAGE / LIFECYCLE). */
@Suppress("UNUSED_PARAMETER")
internal fun headlessEngine(context: Context, appInForeground: Boolean): TerminalEngine =
    TerminalEngine(context) // <- CONNECT: TerminalEngine(context, <headless policy>)

/** Engine for a root/ADB command the user explicitly approved on screen (full policy). */
internal fun interactiveEngine(context: Context): TerminalEngine = TerminalEngine(context)
