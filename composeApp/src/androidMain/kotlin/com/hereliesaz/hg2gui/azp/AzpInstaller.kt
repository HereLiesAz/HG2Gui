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

data class AzpInstallResult(val kind: String, val skillIds: List<String>)

/**
 * Unpacks a downloaded `.azp` - a plain ZIP archive per spec/package-format.md, `manifest.json`
 * at the root - into `filesDir/azp/<id>/<version>/`. HG2Gui has no `.azp` code/WASM runtime, so
 * for every kind except `skill` this is just "download and keep, for use elsewhere" - the same
 * as `apt download` versus `apt install`. A `skill` package's `SKILL.md` files are real usable
 * content here: AzpLibrary reads them back to extend the AI chat's system prompt.
 *
 * Signature verification (package-format.md's Ed25519 model) is NOT implemented in this v1 -
 * packages are trusted at the same level as this app's other locally-stored, unverified settings
 * (e.g. the MCP pairing token). Only free packages are supported; paid packages need a Bearer
 * entitlement this client does not yet obtain.
 */
object AzpInstaller {
    private val json = Json { ignoreUnknownKeys = true }

    fun rootDir(context: Context, id: String, version: String): File =
        File(File(context.filesDir, "azp"), "$id/$version")

    suspend fun install(context: Context, id: String, version: String, bytes: ByteArray): AzpInstallResult? =
        withContext(Dispatchers.IO) {
            val dest = rootDir(context, id, version)
            dest.mkdirs()

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
            if (!manifestFile.exists()) return@withContext null
            val manifest = json.parseToJsonElement(manifestFile.readText()).jsonObject
            val kind = manifest["kind"]?.jsonPrimitive?.content ?: "asset"
            val skillIds = if (kind == "skill") {
                manifest["skill"]?.jsonObject?.get("skills")?.jsonArray
                    ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                    ?: emptyList()
            } else emptyList()

            AzpInstallResult(kind, skillIds)
        }
}
