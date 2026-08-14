import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
}

val versionProps = Properties().apply {
    val versionPropsFile = rootProject.file("version.properties")
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

val legacyVersionCode = 205

// versionBuild in version.properties is versionCode's single source of truth. This is a plain
// read - incrementing it is a dedicated, standalone task (incrementVersionBuild, below), run as
// its own separate, earlier Gradle invocation in CI, strictly before ./gradlew bundleRelease is
// ever invoked. By the time this build configures, whatever's on disk (and already pushed to
// master) is final. -PversionBuild=<n> remains an explicit override, used by merged-build.yml's
// debug workflow with its own commit-count-derived number, independent of this file.
val buildNumber = project.findProperty("versionBuild")?.toString()?.toIntOrNull()
    ?: versionProps.getProperty("versionBuild", "0").toIntOrNull() ?: 0

val resolvedVersionCode = maxOf(buildNumber, legacyVersionCode + 1)

val resolvedVersionName = project.findProperty("versionName")?.toString() ?: String.format(
    "%s.%s.%s.%d",
    versionProps.getProperty("versionMajor", "0"),
    versionProps.getProperty("versionMinor", "0"),
    versionProps.getProperty("versionPatch", "0"),
    buildNumber
)

// Populated by .GitHub/actions/android-keystore in CI (release-play.yml); absent for a plain
// local `assembleRelease`, which then just builds unsigned like it always has. When
// REQUIRE_SIGNING is set, a missing/incomplete keystore fails the build here, at
// configuration time, instead of producing an unsigned bundle that only fails later, in
// ./.GitHub/actions/verify-android-signature or at the Play upload itself.
val releaseKeystoreFile = System.getenv("KEYSTORE_FILE")
val releaseKeystoreType = System.getenv("KEYSTORE_TYPE")
val releaseKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("KEY_ALIAS")
val releaseKeyPassword = System.getenv("KEY_PASSWORD")
val releaseRequireSigning = System.getenv("REQUIRE_SIGNING").toBoolean()

val hasReleaseSigningEnv = !releaseKeystoreFile.isNullOrBlank() &&
    !releaseKeystorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

if (releaseRequireSigning && !hasReleaseSigningEnv) {
    error(
        "REQUIRE_SIGNING is set but one or more of KEYSTORE_FILE / " +
            "KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD is missing."
    )
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            
            implementation(libs.okhttp)
            implementation(libs.comparestring2)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment)
            implementation(libs.androidx.biometric)
            implementation(libs.anthropic.java)
            // localbroadcastmanager is legacy, but keeping for now
            implementation(libs.androidx.localbroadcastmanager)
            implementation(libs.material)

            implementation(project(":terminal-emulator"))
            implementation(project(":termux-shared"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
            implementation(libs.mockito.core)
        }
    }
}

