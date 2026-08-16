import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

val autoIncrementVersion = tasks.register("autoIncrementVersion") {
    doLast {
        val versionFile = rootProject.file("version.properties")
        if (versionFile.exists()) {
            val props = Properties()
            versionFile.inputStream().use { props.load(it) }

            val patch = (props.getProperty("versionPatch")?.toInt() ?: 0) + 1
            val build = (props.getProperty("versionBuild")?.toInt() ?: 0) + 1

            val content = versionFile.readText()
            val updatedContent = content
                .replace(Regex("versionPatch\\s*=\\s*\\d+"), "versionPatch=$patch")
                .replace(Regex("versionBuild\\s*=\\s*\\d+"), "versionBuild=$build")

            versionFile.writeText(updatedContent)
            println("Auto-incremented version.properties: patch=$patch, build=$build")
        }
    }
}

/**
 * Prints the anticipated version information (current + 1) to GITHUB_ENV if available,
 * or simply to stdout. This ensures CI workflows use the same version numbers as the build.
 */
tasks.register("printVersionEnv") {
    doLast {
        val versionFile = rootProject.file("version.properties")
        if (versionFile.exists()) {
            val props = Properties()
            versionFile.inputStream().use { props.load(it) }

            val major = props.getProperty("versionMajor", "0")
            val minor = props.getProperty("versionMinor", "0")
            val patch = (props.getProperty("versionPatch")?.toInt() ?: 0) + 1
            val build = (props.getProperty("versionBuild")?.toInt() ?: 0) + 1
            val versionName = "$major.$minor.$patch.$build"
            // Mirrors composeApp/build.gradle.kts's own `resolvedVersionCode` exactly (same
            // `legacyVersionCode = 205` floor, same +1) - this is a separate Gradle script with
            // no shared code between the two, so nothing enforces they stay in lockstep short of
            // this comment. release-play.yml's "Confirm the built versionCode clears Play" step
            // (and the Play API upload's expected-version-code) both read this VERSION_CODE - a
            // build actually invokes `bundleRelease` with no `-PversionBuild` override, so it too
            // resolves versionCode from version.properties, not this script's own $build.
            val legacyVersionCode = 205
            val versionCode = maxOf(build, legacyVersionCode + 1)

            val githubEnv = System.getenv("GITHUB_ENV")
            if (githubEnv != null) {
                File(githubEnv).appendText(
                    """
                    VERSION_MAJOR=$major
                    VERSION_MINOR=$minor
                    VERSION_PATCH=$patch
                    VERSION_BUILD=$build
                    VERSION_NAME=$versionName
                    VERSION_CODE=$versionCode
                    """.trimIndent() + "\n"
                )
            }
            println("VERSION_MAJOR=$major")
            println("VERSION_MINOR=$minor")
            println("VERSION_PATCH=$patch")
            println("VERSION_BUILD=$build")
            println("VERSION_NAME=$versionName")
            println("VERSION_CODE=$versionCode")
        }
    }
}

tasks.register("incrementAndPushVersion") {
    doLast {
        val versionFile = rootProject.file("version.properties")
        // Auto-detect branch from Environment if not provided via -PtargetBranch
        val branch = project.findProperty("targetBranch")?.toString()
            ?: System.getenv("GITHUB_REF_NAME")
            ?: "master"
        val customMessage = project.findProperty("commitMessage")?.toString()
        
        fun git(vararg args: String) {
            val process = ProcessBuilder(listOf("git") + args)
                .directory(rootProject.projectDir)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().forEachLine { println(it) }
            val exit = process.waitFor()
            if (exit != 0) throw GradleException("git ${args.joinToString(" ")} failed (exit $exit)")
        }

        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            try {
                git("fetch", "origin", branch, "--quiet")
                // Synchronize version.properties with origin to handle concurrent builds
                git("checkout", "origin/$branch", "--", "version.properties")
                
                val props = Properties()
                versionFile.inputStream().use { props.load(it) }
                val patch = (props.getProperty("versionPatch")?.toInt() ?: 0) + 1
                val build = (props.getProperty("versionBuild")?.toInt() ?: 0) + 1
                
                val content = versionFile.readText()
                val updatedContent = content
                    .replace(Regex("versionPatch\\s*=\\s*\\d+"), "versionPatch=$patch")
                    .replace(Regex("versionBuild\\s*=\\s*\\d+"), "versionBuild=$build")
                
                versionFile.writeText(updatedContent)
                
                git("config", "user.name", "github-actions[bot]")
                git("config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
                git("add", "version.properties")
                val message = customMessage ?: "chore: bump version to $patch.$build [skip ci]"
                git("commit", "-m", message)
                git("push", "origin", "HEAD:$branch")
                println("Successfully pushed version bump: patch=$patch, build=$build to branch $branch")
                break
            } catch (e: Exception) {
                if (attempt == maxAttempts) throw e
                println("Push attempt $attempt failed (likely a concurrent push) - retrying in 2s... Error: ${e.message}")
                Thread.sleep(2000)
            }
        }
    }
}

// release-play.yml sets this before invoking bundleRelease: that workflow has already called
// incrementAndPushVersion explicitly and reconciled the result against Play's own highest
// versionCode, so version.properties is deliberately final by the time bundleRelease's
// configuration phase reads it. Without this switch, autoIncrementVersion still ran as a hidden
// dependency of whatever compile/assemble/bundle/kapt task happened to be in that build's task
// graph - not just composeApp's own bundleRelease/bundlePlaystoreRelease, but any library
// subproject's plain compile task too (e.g. :shared:compileAndroidMain), none of which are named
// distinctly enough to exclude by a name pattern - and rewrote version.properties *again* (its
// own execution phase, right after bundleRelease's configuration phase had already captured
// versionCode from the file). That was invisible to the build itself, but it meant the *next*
// Gradle invocation in the same job (`printVersionEnv`, for "Confirm the built versionCode clears
// Play" / the Play upload's expected-version-code) read that further-incremented file and
// reported a version one higher than what was actually signed into the .aab - confirmed on run
// 31933127627: expected 250, Play reported the upload as 249. Debug-build-type tasks
// (assembleDebug, the only other consumer of this project's Gradle tasks) still get the
// auto-bump: that pipeline's own versioning is provided by an explicit -PversionBuild instead, so
// nothing there depends on file-read timing the way release-play.yml's does.
val skipAutoIncrement = providers.gradleProperty("skipAutoIncrementVersion").isPresent

subprojects {
    tasks.configureEach {
        val taskName = name.lowercase()
        if (!skipAutoIncrement
            && (taskName.contains("compile") || taskName.contains("assemble") || taskName.contains("bundle") || taskName.contains("kapt"))
            && name != "autoIncrementVersion"
            && name != "incrementAndPushVersion"
            && name != "printVersionEnv"
        ) {
            dependsOn(autoIncrementVersion)
        }
    }
}
