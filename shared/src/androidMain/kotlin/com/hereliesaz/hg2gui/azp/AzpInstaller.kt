package com.hereliesaz.hg2gui.azp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class AzpInstallResult(
    val kind: String,
    val skillIds: List<String>,
    val trust: AzpTrust,
    /** Present only for `kind:"script"` - see [ScriptDto] and `ScriptInstaller`. */
    val script: ScriptDto? = null,
)

/**
 * Unpacks a downloaded `.azp` - a plain ZIP archive per spec/package-format.md, `manifest.json`
 * at the root - into `filesDir/azp/<id>/<version>/`. HG2Gui has no `.azp` code/WASM runtime, so
 * for every kind except `skill` this is just "download and keep, for use elsewhere" - the same
 * as `apt download` versus `apt install`. A `skill` package's `SKILL.md` files are real usable
 * content here: AzpLibrary reads them back to extend the AI chat's system prompt - which is
 * exactly why per-file integrity matters more here than it might look: a package's Ed25519
 * signature (see [AzpSignatureVerifier]) only covers `manifest.json`'s bytes, never the payload.
 * A signature check alone confirms the *manifest* wasn't altered after signing; it says nothing
 * about whether the `SKILL.md` bytes sitting next to it are the ones the signer actually shipped.
 * Without a per-file digest check, a tampered `SKILL.md` - content that lands verbatim in this
 * app's own AI system prompt - would verify as TRUSTED right alongside a swapped-in prompt
 * injection. So every extracted payload file's SHA-256 is checked against `manifest.files[path]`
 * (package-format.md's own integrity contract - the same check `@azphalt/azp`'s `verifyAzp` runs),
 * and any file present in the archive but absent from `manifest.files` is rejected outright,
 * mirroring `verifyAzp`'s "unlisted payload" check (a ZIP-confusion / signature-bypass vector:
 * an entry nothing hashed is an entry nothing can vouch for).
 *
 * Signature verification runs when the package carries a `signature.json`; a package whose
 * signature fails to verify, whose `signature.json` is too corrupt to even parse, or whose
 * payload fails the digest check above, is rejected outright (the extracted files are deleted and
 * `install()` returns null) - package-format.md's own mandate: "verify the signature... and
 * reject on mismatch". An unsigned package still installs, same trust posture as this app's other
 * locally-stored, unverified settings (e.g. the MCP pairing token) - only its [AzpTrust] is
 * [AzpTrust.UNSIGNED] instead of [AzpTrust.VALID]/[AzpTrust.TRUSTED]; the digest check still
 * applies regardless of signing, since `manifest.files` is a plain integrity contract independent
 * of trust. Only free packages are supported; paid packages need a Bearer entitlement this client
 * does not yet obtain.
 *
 * [install] keeps its nullable contract (null = did not install) for existing callers, but null
 * says nothing about *why*. [installResult] and [downloadAndInstall] are the same pipeline with a
 * typed [AzpInstallOutcome]: every rejection carries an [AzpFailure] naming the cause, so the
 * store can tell the user "digest mismatch" or "signature invalid" instead of a silent INSTALL.
 */
object AzpInstaller {
    private val json = Json { ignoreUnknownKeys = true }

    // Caps on what one archive may expand to - a zip bomb (or just a hostile 10^6-entry archive)
    // must fail the install rather than fill the device's storage.
    private const val MAX_EXTRACTED_BYTES = 512L * 1024 * 1024
    private const val MAX_ENTRIES = 10_000

    /** `filesDir/azp/<id>/<version>/`. [id] and [version] come from the registry (untrusted), so
     *  each must be a plain path segment (see [isValidAzpPathSegment]) and the result must still
     *  canonicalize inside `azp/` - otherwise this throws [IllegalArgumentException], never
     *  returning a directory a later `deleteRecursively()`/extract could escape through. */
    fun rootDir(context: Context, id: String, version: String): File =
        rootDirOrNull(context, id, version)
            ?: throw IllegalArgumentException("invalid package id/version: $id/$version")