android {
    namespace = "com.hereliesaz.hg2gui"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hereliesaz.hg2gui"
        minSdk = 24
        targetSdk = 37
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // anthropic-java pulls in Apache HttpClient5, whose jars (httpclient5, httpcore5,
            // httpcore5-h2) all ship their own copy of this file under the same path - only
            // the release build's resource-merge step catches this (debug compile doesn't
            // merge Java resources), which is why it didn't surface locally until bundleRelease.
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/androidMain/kotlin")
        }
    }
    
    signingConfigs {
        if (hasReleaseSigningEnv) {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                if (!releaseKeystoreType.isNullOrBlank()) storeType = releaseKeystoreType
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    
    flavorDimensions += "default"
    productFlavors {

        create("playstore") {
            dimension = "default"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.ui.tooling)
    implementation(libs.listenablefuture)
}

// Read by release-play.yml's "Read application id" step (./gradlew -q :composeApp:printApplicationId)
// to fill in $GITHUB_ENV without hardcoding the id a second time in the workflow itself. Output
// is prefixed and grepped for on the workflow side, not taken as the command's whole stdout - on
// a fresh CI checkout this is the first Gradle invocation of the job, and the Android Gradle
// Plugin's own SDK/NDK auto-license-acceptance path prints straight to stdout outside Gradle's
// logging (so -q doesn't suppress it), which would otherwise land inside the captured value.
tasks.register("printApplicationId") {
    doLast {
        println("APPLICATION_ID=" + android.defaultConfig.applicationId)
    }
}

// Same reasoning and marker-line convention as printApplicationId above. Read by
// release-play.yml's "Get app version" step, run after incrementVersionBuild has already run
// (and bundleRelease has run against that same, already-final value), so these match the .aab.
tasks.register("printVersionName") {
    doLast {
        println("VERSION_NAME=$resolvedVersionName")
    }
}

tasks.register("printVersionCode") {
    doLast {
        println("VERSION_CODE=$resolvedVersionCode")
    }
}

/**
 * The ONLY place versionBuild is incremented - a standalone task, run as its own explicit,
 * earlier CI step (release-play.yml's "Increment and push versionBuild"), strictly before
 * ./gradlew bundleRelease is ever invoked. Reads the freshest committed version.properties
 * (fetch + reset first, so a concurrent run's push isn't clobbered), decides the next number,
 * writes it to disk, and commits + pushes it to master itself - "push to GitHub before
 * compilation" is the literal requirement this satisfies. It is not a mutation buried in this
 * script's configuration phase, and not a bash script wrapped around Gradle either: the decision
 * of what the next number is, and the act of persisting it, both happen here.
 *
 * -PversionCodeMode=strict fails outright (nothing written, nothing pushed, nothing built) if
 * the committed number wouldn't already clear Play's max. auto (the default) always takes the
 * next code after Play's max, so it can't collide no matter how stale the file is.
 * -PplayMaxVersionCode=<n> supplies that max; CI reads it from the Play API before calling this
 * task, since Gradle has no business making that network call itself - this task only ever
 * consumes the one external fact it cannot know on its own, never decides what to ask Play.
 */
tasks.register("incrementVersionBuild") {
    doLast {
        val versionCodeMode = project.findProperty("versionCodeMode")?.toString() ?: "auto"
        val playMax = project.findProperty("playMaxVersionCode")?.toString()?.toIntOrNull() ?: 0
        val versionFile = rootProject.file("version.properties")

        fun git(vararg args: String) {
            val process = ProcessBuilder(listOf("git") + args)
                .directory(rootProject.projectDir)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().forEachLine { println(it) }
            val exit = process.waitFor()
            if (exit != 0) throw GradleException("git ${args.joinToString(" ")} failed (exit $exit)")
        }

        val maxAttempts = 3
        for (attempt in 1..maxAttempts) {
            git("fetch", "origin", "master", "--quiet")
            git("reset", "--hard", "origin/master", "--quiet")

            val freshProps = Properties().apply { versionFile.inputStream().use { load(it) } }
            val current = freshProps.getProperty("versionBuild", "0").toIntOrNull() ?: 0

            if (versionCodeMode == "strict" && current <= playMax) {
                throw GradleException(
                    "versionCode $current would not clear Play, which already has $playMax. " +
                        "Set versionBuild=$playMax in version.properties and commit, or use auto mode."
                )
            }

            val next = maxOf(current, playMax) + 1
            // A targeted line substitution, not Properties.store() - store() rewrites the whole
            // file with its own field order and an auto-generated timestamp comment, which would
            // turn every single release into a full-file diff instead of a one-line change.
            val versionBuildLine = Regex("(?m)^versionBuild=.*$")
            val text = versionFile.readText()
            versionFile.writeText(
                if (versionBuildLine.containsMatchIn(text)) {
                    text.replace(versionBuildLine, "versionBuild=$next")
                } else {
                    text.trimEnd('\n') + "\nversionBuild=$next\n"
                }
            )

            git("commit", "-q", "-am", "chore: bump versionBuild to $next [skip ci]")
            try {
                git("push", "-q", "origin", "HEAD:master")
                println("VERSION_BUILD=$next")
                break
            } catch (e: GradleException) {
                if (attempt == maxAttempts) throw e
                println("Push attempt $attempt failed (master probably moved) - retrying.")
                Thread.sleep((attempt * 3000).toLong())
            }
        }
    }
}
