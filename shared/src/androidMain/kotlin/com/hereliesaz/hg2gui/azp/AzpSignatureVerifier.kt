package com.hereliesaz.hg2gui.azp

import android.os.Build
import android.util.Base64
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Ed25519 verification of a package's `manifest.json` against its `signature.json`, per
 * spec/package-format.md § Signing: a detached signature over the exact `manifest.json` bytes as
 * stored in the archive, verbatim - no re-canonicalization. Cross-checked against a real signed
 * package downloaded live from `azphalt.store` (`com.hereliesaz.azphalt.3d-protrusion` 1.0.0):
 * `openssl pkeyutl -verify -pubin -inkey pub.der -keyform DER -rawin -in manifest.json -sigfile
 * sig.bin` → `Signature Verified Successfully`, confirming the standard (not URL-safe) base64,
 * the 44-byte SPKI DER public key shape, and the exact-manifest-bytes assumption this file makes.
 *
 * Uses the platform's own Ed25519 support (`java.security`, API 33+) rather than adding a new
 * crypto dependency - this app's existing anthropic-java dependency already showed how much
 * packaging risk a new dependency with transitive deps can carry (see build.gradle.kts's
 * packaging excludes and the R8 dontwarn rules it needed). A device below API 33 reports
 * [AzpTrust.UNVERIFIABLE] rather than silently skipping the check unnoticed.
 *
 * Trust is a separate question from integrity: a valid signature only proves the manifest wasn't
 * tampered with after signing (spec: "tamper-evidence, not identity"). Only a signer key the
 * registry itself publishes in `signingKeys` earns [AzpTrust.TRUSTED] - counter-signature chains
 * (spec/package-format.md's web-of-trust extension) aren't implemented, so a package
 * counter-signed but not directly listed reports [AzpTrust.VALID], not [AzpTrust.TRUSTED].
 */
object AzpSignatureVerifier {
    fun verify(manifestBytes: ByteArray, signature: AzpSignature?, trustedKeys: List<String>): AzpTrust {
        if (signature == null || signature.publicKey.isBlank() || signature.signature.isBlank()) {
            return AzpTrust.UNSIGNED
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return AzpTrust.UNVERIFIABLE
        }
        return try {
            val publicKeyBytes = Base64.decode(signature.publicKey, Base64.DEFAULT)
            val signatureBytes = Base64.decode(signature.signature, Base64.DEFAULT)
            val publicKey = KeyFactory.getInstance("Ed25519")
                .generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val verifier = Signature.getInstance("Ed25519")
            verifier.initVerify(publicKey)
            verifier.update(manifestBytes)
            when {
                !verifier.verify(signatureBytes) -> AzpTrust.INVALID
                signature.publicKey in trustedKeys -> AzpTrust.TRUSTED
                else -> AzpTrust.VALID
            }
        } catch (e: Exception) {
            AzpTrust.INVALID
        }
    }
}
