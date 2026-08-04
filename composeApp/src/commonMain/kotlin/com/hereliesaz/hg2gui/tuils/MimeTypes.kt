package com.hereliesaz.hg2gui.tuils

import kotlin.math.roundToInt

expect fun getSystemMimeType(extension: String): String?

object MimeTypes {
    const val ALL_MIME_TYPES = "*/*"

    private val MIME_TYPES = mapOf(
        "asm" to "text/x-asm",
        "json" to "application/json",
        "js" to "application/javascript",
        "def" to "text/plain",
        "in" to "text/plain",
        "rc" to "text/plain",
        "list" to "text/plain",
        "log" to "text/plain",
        "pl" to "text/plain",
        "prop" to "text/plain",
        "properties" to "text/plain",
        "ini" to "text/plain",
        "md" to "text/markdown",
        "epub" to "application/epub+zip",
        "ibooks" to "application/x-ibooks+zip",
        "ifb" to "text/calendar",
        "eml" to "message/rfc822",
        "msg" to "application/vnd.ms-outlook",
        "ace" to "application/x-ace-compressed",
        "bz" to "application/x-bzip",
        "bz2" to "application/x-bzip2",
        "cab" to "application/vnd.ms-cab-compressed",
        "gz" to "application/x-gzip",
        "lrf" to "application/octet-stream",
        "jar" to "application/java-archive",
        "xz" to "application/x-xz",
        "Z" to "application/x-compress",
        "bat" to "application/x-msdownload",
        "ksh" to "text/plain",
        "sh" to "application/x-sh",
        "db" to "application/octet-stream",
        "db3" to "application/octet-stream",
        "otf" to "application/x-font-otf",
        "ttf" to "application/x-font-ttf",
        "psf" to "application/x-font-linux-psf",
        "cgm" to "image/cgm",
        "btif" to "image/prs.btif",
        "dwg" to "image/vnd.dwg",
        "dxf" to "image/vnd.dxf",
        "fbs" to "image/vnd.fastbidsheet",
        "fpx" to "image/vnd.fpx",
        "fst" to "image/vnd.fst",
        "mdi" to "image/vnd.ms-mdi",
        "npx" to "image/vnd.net-fpx",
        "xif" to "image/vnd.xiff",
        "pct" to "image/x-pict",
        "pic" to "image/x-pict",
        "adp" to "audio/adpcm",
        "au" to "audio/basic",
        "snd" to "audio/basic",
        "m2a" to "audio/mpeg",
        "m3a" to "audio/mpeg",
        "oga" to "audio/ogg",
        "spx" to "audio/ogg",
        "aac" to "audio/x-aac",
        "mka" to "audio/x-matroska",
        "jpgv" to "video/jpeg",
        "jpgm" to "video/jpm",
        "jpm" to "video/jpm",
        "mj2" to "video/mj2",
        "mjp2" to "video/mj2",
        "mpa" to "video/mpeg",
        "ogv" to "video/ogg",
        "flv" to "video/x-flv",
        "mkv" to "video/x-matroska"
    )

    fun getMimeType(path: String, isDirectory: Boolean): String? {
        if (isDirectory) return null

        val extension = getExtension(path)
        if (extension.isEmpty()) return ALL_MIME_TYPES

        val extensionLowerCase = extension.lowercase()
        return getSystemMimeType(extensionLowerCase) ?: MIME_TYPES[extensionLowerCase] ?: ALL_MIME_TYPES
    }

    fun mimeTypeMatch(mime: String, input: String): Boolean {
        val regex = mime.replace("*", ".*").toRegex()
        return regex.matches(input)
    }

    fun getExtension(path: String): String {
        return if (path.contains(".")) path.substringAfterLast(".").lowercase() else ""
    }
}