    /** [rootDir], but null instead of throwing for an invalid [id]/[version]. */
    fun rootDirOrNull(context: Context, id: String, version: String): File? {
        if (!isValidAzpPathSegment(id) || !isValidAzpPathSegment(version)) return null
        val azpRoot = File(context.filesDir, "azp").canonicalFile
        val dir = File(azpRoot, "$id/$version").canonicalFile
        return if (dir.path.startsWith(azpRoot.path + File.separator)) dir else null
    }

    /** [installResult] flattened to the legacy contract: the result, or null on any failure. */
    suspend fun install(
        context: Context,
        id: String,
        version: String,
        bytes: ByteArray,
        trustedKeys: List<String> = emptyList(),
    ): AzpInstallResult? =
        (installResult(context, id, version, bytes, trustedKeys) as? AzpInstallOutcome.Installed)?.result

    /** Downloads (see [AzpClient.downloadResult]) and installs in one step, with the first
     *  failure from either stage as a typed [AzpInstallOutcome.Failed]. */
    suspend fun downloadAndInstall(
        context: Context,
        id: String,
        version: String,
        trustedKeys: List<String> = emptyList(),
    ): AzpInstallOutcome = when (val download = AzpClient.downloadResult(id, version)) {
        is AzpDownloadOutcome.Failed -> AzpInstallOutcome.Failed(download.failure)
        is AzpDownloadOutcome.Downloaded -> installResult(context, id, version, download.bytes, trustedKeys)
    }

    private class ParsedManifest(
        val kind: String,
        val skillIds: List<String>,
        val script: ScriptDto?,
        val declaredFiles: Map<String, String>,
    )

    /** A rejection raised inside the install pipeline; caught by [installResult] and reported. */
    private class AzpInstallException(val kind: AzpFailureKind, message: String) : Exception(message)

