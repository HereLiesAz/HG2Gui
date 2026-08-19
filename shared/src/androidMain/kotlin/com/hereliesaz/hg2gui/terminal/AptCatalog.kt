package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.util.Utils
import java.io.File
import java.io.IOException

/**
 * The full set of packages `apt install` could actually install - not just what's already on
 * disk (that's [DpkgCatalog]), but everything `apt update` downloaded an index for. Parsed from
 * `var/lib/apt/lists`, every file whose name ends in `_Packages`: Debian control-file stanzas
 * (blank-line-separated, one per package), the same format `apt`/`dpkg` themselves read.
 *
 * Termux's real repo carries no Debian "Section" or "Tag" field on any of its ~2900 packages
 * (verified against a live index - the same gap [DpkgCatalog]'s own doc comment found for the
 * ~80 base packages already on disk), so there is no live source of truth to sort these into
 * categories by. [categoryOf] is a name/description heuristic instead - Termux's own naming
 * conventions (`python-*`, `lib*`, `*-dev`, ...) cover a lot of ground reliably; anything it
 * can't place lands in "Other" rather than being left out.
 */
object AptCatalog {

    data class Entry(val name: String, val description: String)

    /** True once `apt update` has actually run - before that there's no index to read at all. */
    fun hasIndex(prefix: File): Boolean = listPackagesFiles(prefix).isNotEmpty()

    fun all(prefix: File): List<Entry> {
        val result = LinkedHashMap<String, Entry>()
        for (file in listPackagesFiles(prefix)) {
            try {
                parseInto(file, result)
            } catch (e: IOException) {
                // This index file is unreadable - skip it, keep whatever the others contributed.
                Utils.log(e)
            }
        }
        return result.values.toList()
    }

    fun categoryOf(name: String, description: String): String {
        val byName = NAME_PATTERNS.firstOrNull { (pattern, _) -> pattern.matches(name) }?.second
        if (byName != null) return byName
        return DESCRIPTION_KEYWORDS.firstOrNull { (pattern, _) -> pattern.containsMatchIn(description) }?.second
            ?: UNCATEGORIZED
    }

    private fun listPackagesFiles(prefix: File): List<File> {
        val listsDir = File(prefix, "var/lib/apt/lists")
        return listsDir.listFiles { f -> f.isFile && f.name.endsWith("_Packages") }.orEmpty().toList()
    }

    // A control-file stanza is one package: consecutive "Field: value" lines, terminated by a
    // blank line (or end of file). Every field but Package/Description is irrelevant here -
    // Version, Depends, SHA256, etc. are apt's own concern, not this catalog's.
    private fun parseInto(file: File, into: MutableMap<String, Entry>) {
        var name: String? = null
        var description = ""
        fun flush() {
            val n = name
            if (n != null) into[n] = Entry(n, description)
            name = null
            description = ""
        }
        file.forEachLine { line ->
            when {
                line.startsWith("Package: ") -> name = line.removePrefix("Package: ").trim()
                line.startsWith("Description: ") -> description = line.removePrefix("Description: ").trim()
                line.isBlank() -> flush()
            }
        }
        flush()
    }

    private const val UNCATEGORIZED = "Other"

    // Checked in order, first match wins - specific language/tool families before the broad
    // lib*/*-dev/*-doc catch-alls, so e.g. "libpython3-dev" still lands under Python, not
    // Libraries. Matched against the whole package name ([Regex.matches]), not a substring.
    private val NAME_PATTERNS: List<Pair<Regex, String>> = listOf(
        Regex("python3?(-.*)?") to "Python",
        Regex("perl(-.*)?") to "Perl",
        Regex("ruby(-.*)?") to "Ruby",
        Regex("golang(-.*)?") to "Go",
        Regex("rust(c|up)?(-.*)?") to "Rust",
        Regex("nodejs(-.*)?|npm|yarn") to "Node.js",
        Regex("php\\d*(-.*)?") to "PHP",
        Regex("openjdk-.*|gradle|maven|kotlin") to "Java/JVM",
        Regex("ocaml(-.*)?") to "OCaml",
        Regex("(tex|texlive)(-.*)?") to "TeX/LaTeX",
        Regex("font-.*") to "Fonts",
        Regex("(vim|neovim|nano|emacs|micro)(-.*)?") to "Editors",
        Regex("(zsh|bash|dash|fish|tcsh|ksh|mksh)(-.*)?") to "Shells",
        Regex(
            "nmap|curl|libcurl.*|wget|openssh(-.*)?|rsync|socat|mtr|whois|iproute2|" +
                "net-tools|iputils|tcpdump|wireshark.*|proxychains.*|dnsutils|mosh"
        ) to "Networking",
        Regex("gnupg|gpgv?|hydra|john|metasploit.*|aircrack-ng|nikto|sqlmap|hashcat|nmap-.*") to "Security",
        Regex("sqlite(-.*)?|postgresql(-.*)?|mariadb(-.*)?|mysql.*|redis|mongodb.*") to "Databases",
        Regex("ffmpeg|sox|imagemagick|mpv|vlc|sdl2?(-.*)?") to "Multimedia",
        Regex("git(-.*)?|subversion|mercurial|cvs") to "Version control",
        Regex("clang(-.*)?|gcc(-.*)?|cmake|make|ninja|autoconf|automake|pkg-config|gdb|valgrind|binutils") to "Build tools",
        Regex(".*-dev") to "Development headers",
        Regex(".*-doc") to "Documentation",
        Regex(".*-dbg(sym)?") to "Debug symbols",
        Regex("lib.*") to "Libraries"
    )

    private val DESCRIPTION_KEYWORDS: List<Pair<Regex, String>> = listOf(
        Regex("(?i)\\bgame\\b") to "Games",
        Regex("(?i)\\beditor\\b") to "Editors",
        Regex("(?i)\\bshell\\b") to "Shells",
        Regex("(?i)\\b(network|http|ftp|dns|proxy|vpn)\\b") to "Networking",
        Regex("(?i)\\b(encrypt|crypto|security|firewall|vulnerab)") to "Security",
        Regex("(?i)\\b(database|\\bsql\\b)") to "Databases",
        Regex("(?i)\\b(audio|video|image|photo|music)\\b") to "Multimedia",
        Regex("(?i)\\bfonts?\\b") to "Fonts",
        Regex("(?i)\\b(compiler|programming language|interpreter)\\b") to "Development"
    )
}
