package com.hereliesaz.hg2gui.terminal

import android.content.Context
import android.os.Build
import android.system.Os
import com.hereliesaz.hg2gui.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Downloads and installs a real Termux bootstrap - the same rootfs archive upstream Termux
 * ships - giving this app a genuine `usr/` prefix with `apt`/`pkg` and real coreutils, rather
 * than the bare, toybox-only `/system/bin/sh` Android provides on its own.
 */
object DistroManager {

    // Pinned, not /releases/latest/download/ - BootstrapManifest's 330 executables/libraries are
    // a frozen snapshot of exactly this release's bin/, lib/, and libexec/, bundled into the APK
    // itself (see that file's own doc comment for why). Pointing this at whatever "latest" is at
    // download time would let the two drift out of sync - a newer release's dpkg database, apt
    // config, or package lists disagreeing with the actual (older, bundled) binary they describe.
    // Bumping this to a new release means regenerating BootstrapManifest.kt and jniLibs/arm64-v8a/
    // from that release's own bootstrap-aarch64.zip at the same time, not on its own.
    private const val BOOTSTRAP_RELEASE_TAG = "bootstrap-2026.08.16-r1+apt.android-7"
    private const val BOOTSTRAP_BASE_URL =
        "https://github.com/termux/termux-packages/releases/download/$BOOTSTRAP_RELEASE_TAG"
    private const val BOOTSTRAP_FILE = "bootstrap.zip"
    private const val SYMLINKS_ENTRY = "SYMLINKS.txt"
    // U+2190 LEFTWARDS ARROW - the field separator Termux's own bootstrap builder puts between
    // a symlink's target and the path it should be created at.
    private const val SYMLINK_SEPARATOR = "←"

    // The absolute prefix a handful of SYMLINKS.txt targets hardcode against Termux's own
    // package name - see extractBootstrap's symlink pass for why this needs rewriting.
    private const val HARDCODED_TERMUX_PREFIX = "/data/data/com.termux/files/usr"

    private fun bootstrapArch(): String? = when (Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "i686"
        else -> null
    }

    fun prefixDir(context: Context): File = File(context.filesDir, "usr")
    fun homeDir(context: Context): File = File(context.filesDir, "home")

    // Requires a working bin/bash, not just the directories existing: CommandTree.scanShell()
    // lists whatever's actually sitting in usr/bin/ and gates entirely on this check, so an
    // existence-only check let it advertise real binaries (apt, coreutils, ...) from an install
    // whose bash is missing, a dangling symlink, or otherwise non-executable - ShellSession
    // .forAndroid() already requires this same bin/bash.canExecute() before it'll use the
    // bootstrap shell at all, so a command picked from such a tree fell through to the bare
    // /system/bin/sh fallback and failed with its "inaccessible or not found" instead of
    // whatever apt/coreutils would have actually said.
    //
    // bin/bash not executing is the one failure this repairs before giving up on it: it's also
    // exactly what a stale exec-exempt link looks like (see repairExecLinks's own doc comment) -
    // a routine app update, not a corrupted or missing install - so a single cheap relink pass is
    // tried here before this reports "not installed" and TerminalActivity's own onCreate falls
    // back to wiping the prefix and re-downloading the whole multi-megabyte archive from scratch.
    fun isInstalled(context: Context): Boolean {
        val usrDir = prefixDir(context)
        if (!usrDir.isDirectory || !File(usrDir, "bin").isDirectory) return false
        val bash = File(usrDir, "bin/bash")
        if (bash.canExecute()) return true
        repairExecLinks(context)
        return bash.canExecute()
    }

    /**
     * Re-links every manifest-covered exec-exempt binary/library (see [BootstrapManifest]) against
     * the CURRENT [Context.getApplicationInfo]`.nativeLibraryDir`. Android assigns that directory a
     * new path on every reinstall or update of this APK - even though the rest of a bootstrap
     * install ([prefixDir], extracted once) is completely unaffected by that - so the links
     * [extractBootstrap] created against the PREVIOUS install's nativeLibraryDir go stale purely
     * from updating the app, with nothing actually wrong in the extracted rootfs itself. A no-op
     * when [prefixDir] doesn't exist yet (nothing to repair before a first bootstrap) and equally a
     * no-op once every link already resolves, so safe to call unconditionally.
     */
    private fun repairExecLinks(context: Context) {
        val prefix = prefixDir(context)
        if (!prefix.isDirectory) return
        linkManifestEntries(prefix, context.applicationInfo.nativeLibraryDir)
    }

