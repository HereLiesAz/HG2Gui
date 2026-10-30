package com.hereliesaz.hg2gui.update

import android.content.pm.ApplicationInfo

/*
 * Pure decisions behind AppUpdateChecker, kept free of Context/PackageManager/JSON so
 * AppUpdatePolicyTest can pin them as plain JVM unit tests.
 *
 * Channel contract (audit M-9): the in-app updater installs ONLY non-debuggable release builds.
 * It used to poll `latest-debug-v<series>` and install `hg2gui-playstore-debug-*.apk`, and the
 * debug build type was signed with the production key so the signature check passed - which meant
 * every user who updated in-app ran a debuggable, un-minified APK under the production signature
 * (`adb shell run-as` readable app data, JDWP attach). The release pipeline must therefore publish
 * `assemblePlaystoreRelease` output to the `latest-v<major>.<minor>` GitHub release; debug builds
 * are signed with the local debug key (composeApp/build.gradle.kts) and can never pass the
 * signer comparison below, nor the debuggable check.
 */

/** Tag of the rolling GitHub release for the build line [series] (`<major>.<minor>`). */
internal fun updateReleaseTag(series: String): String = "latest-v$series"

/** `hg2gui-<flavor>-<buildType>-<versionName>.apk` as AppUpdateChecker publishes it: release builds only. */
internal val UPDATE_APK_NAME = Regex("""hg2gui-playstore-release-([0-9]+(?:\.[0-9]+)*)\.apk""")

/** One asset of the GitHub release, reduced to what version selection needs. */
internal data class ReleaseAsset(val name: String, val url: String, val digest: String?)

/** The 64-hex SHA-256 inside a GitHub `digest` field (`sha256:<hex>`), or null if absent/other/malformed. */
internal fun sha256FromDigest(raw: String?): String? =
    raw?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.takeIf { it.length == 64 }

/**
 * The highest-versioned update among [assets] that is newer than [installedCode], or null.
 * An asset counts only if its name is a release APK ([UPDATE_APK_NAME]), its version name ends in a
 * numeric build number (which is the versionCode the APK must carry - verified again after the
 * download), and it has a download url.
 */
internal fun newestUpdate(assets: List<ReleaseAsset>, installedCode: Long): AppUpdateChecker.Update? {
    var newest: AppUpdateChecker.Update? = null
    for (asset in assets) {
        val match = UPDATE_APK_NAME.matchEntire(asset.name) ?: continue
        val versionName = match.groupValues[1]
        val build = versionName.substringAfterLast('.').toLongOrNull() ?: continue
        if (asset.url.isBlank()) continue
        val candidate = AppUpdateChecker.Update(versionName, build, asset.url, sha256FromDigest(asset.digest))
        if (newest == null || candidate.versionCode > newest.versionCode) newest = candidate
    }
    return newest?.takeIf { it.versionCode > installedCode }
}

/**
 * Identity checks on a downloaded archive, as an error message (null = fine): same package as the
 * running app, versionCode equal to the one the release advertised, and strictly newer than what
 * is installed (no downgrade or reinstall of the same build).
 */
internal fun archiveIdentityError(
    archivePackage: String,
    expectedPackage: String,
    archiveCode: Long,
    advertisedCode: Long,
    installedCode: Long
): String? = when {
    archivePackage != expectedPackage -> "Update APK belongs to $archivePackage, not $expectedPackage"
    archiveCode != advertisedCode -> "Update APK version code $archiveCode does not match release $advertisedCode"
    archiveCode <= installedCode -> "Downloaded APK is not newer than the installed app"
    else -> null
}

/**
 * Whether an archive signed by [archive] may replace an app signed by [installed]. An unsigned
 * archive (empty set) is always rejected; otherwise the signer sets must be identical, unless the
 * installed set is unknown (empty), in which case the platform installer's own check is left to decide.
 */
internal fun signersAcceptable(installed: Set<String>, archive: Set<String>): Boolean =
    archive.isNotEmpty() && (installed.isEmpty() || archive == installed)

/** True when `ApplicationInfo.flags` carries FLAG_DEBUGGABLE. */
internal fun isDebuggable(applicationFlags: Int): Boolean = applicationFlags and ApplicationInfo.FLAG_DEBUGGABLE != 0
