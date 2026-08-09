package com.hereliesaz.hg2gui.azp

import android.content.Context
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.terminal.TerminalEngine
import java.io.File

/**
 * Turns an installed `kind:"script"` package's [ScriptDto] into an actually-runnable command -
 * the `pkg install` step `spec/script.md` describes, mirroring how a real package manager (apt,
 * brew, ...) resolves a package's declared dependencies before exposing its binary. Only the
 * `"apt"` dependency namespace means anything here, since HG2Gui's real OS access is entirely
 * through its bundled Termux prefix (see [DistroManager]) - any other namespace (e.g. `"brew"`)
 * is present-but-unusable, same as an `mcp` package's unrecognized `grants` entry.
 *
 * Once dependencies resolve, a thin wrapper is written to `prefix/bin/<command>` that `exec`s the
 * interpreter against the package's own extracted, digest-verified entry file - never copying or
 * rewriting those bytes, so the integrity check [AzpInstaller] already ran keeps covering exactly
 * what runs. Requires a Termux bootstrap to already be installed (see [DistroManager.isInstalled])
 * - there is no OS package manager, and nothing to install a package's dependencies with, until
 * the user has bootstrapped one.
 */
object ScriptInstaller {

    sealed class Result {
        /** The wrapper is written and executable, callable as [command] from any shell session. */
        data class Installed(val command: String) : Result()
        /** `pkg install -y` for one or more declared dependencies exited non-zero; [output] is
         *  the shell transcript for diagnosis. Nothing is wired onto PATH. */
        data class DependencyFailed(val output: String) : Result()
        /** No Termux bootstrap yet - there's no `pkg`/`apt` to resolve dependencies with, and no
         *  `prefix/bin` to place a wrapper in. The package's bytes are still on disk from
         *  [AzpInstaller.install]; only becoming runnable is deferred. */
        object BootstrapMissing : Result()
    }

    suspend fun install(
        context: Context,
        id: String,
        version: String,
        script: ScriptDto,
    ): Result {
        if (!DistroManager.isInstalled(context)) return Result.BootstrapMissing

        val prefix = DistroManager.prefixDir(context)
        val engine = TerminalEngine(context)
        try {
            val aptPackages = script.dependencies["apt"].orEmpty()
            if (aptPackages.isNotEmpty()) {
                val (output, exitCode) = engine.runToCompletion(
                    "pkg install -y " + aptPackages.joinToString(" ") { it.shellQuoted() }
                )
                if (exitCode != 0) return Result.DependencyFailed(output)
            }

            val entryFile = File(AzpInstaller.rootDir(context, id, version), script.entry)
            val command = script.command?.takeIf { it.isNotBlank() } ?: commandNameFor(script.entry)
            val interpreter = resolveOnPrefix(prefix, script.interpreter)
            val argsSuffix = script.args.joinToString("") { " " + it.shellQuoted() }

            val wrapper = File(prefix, "bin/$command")
            wrapper.writeText(
                "#!${File(prefix, "bin/bash").absolutePath}\n" +
                    "exec ${interpreter.shellQuoted()} ${entryFile.absolutePath.shellQuoted()}$argsSuffix \"\$@\"\n"
            )
            wrapper.setExecutable(true)
            return Result.Installed(command)
        } finally {
            engine.destroy()
        }
    }

    /** The interpreter's absolute path within the prefix if it's there (e.g. just installed as a
     *  dependency), otherwise its bare name - resolved via the shell's own inherited PATH, which
     *  ShellSession already points at `prefix/bin` for every Termux-backed session. */
    private fun resolveOnPrefix(prefix: File, interpreter: String): String {
        val direct = File(prefix, "bin/$interpreter")
        return if (direct.canExecute()) direct.absolutePath else interpreter
    }
}

/** `script.entry`'s filename, extension stripped - the fallback `command` name when a package
 *  omits one, e.g. `script/git-sync.sh` -> `git-sync`. Pure and Context-free, split out from
 *  [ScriptInstaller.install] specifically so it's unit-testable without a Robolectric/instrumented
 *  Android runtime - see ScriptInstallerTest. */
internal fun commandNameFor(entry: String): String =
    File(entry).nameWithoutExtension.ifBlank { "script" }

/** Single-quotes [this] for safe interpolation into a generated shell wrapper, escaping any
 *  embedded single quote the POSIX-shell way (`'\''`). Pure and Context-free for the same
 *  testability reason as [commandNameFor]. */
internal fun String.shellQuoted(): String = "'" + replace("'", "'\\''") + "'"
