package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.system.Os
import java.io.File
import java.nio.file.Files

object TermuxRuntimeRepair {
    private const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    private const val WRAPPERS = ".hg2gui-script-wrappers.sh"
    private const val PROFILE_LINE = "[ -f \"\$HOME/$WRAPPERS\" ] && . \"\$HOME/$WRAPPERS\""
    private const val APT_KEY_LAUNCHER = "libhg2gui_apt_key.so"
    private const val MAIN_REPO = "https://packages.termux.dev/apt/termux-main"
    private const val ROOT_REPO = "https://packages.termux.dev/apt/termux-root"
    private const val X11_REPO = "https://packages.termux.dev/apt/termux-x11"
    private const val NEEDS_APT_UPDATE = ".hg2gui-needs-update"

    fun repair(context: Context) {
        val prefix = DistroManager.prefixDir(context)
        val bin = File(prefix, "bin")
        if (!prefix.isDirectory || !bin.isDirectory) return
        listOf("bin", "libexec", "lib").forEach { subdir ->
            val root = File(prefix, subdir)
            if (root.exists()) root.walkTopDown().forEach { repairScript(it, prefix) }
        }
        repairMainRepoKey(prefix)
        writeMainRepoSource(prefix)
        writeAptConfig(prefix, context.applicationInfo.nativeLibraryDir)
        writeScriptWrappers(prefix, DistroManager.homeDir(context))
        TermuxElfAudit.audit(context)
    }

    private fun repairScript(file: File, prefix: File) {
        if (!file.isFile || Files.isSymbolicLink(file.toPath()) || isElf(file)) return
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return
        if (bytes.size < 2 || bytes[0] != '#'.code.toByte() || bytes[1] != '!'.code.toByte()) return
        val text = String(bytes, Charsets.UTF_8)
        if (TERMUX_PREFIX in text) runCatching { file.writeText(text.replace(TERMUX_PREFIX, prefix.absolutePath)) }
        runCatching { file.setExecutable(true) }
    }

    private fun repairMainRepoKey(prefix: File) {
        val target = File(prefix, "share/termux-keyring/termux-pacman.gpg")
        if (!target.exists()) return
        val link = File(prefix, "etc/apt/trusted.gpg.d/termux-pacman.gpg")
        link.parentFile?.mkdirs()
        if (runCatching { link.canonicalPath == target.canonicalPath }.getOrDefault(false)) return
        runCatching { link.delete() }
        runCatching { Os.symlink(target.absolutePath, link.absolutePath) }
    }

    private fun writeMainRepoSource(prefix: File) {
        val aptDir = File(prefix, "etc/apt").apply { mkdirs() }
        val source = File(aptDir, "sources.list")
        val desiredSource = "deb $MAIN_REPO stable main\n"
        val sourceChanged = !source.exists() || source.readText() != desiredSource
        source.writeText(desiredSource)

        val termuxDir = File(prefix, "etc/termux").apply { mkdirs() }
        val mirrorDir = File(termuxDir, "mirrors/hg2gui").apply { mkdirs() }
        val descriptor = File(mirrorDir, "packages.termux.dev")
        descriptor.writeText(
            """
            # This file is sourced by pkg
            # HG2Gui pinned Termux primary host
            # Termux primary package host | https://packages.termux.dev
            # Managed by HG2Gui runtime repair.
            WEIGHT=10
            MAIN="$MAIN_REPO"
            ROOT="$ROOT_REPO"
            X11="$X11_REPO"
            """.trimIndent() + "\n"
        )

        val chosen = File(termuxDir, "chosen_mirrors")
        runCatching { chosen.deleteRecursively() }
        runCatching { Os.symlink(descriptor.absolutePath, chosen.absolutePath) }

        val listsDir = File(prefix, "var/lib/apt/lists").apply { mkdirs() }
        val hasPrimaryIndex = listsDir.listFiles().orEmpty().any {
            it.isFile && it.name.contains("packages.termux.dev")
        }
        if (sourceChanged || !hasPrimaryIndex) {
            File(listsDir, NEEDS_APT_UPDATE).writeText("1\n")
        }
    }

    private fun writeAptConfig(prefix: File, nativeLibraryDir: String) {
        val p = prefix.absolutePath
        val aptKeyLauncher = File(nativeLibraryDir, APT_KEY_LAUNCHER)
        File(prefix, "var/cache/apt/archives/partial").mkdirs()
        File(prefix, "var/lib/apt/lists/partial").mkdirs()
        val etc = File(prefix, "etc/apt").apply { mkdirs() }
        File(etc, "apt.conf").writeText(
            """
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
            Dir::Bin::dpkg "$p/bin/dpkg";
            Dir::Bin::gpg "$p/bin/gpgv";
            Dir::Bin::apt-key "${aptKeyLauncher.absolutePath}";
            APT::Key::gpgvcommand "$p/bin/gpgv";
            Acquire::gpgv::Command "$p/bin/gpgv";
            Acquire::https::CaInfo "$p/etc/tls/cert.pem";
            Acquire::Retries "3";
            Acquire::http::Timeout "20";
            Acquire::https::Timeout "20";
            """.trimIndent()
        )
    }

    private fun writeScriptWrappers(prefix: File, home: File) {
        home.mkdirs()
        val scripts = File(prefix, "bin").listFiles().orEmpty()
            .filter { it.isFile && !Files.isSymbolicLink(it.toPath()) && !isElf(it) }
            .map { it.name }
            .sorted()
        File(home, WRAPPERS).writeText(buildString {
            appendLine("# Auto-generated by HG2Gui. Do not edit.")
            scripts.forEach { appendLine("$it() ( source \"\$PREFIX/bin/$it\" \"\$@\" )") }
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
        })
        val profile = File(home, ".bash_profile")
        val existing = if (profile.exists()) profile.readText() else ""
        if (PROFILE_LINE !in existing) {
            profile.appendText((if (existing.isNotEmpty() && !existing.endsWith('\n')) "\n" else "") + PROFILE_LINE + "\n")
        }
    }

    private fun isElf(file: File): Boolean = runCatching {
        file.inputStream().use { input ->
            val m = ByteArray(4)
            input.read(m) == 4 && m[0].toInt() == 0x7f && m[1] == 'E'.code.toByte() &&
                m[2] == 'L'.code.toByte() && m[3] == 'F'.code.toByte()
        }
    }.getOrDefault(false)
}
