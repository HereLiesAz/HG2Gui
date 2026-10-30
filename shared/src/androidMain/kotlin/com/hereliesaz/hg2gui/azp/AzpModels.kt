package com.hereliesaz.hg2gui.azp

import kotlinx.serialization.Serializable

/**
 * Wire shapes for the azphalt Repository API (spec/repository-api.md), trimmed to the fields
 * HG2Gui actually reads. Extra server fields are ignored by kotlinx.serialization's default
 * lenient decoding.
 */
@Serializable
data class AzpPackageSummary(
    val id: String,
    val name: String,
    val description: String = "",
    val author: String = "",
    val version: String = "",
    val kind: String = "asset",
    val priceStatus: String = "free",
    val downloads: Long = 0,
    val byteSize: Long = 0,
)

@Serializable
data class AzpSearchResponse(
    val packages: List<AzpPackageSummary> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
)

/** A locally installed package, as tracked by [com.hereliesaz.hg2gui.managers.AzpLibrary]. */
data class AzpInstalled(
    val id: String,
    val name: String,
    val version: String,
    val kind: String,
    val trust: AzpTrust = AzpTrust.UNSIGNED,
)

/** The detached signature stored as a package's `signature.json` (spec/package-format.md §
 *  Signing). `publicKey` is base64 SPKI DER; `signature` is base64 raw Ed25519 over the exact
 *  `manifest.json` bytes, verbatim. */
@Serializable
data class AzpSignature(
    val alg: String = "",
    val publicKey: String = "",
    val keyId: String? = null,
    val signature: String = "",
)

@Serializable
data class AzpSigningKeyInfo(val publicKey: String, val keyId: String? = null, val label: String? = null)

/** `GET /.well-known/azphalt-repository.json` - `signingKeys` is the registry's own trust
 *  anchor: a package whose signer key appears here is [AzpTrust.TRUSTED], not just internally
 *  consistent. May be empty if the registry hasn't published any (packages can still be signed
 *  and internally verify - see [AzpTrust.VALID]). */
@Serializable
data class AzpDiscovery(
    val name: String = "",
    val version: String = "",
    val signingKeys: List<AzpSigningKeyInfo> = emptyList(),
)

/**
 * The `script` block of a `kind:"script"` package (`spec/script.md`) - a native script installed
 * and run the way a package manager installs its own package. `dependencies` maps a
 * package-manager namespace to the packages it needs; only `"apt"` means anything to this
 * Termux-backed app (any other namespace is present-but-unusable here, same as an unrecognized
 * `mcp` `grants` entry), resolved by [ScriptInstaller] before the script is made runnable.
 */
@Serializable
data class ScriptDto(
    val interpreter: String = "",
    val entry: String = "",
    val command: String? = null,
    val args: List<String> = emptyList(),
    val dependencies: Map<String, List<String>> = emptyMap(),
)

/** The outcome of verifying a package's `signature.json` against its `manifest.json` bytes -
 *  see [com.hereliesaz.hg2gui.azp.AzpSignatureVerifier]. */
enum class AzpTrust {
    /** No trust was ever recorded for this install - it predates signature verification, so
     *  nothing is actually known here (not the same claim as [UNSIGNED]). Re-installing checks
     *  it for real. */
    UNCHECKED,
    /** No `signature.json` in the package. */
    UNSIGNED,
    /** `signature.json` present but the signature does not verify (or the file itself is corrupt
     *  and doesn't even parse) - tampered or corrupt either way. */
    INVALID,
    /** Signature verifies, but the signer key isn't one the registry publishes - "tamper-evidence,
     *  not identity" per spec: internally consistent, provenance not established. */
    VALID,
    /** Signature verifies and the signer key is directly in the registry's published set. */
    TRUSTED,
    /** This Android version has no Ed25519 support (`java.security`, API 33+) to check with. */
    UNVERIFIABLE;

    /**
     * Whether text from a package at this trust level may be folded into the AI system prompt
     * (`AzpLibrary.installedSkillTexts`). Only [TRUSTED] qualifies: a skill's `SKILL.md` becomes
     * instructions to the model, so a package nobody vouched for (unsigned, merely self-signed
     * [VALID] - anyone can self-sign - unverifiable, a legacy [UNCHECKED] record, or a failed
     * [INVALID] one) must not be able to steer command suggestions. Such a skill stays installed
     * and on disk; it just is not read into prompts until a reinstall earns [TRUSTED].
     */
    val reachesAiPrompt: Boolean get() = this == TRUSTED

    companion object {
        /** The trust stored as [name] by `AzpLibrary.record`; null or an unknown name is
         *  [UNCHECKED] (a record that predates verification), never a positive claim. */
        fun fromStored(name: String?): AzpTrust =
            name?.let { stored -> entries.firstOrNull { it.name == stored } } ?: UNCHECKED
    }
}
