package com.hereliesaz.hg2gui.azp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.zip.ZipInputStream

data class AzpInstallResult(val kind: String, val skillIds: List<String>, val trust: AzpTrust)

/**
 * Unpacks a downloaded `.azp` - a plain ZIP archive per spec/package-format.md, `manifest.json`
 * at the root - into `filesDir/azp/<id>/<version>/`. HG2Gui has no `.azp` code/WASM runtime, so
 * for every kind except `skill` this is just "download and keep, for use elsewhere" - the same
 * as `apt download` versus `apt install`. A `skill` package's `SKILL.md` files are real usable
 * content here: AzpLibrary reads them back to extend the AI chat's system prompt.
 *
 * Signature verification (see [AzpSignatureVerifier]) runs when the package carries a
 * `signature.json`; a package whose signature fails to verify is rejected outright (the
 * extracted files are deleted and `install()` returns null) - package-format.md's own mandate:
 * "verify the signature... and reject on mismatch". An unsigned package still installs, same
 * trust posture as this app's other locally-stored, unverified settings (e.g. the MCP pairing
 * token) - only its [AzpTrust] is [AzpTrust.UNSIGNED] instead of [AzpTrust.VALID]/[AzpTrust.TRUSTED].
 * Per-file integrity against `manifest.files`' digests is not implemented - only the manifest
 * signature itself. Only free packages are supported; paid packages need a Bearer entitlement
 * this client does not yet obtain.
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
                            target.outputStream().use { out -> zip.copyTo(out) }
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
