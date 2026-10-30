package com.hereliesaz.hg2gui.terminal

import java.io.File
import java.nio.file.Files

/**
 * The single source of truth for the two runtime files HG2Gui generates inside a Termux prefix:
 * `etc/apt/apt.conf` and `$HOME/.hg2gui-script-wrappers.sh`.
 *
 * They used to be written by two different callers with two different contents
 * ([DistroManager] after a bootstrap / on launch, and the app's TermuxRuntimeRepair on every
 * launch), so whichever ran last silently won - e.g. the `dpkg()`/`apt()` shell wrappers and the
 * `Dir::Bin::dpkg` launcher setting vanished until the next repair. Both callers now delegate
 * here, so there is exactly one content and exactly one writer implementation.
 */
object PrefixRuntimeConfig {
    const val WRAPPER_SCRIPT_NAME = ".hg2gui-script-wrappers.sh"

    /** Marker under `var/lib/apt/lists` meaning "the apt lists predate the current sources.list". */
    const val NEEDS_APT_UPDATE = ".hg2gui-needs-update"
    const val APT_KEY_LAUNCHER = "libhg2gui_apt_key.so"
    const val DPKG_LAUNCHER = "libhg2gui_dpkg.so"

    private const val PROFILE_LINE_BODY = "[ -f \"\$HOME/$WRAPPER_SCRIPT_NAME\" ] && . \"\$HOME/$WRAPPER_SCRIPT_NAME\""
    private val SOURCEABLE_SHELLS = setOf("sh", "bash")

    // Each name becomes a bash function definition sourced on every login, so it must never be
    // able to carry shell syntax: letters/digits/`._+-` only, no leading `-` (would parse as an
    // option), at most 64 chars, and never "." or "..".
    private val SAFE_SCRIPT_NAME = Regex("[A-Za-z0-9_.][A-Za-z0-9_.+-]{0,63}")

    /** True iff [name] may be embedded as a shell function identifier in the wrapper profile. */
    fun isSafeScriptName(name: String): Boolean = name != "." && name != ".." && SAFE_SCRIPT_NAME.matches(name)

    /**
     * True iff [shebangLine] (the script's first line) runs it with sh or bash - directly or via
     * `env` - the only scripts the wrapper may `source` into bash. A python/perl/node script
     * sourced into bash would just be parsed as shell. Pure.
     */
    fun isShellShebang(shebangLine: String): Boolean {
        val words = shebangLine.removePrefix("#!").trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        val first = words.firstOrNull()?.substringAfterLast('/')
        val interpreter = if (first == "env") words.drop(1).firstOrNull { !it.startsWith("-") }?.substringAfterLast('/') else first
        return shebangLine.startsWith("#!") && interpreter in SOURCEABLE_SHELLS
    }

    /**
     * The complete `apt.conf`. [prefix] is the LOGICAL prefix apt will see at run time (the final
     * install path), which differs from the directory being written to while a bootstrap is still
     * being staged.
     */
    fun aptConfText(prefix: String, nativeLibraryDir: String): String {
        val p = prefix
        val aptKeyLauncher = File(nativeLibraryDir, APT_KEY_LAUNCHER).absolutePath
        val dpkgLauncher = File(nativeLibraryDir, DPKG_LAUNCHER).absolutePath
        return """
            Dir "$p/";
            Dir::Etc "$p/etc/apt/";
            Dir::Etc::sourcelist "$p/etc/apt/sources.list";
            Dir::Etc::sourceparts "$p/etc/apt/sources.list.d/";
            Dir::Etc::vendorlist "$p/etc/apt/vendors.list";
            Dir::Etc::vendorparts "$p/etc/apt/vendors.list.d/";
            Dir::Etc::main "$p/etc/apt/apt.conf";
            Dir::Etc::parts "$p/etc/apt/apt.conf.d/";
            Dir::Etc::preferences "$p/etc/apt/preferences";
            Dir::Etc::preferencesparts "$p/etc/apt/preferences.d/";
            Dir::Etc::trusted "$p/etc/apt/trusted.gpg";
            Dir::Etc::trustedparts "$p/etc/apt/trusted.gpg.d/";
            Dir::State "$p/var/lib/apt/";
            Dir::State::lists "$p/var/lib/apt/lists/";
            Dir::State::status "$p/var/lib/dpkg/status";
            Dir::Cache "$p/var/cache/apt/";
            Dir::Cache::archives "$p/var/cache/apt/archives/";
            Dir::Log "$p/var/log/apt/";
            Dir::Bin::methods "$p/lib/apt/methods/";
            Dir::Bin::solvers:: "$p/lib/apt/solvers/";
            Dir::Bin::dpkg "$dpkgLauncher";
            Dir::Bin::gpg "$p/bin/gpgv";
            Dir::Bin::apt-key "$aptKeyLauncher";
            APT::Key::gpgvcommand "$p/bin/gpgv";
            Acquire::gpgv::Command "$p/bin/gpgv";
            Acquire::https::CaInfo "$p/etc/tls/cert.pem";
            Acquire::Retries "3";
            Acquire::http::Timeout "20";
            Acquire::https::Timeout "20";
        """.trimIndent()
    }

