package com.hereliesaz.hg2gui.azp

/**
 * Why an `.azp` download or install did not complete. The store screen shows [AzpFailure.message]
 * as-is; [AzpFailureKind] lets a caller branch (retry on [AzpFailureKind.DOWNLOAD_NETWORK], offer
 * nothing on [AzpFailureKind.SIGNATURE_INVALID]) without parsing text.
 */
enum class AzpFailureKind {
    /** The registry-supplied id/version is not a plain path segment; nothing was fetched/written. */
    INVALID_PACKAGE,

    /** The store answered with a non-success HTTP status (including a refused redirect). */
    DOWNLOAD_HTTP,

    /** The archive is larger than the download cap. */
    DOWNLOAD_TOO_LARGE,

    /** The connection dropped, timed out or could not be made. */
    DOWNLOAD_NETWORK,

    /** The bytes are not a readable ZIP archive. */
    ARCHIVE_UNREADABLE,

    /** Too many entries, or it expands beyond the extraction cap (zip-bomb guard). */
    ARCHIVE_LIMITS,

    /** An entry would be written outside the package directory (zip-slip). */
    ARCHIVE_PATH_TRAVERSAL,

    /** No `manifest.json` at the archive root. */
    MANIFEST_MISSING,

    /** `manifest.json` is not parseable or has fields of the wrong shape. */
    MANIFEST_INVALID,

    /** A payload file's digest differs from `manifest.files`, or files are unlisted/missing. */
    INTEGRITY_MISMATCH,

    /** `signature.json` is corrupt or the Ed25519 signature does not verify. */
    SIGNATURE_INVALID,

    /** Writing the package into app storage failed. */
    STORAGE,
}

/** A typed, user-presentable failure: [kind] to branch on, [message] to show. */
data class AzpFailure(val kind: AzpFailureKind, val message: String)

/** Result of [AzpClient.downloadResult]. */
sealed interface AzpDownloadOutcome {
    class Downloaded(val bytes: ByteArray) : AzpDownloadOutcome
    data class Failed(val failure: AzpFailure) : AzpDownloadOutcome
}

/** Result of [AzpInstaller.installResult] / [AzpInstaller.downloadAndInstall]. */
sealed interface AzpInstallOutcome {
    data class Installed(val result: AzpInstallResult) : AzpInstallOutcome
    data class Failed(val failure: AzpFailure) : AzpInstallOutcome
}

/** The user-facing sentence for a store HTTP status on a download. Pure, see AzpOutcomeTest. */
internal fun describeDownloadHttpFailure(code: Int): String = when (code) {
    401, 403 -> "The store refused the download (HTTP $code)."
    402 -> "This is a paid package; HG2Gui can only install free packages."
    404 -> "The store has no such package version (HTTP 404)."
    in 300..399 -> "The store tried to redirect the download (HTTP $code); redirects are not followed."
    in 500..599 -> "The store had a server error (HTTP $code). Try again later."
    else -> "The store returned HTTP $code."
}

/**
 * The first reason [extracted] (path -> lowercase-hex SHA-256 of the bytes on disk) disagrees with
 * [declared] (`manifest.files`, `sha256-` prefix stripped), or null when they agree. Paths are
 * visited in sorted order so the message is deterministic. Same rule as [filesMatchDigests], which
 * is defined in terms of this.
 */
internal fun describeDigestProblem(extracted: Map<String, String>, declared: Map<String, String>): String? {
    for (path in extracted.keys.sorted()) {
        if (path == "manifest.json" || path == "signature.json") continue
        val want = declared[path] ?: return "\"$path\" is in the archive but not listed in the manifest."
        if (!want.equals(extracted.getValue(path), ignoreCase = true)) {
            return "\"$path\" does not match the digest in the manifest."
        }
    }
    for (path in declared.keys.sorted()) {
        if (path !in extracted) return "\"$path\" is listed in the manifest but missing from the archive."
    }
    return null
}