    /**
     * The install pipeline. Never throws (except cancellation): a rejection - bad id, corrupt or
     * hostile archive, digest or signature failure - is an [AzpInstallOutcome.Failed] whose
     * [AzpFailure.message] says which check refused it. The previous working copy is untouched on
     * every failure.
     */
    suspend fun installResult(
        context: Context,
        id: String,
        version: String,
        bytes: ByteArray,
        trustedKeys: List<String> = emptyList(),
    ): AzpInstallOutcome =
        withContext(Dispatchers.IO) {
            val dest = rootDirOrNull(context, id, version)
                ?: return@withContext AzpInstallOutcome.Failed(
                    AzpFailure(AzpFailureKind.INVALID_PACKAGE, "Invalid package id or version: $id / $version")
                )
            // AZP-9: extracted and validated into a staging directory first, never straight into
            // `dest`. A reinstall of an already-installed id/version (e.g. retrying after a
            // network blip) used to extract directly on top of the working copy; a corrupt
            // download or a failed digest/signature check then deleted `dest` in the catch block
            // below, wiping the previously-good install even though AzpLibrary still records it
            // as installed. `dest` is only ever touched, atomically, once every check below has
            // already passed.
            val staging = File(dest.parentFile, "${dest.name}.staging-${System.identityHashCode(bytes)}")
            staging.deleteRecursively()
            staging.mkdirs()

            // A corrupt archive or a manifest that fails to parse must not crash the caller -
            // any exception past this point is treated as a failed install, same outcome as an
            // explicit rejection, with the partially-extracted staging directory cleaned up
            // either way - `dest` (the previous working copy, if any) is never touched on failure.
            try {
                // path (as stored in the archive, "/"-separated) -> lowercase-hex SHA-256 digest
                // of the bytes actually written to disk. Computed inline via DigestOutputStream
                // so the check costs no extra read pass over what was just extracted.
                val extractedDigests = mutableMapOf<String, String>()

                var totalBytes = 0L
                var entryCount = 0
                ZipInputStream(bytes.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (++entryCount > MAX_ENTRIES) {
                            throw AzpInstallException(AzpFailureKind.ARCHIVE_LIMITS, "The archive has too many entries.")
                        }
                        // Path containment: refuse any entry that would escape staging via .. or
                        // an absolute path (package-format.md's own path-containment requirement).
                        // AZP-8: a package that has to smuggle a zip-slip entry to work at all is
                        // not a package worth installing minus that one entry - silently skipping
                        // it used to let the rest of an otherwise-hostile archive install with no
                        // signal at all, rather than the same hard rejection a bad digest gets.
                        val target = File(staging, name).canonicalFile
                        if (!target.path.startsWith(staging.canonicalPath + File.separator) && target != staging) {
                            throw AzpInstallException(
                                AzpFailureKind.ARCHIVE_PATH_TRAVERSAL,
                                "The archive entry \"$name\" would be written outside the package folder."
                            )
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            val digest = MessageDigest.getInstance("SHA-256")
                            DigestOutputStream(target.outputStream(), digest).use { out ->
                                // Bounded copy: entry sizes in the zip header are attacker-chosen,
                                // so count the bytes actually inflated instead.
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val n = zip.read(buffer)
                                    if (n < 0) break
                                    totalBytes += n
                                    if (totalBytes > MAX_EXTRACTED_BYTES) {
                                        throw AzpInstallException(
                                            AzpFailureKind.ARCHIVE_LIMITS,
                                            "The archive expands beyond ${MAX_EXTRACTED_BYTES / (1024 * 1024)} MB."
                                        )
                                    }
                                    out.write(buffer, 0, n)
                                }
                            }
                            extractedDigests[name] = digest.digest().joinToString("") { "%02x".format(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                val manifestFile = File(staging, "manifest.json")
                if (!manifestFile.exists()) {
                    throw AzpInstallException(AzpFailureKind.MANIFEST_MISSING, "The package has no manifest.json.")
                }
                val manifestBytes = manifestFile.readBytes()
                val parsed = try {
                    val manifest = json.parseToJsonElement(String(manifestBytes)).jsonObject
                    val manifestKind = manifest["kind"]?.jsonPrimitive?.content ?: "asset"
                    ParsedManifest(
                        kind = manifestKind,
                        skillIds = if (manifestKind == "skill") {
                            manifest["skill"]?.jsonObject?.get("skills")?.jsonArray
                                ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                                ?: emptyList()
                        } else emptyList(),
                        script = if (manifestKind == "script") {
                            manifest["script"]?.jsonObject?.let {
                                try { json.decodeFromJsonElement(ScriptDto.serializer(), it) } catch (e: Exception) { null }
                            }
                        } else null,
                        declaredFiles = manifest["files"]?.jsonObject
                            ?.mapValues { (_, v) -> v.jsonPrimitive.content.removePrefix("sha256-") }
                            ?: emptyMap()
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    throw AzpInstallException(
                        AzpFailureKind.MANIFEST_INVALID,
                        "manifest.json is not a valid package manifest (${e.message ?: e.javaClass.simpleName})."
                    )
                }

                // Integrity: every payload entry's digest must match `manifest.files`, and every
                // entry (besides the detached `signature.json`, which is exempt - it's the
                // signature, not a signed payload file, mirroring @azphalt/azp's verifyAzp) must
                // be listed there. A digest mismatch or an unlisted payload file both fail
                // installation the same way a bad signature does: reject and delete, don't
                // install partially-trusted content.
                describeDigestProblem(extractedDigests, parsed.declaredFiles)?.let {
                    throw AzpInstallException(AzpFailureKind.INTEGRITY_MISMATCH, "Integrity check failed: $it")
                }

                val signatureFile = File(staging, "signature.json")
                val trust = if (!signatureFile.exists()) {
                    AzpTrust.UNSIGNED
                } else {
                    val signature = try {
                        json.decodeFromString(AzpSignature.serializer(), signatureFile.readText())
                    } catch (e: Exception) {
                        null
                    }
                    // A signature.json that exists but doesn't even parse isn't "no signature" -
                    // it's a corrupt or tampered one, and gets the same outright rejection as a
                    // signature that fails cryptographic verification, not the benign UNSIGNED
                    // tier AzpSignatureVerifier.verify() would otherwise give a null signature.
                    if (signature == null) {
                        throw AzpInstallException(
                            AzpFailureKind.SIGNATURE_INVALID, "signature.json is corrupt and cannot be read."
                        )
                    } else {
                        AzpSignatureVerifier.verify(manifestBytes, signature, trustedKeys)
                    }
                }
                if (trust == AzpTrust.INVALID) {
                    throw AzpInstallException(
                        AzpFailureKind.SIGNATURE_INVALID, "The package signature does not verify; it was altered or is corrupt."
                    )
                }

                // Every check passed - only now does the previous working copy (if any) get
                // replaced, and only by first deleting `dest` and renaming staging into place, so
                // there's never a window where `dest` is a half-written mix of old and new files.
                dest.deleteRecursively()
                if (!staging.renameTo(dest)) {
                    // Cross-filesystem renames (or an unlucky race) can make renameTo fail even
                    // though both dirs are otherwise fine - fall back to a copy, then clean up
                    // staging either way so it never lingers as an orphaned "<id>/<version>.staging-*".
                    staging.copyRecursively(dest, overwrite = true)
                    staging.deleteRecursively()
                }

                AzpInstallOutcome.Installed(AzpInstallResult(parsed.kind, parsed.skillIds, trust, parsed.script))
            } catch (e: CancellationException) {
                staging.deleteRecursively()
                throw e
            } catch (e: AzpInstallException) {
                staging.deleteRecursively()
                AzpInstallOutcome.Failed(AzpFailure(e.kind, e.message.orEmpty()))
            } catch (e: Exception) {
                staging.deleteRecursively()
                AzpInstallOutcome.Failed(classifyInstallError(e))
            }
        }
}

/** Maps an unexpected exception from the install pipeline to a typed failure. */
internal fun classifyInstallError(e: Exception): AzpFailure {
    val detail = e.message ?: e.javaClass.simpleName
    return when (e) {
        is java.util.zip.ZipException -> AzpFailure(AzpFailureKind.ARCHIVE_UNREADABLE, "The package is not a readable ZIP archive ($detail).")
        is java.io.IOException -> AzpFailure(AzpFailureKind.STORAGE, "Could not write the package to app storage ($detail).")
        is IllegalArgumentException -> AzpFailure(AzpFailureKind.MANIFEST_INVALID, "The package manifest is malformed ($detail).")
        else -> AzpFailure(AzpFailureKind.STORAGE, "Installation failed ($detail).")
    }
}

/**
 * True iff every non-manifest, non-signature entry in [extracted] (path -> lowercase-hex SHA-256
 * of the bytes actually on disk) has a matching digest in [declared] (path -> lowercase-hex
 * SHA-256, `manifest.files` with its `sha256-` prefix already stripped), AND every path in
 * [declared] actually shows up in [extracted]. A path present in [extracted] but absent from
 * [declared] fails ("unlisted payload"), as does any digest mismatch - and, AZP-5, so does a
 * declared path that's simply missing from the archive: the manifest vouching for a file that
 * was quietly deleted before signing is exactly as much a tamper as swapping one's bytes, but a
 * check that only ever walked what's present would never have noticed it. Pure and Context-free
 * by design, specifically so it's unit-testable without a Robolectric/instrumented Android
 * runtime - see AzpInstallerDigestTest.
 */
internal fun filesMatchDigests(extracted: Map<String, String>, declared: Map<String, String>): Boolean =
    describeDigestProblem(extracted, declared) == null

private val AZP_PATH_SEGMENT = Regex("^[A-Za-z0-9._-]+$")

/** True iff [segment] (a registry-supplied package id or version) is safe as a single directory
 *  name: only letters, digits, `.`, `_`, `-`, and not `.` or `..`. Pure, see AzpInstallerDigestTest. */
internal fun isValidAzpPathSegment(segment: String): Boolean =
    segment != "." && segment != ".." && AZP_PATH_SEGMENT.matches(segment)