    /**
     * Writes `etc/apt/apt.conf` (and the directories apt will not create on demand) under [root].
     * [root] is where the files physically go; [logicalPrefix] is the path written into the
     * config. They are the same directory except while a bootstrap is extracted into a staging
     * directory that is renamed into place afterwards.
     */
    fun writeAptConf(root: File, logicalPrefix: File, nativeLibraryDir: String) {
        // Real Termux only ever runs from a live install apt/dpkg have already been managing -
        // var/lib/apt and var/cache/apt (and their "partial" subdirs, where an in-progress
        // download or list fetch lands before being renamed into place) get created the first
        // time some package's own postinst or dpkg trigger touches them. A bootstrap zip is a
        // frozen snapshot, so neither tree exists in it - and apt will not mkdir -p "partial" on
        // demand ("Archives directory ... is missing" is a hard Acquire error).
        File(root, "var/cache/apt/archives/partial").mkdirs()
        File(root, "var/lib/apt/lists/partial").mkdirs()
        File(root, "var/lib/dpkg").mkdirs()
        val etc = File(root, "etc/apt").apply { mkdirs() }
        AtomicFiles.write(
            File(etc, "apt.conf"),
            aptConfText(logicalPrefix.absolutePath, nativeLibraryDir).toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * The sourced wrapper profile: one `name() ( source "$PREFIX/bin/name" "$@" )` per shell
     * script, plus `dpkg()`/`apt()` functions routing to the APK-installed launcher / the
     * stale-index-aware apt entry point. [scripts] must already be filtered with
     * [isSafeScriptName].
     */
    fun wrapperScriptText(scripts: List<String>, dpkgLauncher: String): String = buildString {
        appendLine("# Auto-generated by HG2Gui. Do not edit.")
        appendLine("export DPKG_ADMINDIR=\"\$PREFIX/var/lib/dpkg\"")
        scripts.forEach { appendLine("$it() ( source \"\$PREFIX/bin/$it\" \"\$@\" )") }
        appendLine()
        appendLine("dpkg() {")
        appendLine("  ${shellQuoted(dpkgLauncher)} \"\$@\"")
        appendLine("}")
        appendLine()
        appendLine("apt() {")
        appendLine("  _hg2gui_marker=\"\$PREFIX/var/lib/apt/lists/$NEEDS_APT_UPDATE\"")
        appendLine("  if [ -f \"\$_hg2gui_marker\" ] && [ \"\${1:-}\" != update ]; then")
        appendLine("    \"\$PREFIX/bin/apt\" update || return \$?")
        appendLine("    rm -f \"\$_hg2gui_marker\"")
        appendLine("  fi")
        appendLine("  \"\$PREFIX/bin/apt\" \"\$@\"")
        appendLine("  _hg2gui_rc=\$?")
        appendLine("  if [ \"\${1:-}\" = update ] && [ \$_hg2gui_rc -eq 0 ]; then rm -f \"\$_hg2gui_marker\"; fi")
        appendLine("  return \$_hg2gui_rc")
        appendLine("}")
    }

    /**
     * Regenerates [home]/[WRAPPER_SCRIPT_NAME] from the shell scripts currently in `bin/` and makes
     * sure `.bash_profile` sources it. Idempotent; the user's own dotfile content is never
     * rewritten, only appended to once.
     *
     * Android's write-xor-execute policy (see BootstrapManifest) only exempts files the OS itself
     * installed as a native lib, so `pkg`, `apt-key` and ~77 other bash scripts from the upstream
     * bootstrap can come back "Permission denied" when exec'd. `source`ing the script inside a
     * subshell sidesteps that: a subshell is a plain fork() with no execve(), and running in one
     * keeps a script's own `set -e`/`exit` from ending the session that ran it. Covers commands
     * typed into the interactive shell only - an ELF program shelling out to one of these scripts
     * still goes through the OS's execve().
     */
    fun writeScriptWrappers(prefix: File, home: File, nativeLibraryDir: String) {
        home.mkdirs()
        val scripts = File(prefix, "bin").listFiles().orEmpty()
            .filter { it.isFile && !Files.isSymbolicLink(it.toPath()) && !isElf(it) && isShellShebang(firstLine(it)) }
            .map { it.name }
            .filter(::isSafeScriptName)
            .sorted()
        val dpkgLauncher = File(nativeLibraryDir, DPKG_LAUNCHER).absolutePath
        AtomicFiles.write(
            File(home, WRAPPER_SCRIPT_NAME),
            wrapperScriptText(scripts, dpkgLauncher).toByteArray(Charsets.UTF_8)
        )
        val profile = File(home, ".bash_profile")
        val existing = if (profile.exists()) profile.readText() else ""
        if (PROFILE_LINE_BODY !in existing) {
            profile.appendText((if (existing.isNotEmpty() && !existing.endsWith('\n')) "\n" else "") + PROFILE_LINE_BODY + "\n")
        }
    }

    /** First line of [file] (at most the first 256 bytes), or "" if unreadable. */
    fun firstLine(file: File): String = runCatching {
        file.inputStream().use { input ->
            val head = ByteArray(256)
            val n = input.read(head).coerceAtLeast(0)
            String(head, 0, n, Charsets.UTF_8).lineSequence().firstOrNull().orEmpty()
        }
    }.getOrDefault("")

    /** True iff [file] starts with the ELF magic. */
    fun isElf(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val m = ByteArray(4)
            input.read(m) == 4 && m[0].toInt() == 0x7f && m[1] == 'E'.code.toByte() &&
                m[2] == 'L'.code.toByte() && m[3] == 'F'.code.toByte()
        }
    }.getOrDefault(false)

    private fun shellQuoted(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}
