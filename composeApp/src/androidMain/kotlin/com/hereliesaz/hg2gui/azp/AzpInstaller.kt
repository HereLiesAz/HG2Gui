package com.hereliesaz.hg2gui.azp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class AzpInstallResult(val kind: String, val skillIds: List<String>, val trust: AzpTrust)

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
 */
object AzpInstaller {
    private val json = Json { ignoreUnknownKeys = true }

    fun rootDir(context: Context, id: String, version: String): File =
        File(File(context.filesDir, "azp"), "$id/$version")

    suspend fun install(
        context: Context,
        id: String,
        version: String,
        bytes: ByteArray,
        trustedKeys: List<String> = emptyList(),
    ): AzpInstallResult? =
        withContext(Dispatchers.IO) {
            val dest = rootDir(context, id, version)
            dest.mkdirs()

            // A corrupt archive or a manifest that fails to parse must not crash the caller -
            // any exception past this point is treated as a failed install, same outcome as an
            // explicit rejection, with the partially-extracted directory cleaned up either way.
            try {
                // path (as stored in the archive, "/"-separated) -> lowercase-hex SHA-256 digest
                // of the bytes actually written to disk. Computed inline via DigestOutputStream
                // so the check costs no extra read pass over what was just extracted.
                val extractedDigests = mutableMapOf<String, String>()

                ZipInputStream(bytes.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // Path containment: refuse any entry that would escape dest via .. or an
                        // absolute path (package-format.md's own path-containment requirement).
                        val target = File(dest, name).canonicalFile
                        if (!target.path.startsWith(dest.canonicalPath + File.separator) && target != dest) {
                            zip.closeEntry(); entry = zip.nextEntry; continue
                        }
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            val digest = MessageDigest.getInstance("SHA-256")
                            DigestOutputStream(target.outputStream(), digest).use { out -> zip.copyTo(out) }
                            extractedDigests[name] = digest.digest().joinToString("") { "%02x".format(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                val manifestFile = File(dest, "manifest.json")
                if (!manifestFile.exists()) { dest.deleteRecursively(); return@withContext null }
                val manifestBytes = manifestFile.readBytes()
                val manifest = json.parseToJsonElement(String(manifestBytes)).jsonObject
                val kind = manifest["kind"]?.jsonPrimitive?.content ?: "asset"
                val skillIds = if (kind == "skill") {
                    manifest["skill"]?.jsonObject?.get("skills")?.jsonArray
                        ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                        ?: emptyList()
                } else emptyList()

                // Integrity: every payload entry's digest must match `manifest.files`, and every
                // entry (besides the detached `signature.json`, which is exempt - it's the
                // signature, not a signed payload file, mirroring @azphalt/azp's verifyAzp) must
                // be listed there. A digest mismatch or an unlisted payload file both fail
                // installation the same way a bad signature does: reject and delete, don't
                // install partially-trusted content.
                val declaredFiles = manifest["files"]?.jsonObject
                    ?.mapValues { (_, v) -> v.jsonPrimitive.content.removePrefix("sha256-") }
                    ?: emptyMap()
                if (!filesMatchDigests(extractedDigests, declaredFiles)) {
                    dest.deleteRecursively(); return@withContext null
                }

                val signatureFile = File(dest, "signature.json")
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
                    if (signature == null) AzpTrust.INVALID
                    else AzpSignatureVerifier.verify(manifestBytes, signature, trustedKeys)
                }
                if (trust == AzpTrust.INVALID) { dest.deleteRecursively(); return@withContext null }

                AzpInstallResult(kind, skillIds, trust)
            } catch (e: Exception) {
                dest.deleteRecursively()
                null
            }
        }
}

/**
 * True iff every non-manifest, non-signature entry in [extracted] (path -> lowercase-hex SHA-256
 * of the bytes actually on disk) has a matching digest in [declared] (path -> lowercase-hex
 * SHA-256, `manifest.files` with its `sha256-` prefix already stripped). A path present in
 * [extracted] but absent from [declared] fails ("unlisted payload"), as does any digest mismatch.
 * Pure and Context-free by design, specifically so it's unit-testable without a Robolectric/
 * instrumented Android runtime - see AzpInstallerDigestTest.
 */
internal fun filesMatchDigests(extracted: Map<String, String>, declared: Map<String, String>): Boolean {
    for ((path, actualDigest) in extracted) {
        if (path == "manifest.json" || path == "signature.json") continue
        val wantDigest = declared[path] ?: return false
        if (!wantDigest.equals(actualDigest, ignoreCase = true)) return false
    }
    return true
}