    fun bootstrap(context: Context, client: OkHttpClient): Flow<String> = flow {
        val arch = bootstrapArch()
        if (arch == null) {
            emit("Error: no Termux bootstrap is published for this device's ABI (${Build.SUPPORTED_ABIS.joinToString()}).")
            return@flow
        }

        val url = "$BOOTSTRAP_BASE_URL/bootstrap-$arch.zip"
        emit("Starting bootstrap process...")

        val destFile = File(context.cacheDir, BOOTSTRAP_FILE)
        if (destFile.exists()) destFile.delete()

        emit("Downloading rootfs from $url...")

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Download failed: HTTP ${response.code}")

                val body = response.body ?: throw Exception("Empty response body")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            // Progress update every ~1MB
                            if (downloadedBytes % (1024 * 1024) < 8192) {
                                val total = if (totalBytes > 0) "${totalBytes / (1024 * 1024)}MB" else "?"
                                emit("Downloaded: ${downloadedBytes / (1024 * 1024)}MB / $total")
                            }
                        }
                    }
                }
            }

            emit("Download complete. Extracting...")

            val prefix = prefixDir(context)
            extractBootstrap(destFile, prefix, context.applicationInfo.nativeLibraryDir)
            homeDir(context).mkdirs()

            emit("Extraction complete. Fixing permissions...")
            makeBinariesExecutable(prefix)

            emit("Bootstrap successful! You can now use 'apt', 'pkg', and real coreutils.")
        } catch (e: Exception) {
            // UX-6: isInstalled() is existence-only (prefix/ and prefix/bin/ both exist) - and
            // extractBootstrap() creates prefix/bin/ the moment its first file lands, long before
            // the archive finishes. A failure partway through (storage full, backgrounded,
            // corrupted archive) used to leave exactly that directory behind: "installed" forever,
            // with no retry, same as AzpInstaller already rolls back a partial install to avoid.
            prefixDir(context).deleteRecursively()
            emit("Error during bootstrap: ${e.message}")
            Utils.log(e)
        } finally {
            if (destFile.exists()) destFile.delete()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Extracts a Termux bootstrap zip into [prefix]. Regular files unpack directly, except for
     * the ~330 executables/libraries [BootstrapManifest] covers - those are skipped here (their
     * content from the zip would just be inert, blocked from ever executing - see that file's own
     * doc comment) and symlinked to their bundled, exec-exempt counterpart in [nativeLibraryDir]
     * instead, once every other file has landed. A zip can't carry real symlinks either, so
     * Termux's bootstrap builder lists those separately in a root-level SYMLINKS.txt (one per
     * line, "<target><SEPARATOR><link path>") and they're created here as their own pass, once
     * every real file they might point at already exists.
     */
    private fun extractBootstrap(zipFile: File, prefix: File, nativeLibraryDir: String) {
        prefix.mkdirs()
        val symlinksText = StringBuilder()
        val manifest = BootstrapManifest.ENTRIES.toMap()

        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                when {
                    entry.name == SYMLINKS_ENTRY -> {
                        symlinksText.append(zis.bufferedReader(Charsets.UTF_8).readText())
                    }
                    entry.isDirectory -> {
                        File(prefix, entry.name).mkdirs()
                    }
                    entry.name in manifest -> {
                        // Still needs its own directory to exist, for whatever else in this same
                        // directory isn't manifest-covered - just not this file's own content.
                        File(prefix, entry.name).parentFile?.mkdirs()
                    }
                    else -> {
                        val outFile = File(prefix, entry.name)
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().buffered().use { out -> zis.copyTo(out) }
                        rewriteShebangIfNeeded(outFile, prefix)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        symlinksText.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val separatorIndex = line.indexOf(SYMLINK_SEPARATOR)
            if (separatorIndex < 0) return@forEach
            val rawTarget = line.substring(0, separatorIndex)
            // A handful of targets (apt/pacman keyring files, a couple of bin/ aliases) are
            // absolute paths baked in at Termux's own build time against ITS package name -
            // `/data/data/com.termux/files/usr/...` - which doesn't exist under this app's
            // sandbox at all. Every other target is already relative (resolved against the
            // symlink's own containing directory, so it needs no rewriting) - this only touches
            // the ones that would otherwise dangle.
            val target = if (rawTarget.startsWith(HARDCODED_TERMUX_PREFIX)) {
                prefix.absolutePath + rawTarget.removePrefix(HARDCODED_TERMUX_PREFIX)
            } else {
                rawTarget
            }
            val linkPath = line.substring(separatorIndex + SYMLINK_SEPARATOR.length)
            val linkFile = File(prefix, linkPath)
            linkFile.parentFile?.mkdirs()
            // unlink()s the path itself rather than whatever it points to - a no-op if nothing
            // was there yet, and clears a stale symlink left by a prior failed bootstrap attempt.
            linkFile.delete()
            try {
                Os.symlink(target, linkFile.absolutePath)
            } catch (e: Exception) {
                Utils.log(e)
            }
        }
        linkMainRepoKeyring(prefix)
        linkManifestEntries(prefix, nativeLibraryDir)
        writeAptConf(prefix)
    }

    // Shared by extractBootstrap (fresh install) and repairExecLinks (an existing install whose
    // links now point at a previous, no-longer-valid nativeLibraryDir) - see repairExecLinks's own
    // doc comment for why that happens on nothing more than a routine app update.
    private fun linkManifestEntries(prefix: File, nativeLibraryDir: String) {
        for ((realPath, flatName) in BootstrapManifest.ENTRIES) {
            val linkFile = File(prefix, realPath)
            linkFile.parentFile?.mkdirs()
            linkFile.delete()
            val realFile = File(nativeLibraryDir, flatName)
            try {
                // A hard link shares the target's own inode - and with it, the target's own
                // security context - so there's no separate "does exec permission follow a
                // symlink to its target?" question left to answer for it. Only possible when
                // both paths are on the same filesystem, which prefix (under context.filesDir)
                // and nativeLibraryDir aren't guaranteed to be - falls back to a symlink (this
                // app's only previously-available option) when the OS refuses to link across
                // filesystems.
                Files.createLink(linkFile.toPath(), realFile.toPath())
            } catch (e: Exception) {
                try {
                    Os.symlink(realFile.absolutePath, linkFile.absolutePath)
                } catch (e2: Exception) {
                    Utils.log(e2)
                }
            }
        }
    }

    // The bootstrap zip's own SYMLINKS.txt wires every *community* keyring under
    // share/termux-keyring/ into etc/apt/trusted.gpg.d/ (so apt trusts it without ever running
    // `apt-key add`) - every one of them except termux-pacman.gpg, the key that actually signs
    // the main repo (packages-cf.termux.dev/apt/termux-main, the one `apt`/`pkg update` hits by
    // default). On real Termux that gap is covered by the termux-keyring package's own postinst
    // script; this app's raw zip extraction never runs any dpkg script at all (see this class's
    // own top-level doc comment), so without this, apt has no trusted key for the one repo that
    // matters most and every `apt update`/`pkg update` fails signature verification. Mirrors
    // exactly what SYMLINKS.txt already does for the others, just for the one entry it omits.
    private fun linkMainRepoKeyring(prefix: File) {
        val target = File(prefix, "share/termux-keyring/termux-pacman.gpg")
        if (!target.exists()) return
        val linkFile = File(prefix, "etc/apt/trusted.gpg.d/termux-pacman.gpg")
        linkFile.parentFile?.mkdirs()
        linkFile.delete()
        try {
            Os.symlink(target.absolutePath, linkFile.absolutePath)
        } catch (e: Exception) {
            Utils.log(e)
        }
    }

    // Every script Termux's bootstrap builder compiled - `pkg` among them - gets a shebang line
    // baked at ITS build time against ITS package: `#!/data/data/com.termux/files/usr/bin/bash`.
    // That path doesn't exist under this app's sandbox, so the kernel's own "bad interpreter"
    // check fails before bash (or whatever's named) ever runs - a real command extractBootstrap
    // never touches otherwise, since only the ~330 manifest-covered ELF files need the
    // exec-exemption trick; a plain script just needs its own first line to name a real path.
    //
    // The shebang isn't the only hardcoded reference a script can carry, though - `pkg` itself
    // also calls other bootstrap tools (termux-setup-package-manager among them) by the same
    // ITS-package absolute path, not by $PREFIX or a bare command name that PATH would resolve,
    // so those calls failed with "No such file or directory" even once the shebang fix alone let
    // the script start running at all. Once a file is confirmed to be a script (shebang-gated,
    // same as before - never touches the ~330 manifest-covered ELF files, and never risks
    // corrupting a byte sequence inside a real binary that only coincidentally matches this
    // text), every occurrence of the hardcoded prefix throughout its whole body is rewritten, not
    // just the first line.
    private fun rewriteShebangIfNeeded(file: File, prefix: File) {
        if (file.length() < 3) return
        val bytes = file.readBytes()
        if (bytes.size < 2 || bytes[0] != '#'.code.toByte() || bytes[1] != '!'.code.toByte()) return
        val text = String(bytes, Charsets.UTF_8)
        if (!text.contains(HARDCODED_TERMUX_PREFIX)) return
        file.writeBytes(text.replace(HARDCODED_TERMUX_PREFIX, prefix.absolutePath).toByteArray(Charsets.UTF_8))
    }

    // apt's own Dir::* settings (where it looks for apt.conf.d, sources.list, its package cache,
    // dpkg's status file, ...) are compiled in as absolute paths against Termux's own package too
    // - unlike a shebang line, there's no single first-line fix, since these are baked into
    // apt/libapt-pkg's own binary as C++ string constants apt reads at startup, before it's ever
    // consulted a config file. What apt *does* still honor from the environment is APT_CONFIG - a
    // path to a config file it reads before falling back to those compiled-in defaults - so this
    // writes one, with every Dir:: apt.conf(5) documents pointed at this app's real prefix
    // instead, and ShellSession points APT_CONFIG at it for every bootstrap-tier command.
    //
    // Acquire::https::CaInfo is here for a related but separate reason: the bootstrap zip already
    // ships a real CA bundle at etc/tls/cert.pem (extracted like any other regular file, no fixup
    // needed for the file itself), but on a genuine Termux install that bundle only becomes the
    // thing TLS actually consults once the `ca-certificates` package's own postinst runs `trust
    // extract` to activate it - a dpkg step this extraction never runs, so the bundle just sits
    // there unused otherwise. Pointing apt straight at the raw bundle sidesteps needing that
    // activation step at all.
    private fun writeAptConf(prefix: File) {
        val p = prefix.absolutePath
        val aptConfDir = File(prefix, "etc/apt")
        aptConfDir.mkdirs()

        // Real Termux only ever runs from a live install apt/dpkg have already been managing -
        // var/lib/apt and var/cache/apt (and their own "partial" subdirs, where an in-progress
        // download or list fetch lands before being renamed into place) get created the first
        // time some package's own postinst or dpkg trigger touches them. This zip is a frozen
        // snapshot, not a live install, so neither tree exists in it at all - and unlike most of
        // apt's own Dir::* paths, it won't just mkdir -p these two on demand; a missing "partial"
        // is a hard Acquire error ("Archives directory ... is missing").
        File(prefix, "var/cache/apt/archives/partial").mkdirs()
        File(prefix, "var/lib/apt/lists/partial").mkdirs()

        File(aptConfDir, "apt.conf").writeText(
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
            Acquire::https::CaInfo "$p/etc/tls/cert.pem";
            """.trimIndent()
        )
    }

    private fun makeBinariesExecutable(prefix: File) {
        val manifestPaths = BootstrapManifest.ENTRIES.mapTo(HashSet()) { (realPath, _) -> File(prefix, realPath) }
        listOf("bin", "libexec", "lib").forEach { sub ->
            File(prefix, sub).walkTopDown().forEach { f ->
                // Manifest-covered paths are a symlink or hard link into nativeLibraryDir now,
                // not this app's own extracted file - already executable (the OS sets that at
                // install time), and not something this app can, or needs to, chmod itself.
                if (f.isFile && !Files.isSymbolicLink(f.toPath()) && f !in manifestPaths) f.setExecutable(true)
            }
        }
    }

    private data class BundledScript(val name: String, val body: String)

    private val BUNDLED_SCRIPTS: List<BundledScript>
        get() = listOf(
            BundledScript("osint-lookup", OSINT_SCRIPT_BODY),
            BundledScript("net-inventory", NET_INVENTORY_SCRIPT_BODY),
            BundledScript("harden-check", HARDEN_CHECK_SCRIPT_BODY),
            BundledScript("sysinfo", SYSINFO_SCRIPT_BODY)
        )

    /** Idempotent: a no-op before a bootstrap exists, and only writes a script once it's
     *  actually missing - called both right after a fresh bootstrap and on every launch
     *  thereafter, so an install from before one of these scripts existed picks it up too. */
    fun ensureBundledScripts(context: Context) {
        if (!isInstalled(context)) return
        val prefix = prefixDir(context)
        val shebang = "#!${File(prefix, "bin/bash").absolutePath}\n"
        for (script in BUNDLED_SCRIPTS) {
            val file = File(prefix, "bin/${script.name}")
            if (file.exists()) continue
            file.writeText(shebang + script.body)
            file.setExecutable(true)
        }
        writeScriptWrapperProfile(prefix, homeDir(context))
    }

    private const val WRAPPER_SCRIPT_NAME = ".hg2gui-script-wrappers.sh"

    // Android's write-xor-execute policy (see BootstrapManifest's own doc comment) only exempts
    // files the OS itself installed as a native lib - real ELF binaries flattened into jniLibs/.
    // `pkg`, `apt-key`, and ~77 others the upstream bootstrap ships as plain bash scripts were
    // never eligible for that trick (a text file can't pass as a native library), so bash's own
    // exec of one - directly typed, or via its own "read the #! line, exec the interpreter"
    // fallback when the kernel can't run the script as-is - can come back "Permission denied"
    // for the interpreter it names, even though that same interpreter runs fine launched
    // directly. `source`ing the script's own content inside a subshell instead sidesteps this
    // entirely: a subshell is a plain fork() with no execve() involved at all, so nothing here
    // depends on whichever OS-level restriction is actually in play - and running inside a
    // subshell, not the interactive shell itself, keeps a script's own `set -e`/`exit` from
    // touching the session that ran it. Regenerated (overwritten, not appended) on every launch
    // so a bootstrap upgrade's added/removed scripts stay in sync - written to its own file, only
    // ever sourced from .bash_profile, so a user's own dotfile customizations are never touched
    // once that first line exists. Covers commands typed directly into this interactive shell
    // only - a real ELF program (apt, dpkg) shelling out to one of these scripts itself still
    // goes through the OS's own execve(), unaffected by a bash function defined in this shell.
    private fun writeScriptWrapperProfile(prefix: File, home: File) {
        val binDir = File(prefix, "bin")
        val scriptNames = binDir.listFiles { f -> f.isFile && !Files.isSymbolicLink(f.toPath()) && !isElf(f) }
            ?.map { it.name }
            ?.sorted()
            .orEmpty()

        val wrapperFile = File(home, WRAPPER_SCRIPT_NAME)
        if (scriptNames.isEmpty()) {
            wrapperFile.delete()
            return
        }
        wrapperFile.writeText(buildString {
            appendLine("# Auto-generated by HG2Gui - regenerated on every launch, do not edit.")
            for (name in scriptNames) {
                appendLine("$name() ( source \"\$PREFIX/bin/$name\" \"\$@\" )")
            }
        })

        val profile = File(home, ".bash_profile")
        if (!profile.exists()) {
            profile.writeText("[ -f \"\$HOME/$WRAPPER_SCRIPT_NAME\" ] && . \"\$HOME/$WRAPPER_SCRIPT_NAME\"\n")
        }
    }

    private fun isElf(file: File): Boolean = try {
        file.inputStream().use { input ->
            val magic = ByteArray(4)
            input.read(magic) == 4 &&
                magic[0].toInt() == 0x7f && magic[1] == 'E'.code.toByte() &&
                magic[2] == 'L'.code.toByte() && magic[3] == 'F'.code.toByte()
        }
    } catch (e: IOException) {
        false
    }

    // Self-scoped, read-only recon for a domain YOU name - your own domain, or one you have a
    // legitimate reason to research (due diligence, checking your own exposure). Whois, DNS
    // records, and certificate-transparency logs (crt.sh) are all passive lookups against
    // publicly published data; nothing here scans, brute-forces, or contacts the target's own
    // infrastructure. Not a tool for surveilling arbitrary third parties.
    private const val OSINT_SCRIPT_BODY = """
set -u
target="${'$'}{1:-}"
if [ -z "${'$'}target" ]; then
  echo "usage: osint-lookup <domain>" >&2
  exit 1
fi

echo "== whois: ${'$'}target =="
if command -v whois >/dev/null 2>&1; then
  whois "${'$'}target" 2>&1 | head -n 40
else
  echo "(whois not installed - pkg install whois)"
fi

echo
echo "== DNS records: ${'$'}target =="
if command -v dig >/dev/null 2>&1; then
  for rtype in A AAAA MX TXT NS; do
    echo "-- ${'$'}rtype --"
    dig +short "${'$'}target" "${'$'}rtype"
  done
else
  echo "(dig not installed - pkg install dnsutils)"
fi

echo
echo "== certificate transparency (crt.sh): ${'$'}target =="
if command -v curl >/dev/null 2>&1; then
  if command -v jq >/dev/null 2>&1; then
    curl -s "https://crt.sh/?q=%25.${'$'}target&output=json" | jq -r '.[].name_value' | sort -u | head -n 40
  else
    curl -s "https://crt.sh/?q=%25.${'$'}target&output=json" | head -c 2000
    echo
    echo "(install jq for readable output - pkg install jq)"
  fi
else
  echo "(curl not installed - pkg install curl)"
fi
"""

    // Local, read-only network self-inventory - interfaces, routes, resolver config on THIS
    // device. Nothing here sends a packet anywhere or touches another host.
    private const val NET_INVENTORY_SCRIPT_BODY = """
set -u
echo "== interfaces =="
if command -v ip >/dev/null 2>&1; then
  ip -brief addr show 2>/dev/null || ip addr show
elif command -v ifconfig >/dev/null 2>&1; then
  ifconfig
else
  echo "(neither ip nor ifconfig installed - pkg install iproute2 or net-tools)"
fi

echo
echo "== routes =="
if command -v ip >/dev/null 2>&1; then
  ip route show
elif command -v route >/dev/null 2>&1; then
  route -n
else
  echo "(neither ip nor route installed - pkg install iproute2 or net-tools)"
fi

echo
echo "== DNS =="
found_resolv=0
for f in /system/etc/resolv.conf /etc/resolv.conf; do
  if [ -r "${'$'}f" ]; then
    echo "-- ${'$'}f --"
    cat "${'$'}f"
    found_resolv=1
  fi
done
if [ -x /system/bin/getprop ]; then
  for p in net.dns1 net.dns2; do
    v=$(/system/bin/getprop "${'$'}p" 2>/dev/null)
    if [ -n "${'$'}v" ]; then
      echo "${'$'}p=${'$'}v"
      found_resolv=1
    fi
  done
fi
[ "${'$'}found_resolv" -eq 1 ] || echo "(no resolver config found)"
"""

    // Self-audit of THIS device's own Termux setup - ssh key permissions, what's actually
    // listening locally, and any world-writable file under $HOME. Nothing here probes another
    // host; every check is against files and sockets that already belong to this install.
    private const val HARDEN_CHECK_SCRIPT_BODY = """
set -u
warn=0

echo "== SSH key hygiene (${'$'}HOME/.ssh) =="
sshdir="${'$'}HOME/.ssh"
if [ -d "${'$'}sshdir" ]; then
  dirmode=$(stat -c '%a' "${'$'}sshdir" 2>/dev/null)
  if [ "${'$'}dirmode" = "700" ]; then
    echo "  [ok] ${'$'}sshdir is 700"
  else
    echo "  [warn] ${'$'}sshdir is ${'$'}dirmode, expected 700 (chmod 700 ${'$'}sshdir)"
    warn=1
  fi
  for f in "${'$'}sshdir"/id_*; do
    [ -e "${'$'}f" ] || continue
    case "${'$'}f" in
      *.pub) continue ;;
    esac
    mode=$(stat -c '%a' "${'$'}f" 2>/dev/null)
    if [ "${'$'}mode" = "600" ]; then
      echo "  [ok] $(basename "${'$'}f") is 600"
    else
      echo "  [warn] $(basename "${'$'}f") is ${'$'}mode, expected 600 (chmod 600 ${'$'}f)"
      warn=1
    fi
  done
  if [ -e "${'$'}sshdir/authorized_keys" ]; then
    mode=$(stat -c '%a' "${'$'}sshdir/authorized_keys" 2>/dev/null)
    case "${'$'}mode" in
      600|644) echo "  [ok] authorized_keys is ${'$'}mode" ;;
      *)
        echo "  [warn] authorized_keys is ${'$'}mode, expected 600 or 644 (chmod 600 ${'$'}sshdir/authorized_keys)"
        warn=1
        ;;
    esac
  fi
else
  echo "  (no ${'$'}sshdir - nothing to check)"
fi

echo
echo "== Listening sockets =="
if command -v ss >/dev/null 2>&1; then
  ss -tln 2>/dev/null | tail -n +2
elif command -v netstat >/dev/null 2>&1; then
  netstat -tln 2>/dev/null | tail -n +3
else
  echo "  (neither ss nor netstat installed - pkg install iproute2 or net-tools)"
fi

echo
echo "== World-writable files under ${'$'}HOME (top 3 levels) =="
found=$(find "${'$'}HOME" -maxdepth 3 -type f -perm -0002 2>/dev/null)
if [ -n "${'$'}found" ]; then
  echo "${'$'}found" | sed 's/^/  [warn] /'
  echo "  (chmod o-w on each of the above)"
  warn=1
else
  echo "  [ok] none found"
fi

echo
if [ "${'$'}warn" -eq 0 ]; then
  echo "No obvious issues found."
else
  echo "Some checks need attention - see [warn] lines above."
fi
"""

    // A quick "what's actually here" report - installed packages, PATH, disk usage. Local only.
    private const val SYSINFO_SCRIPT_BODY = """
set -u
echo "== Environment =="
echo "shell:    ${'$'}{SHELL:-unknown}"
echo "PATH:     ${'$'}PATH"
if command -v uname >/dev/null 2>&1; then
  echo "kernel:   $(uname -a)"
fi
if command -v dpkg >/dev/null 2>&1; then
  echo "packages: $(dpkg -l 2>/dev/null | awk '${'$'}1=="ii"' | wc -l) installed"
else
  echo "packages: (dpkg not on PATH)"
fi

echo
echo "== Disk usage =="
if command -v df >/dev/null 2>&1; then
  df -h "${'$'}HOME" 2>/dev/null
else
  echo "(df not installed)"
fi

echo
echo "== Largest items under ${'$'}HOME =="
if command -v du >/dev/null 2>&1; then
  du -sh "${'$'}HOME"/* 2>/dev/null | sort -rh 2>/dev/null | head -n 10
else
  echo "(du not installed)"
fi
"""
}
