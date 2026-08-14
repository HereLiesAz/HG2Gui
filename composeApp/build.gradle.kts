import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
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
val buildNumberFromProp = project.findProperty("versionBuild")?.toString()?.toIntOrNull()
val buildNumber = buildNumberFromProp ?: ((versionProps.getProperty("versionBuild", "0").toIntOrNull() ?: 0) + 1)

val resolvedVersionCode = maxOf(buildNumber, legacyVersionCode + 1)

val resolvedVersionName = project.findProperty("versionName")?.toString() ?: String.format(
    "%s.%s.%s.%d",
    versionProps.getProperty("versionMajor", "0"),
    versionProps.getProperty("versionMinor", "0"),
    (versionProps.getProperty("versionPatch", "0").toIntOrNull() ?: 0) + 1,
    buildNumber
)

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
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
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
            excludes += "/META-INF/DEPENDENCIES"
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
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "37.0.0"
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":terminal-emulator"))
    implementation(project(":termux-shared"))
    
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.material)
    
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
    implementation(libs.ui)
    
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.listenablefuture)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.ui.tooling)
}

tasks.register("printApplicationId") {
    doLast {
        println("APPLICATION_ID=" + android.defaultConfig.applicationId)
    }
}

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
            val currentBuild = freshProps.getProperty("versionBuild", "0").toIntOrNull() ?: 0
            val currentPatch = freshProps.getProperty("versionPatch", "0").toIntOrNull() ?: 0

            if (versionCodeMode == "strict" && currentBuild <= playMax) {
                throw GradleException(
                    "versionCode $currentBuild would not clear Play, which already has $playMax. " +
                        "Set versionBuild=$playMax in version.properties and commit, or use auto mode."
                )
            }

            val nextBuild = maxOf(currentBuild, playMax) + 1
            val nextPatch = currentPatch + 1

            val text = versionFile.readText()
            val updatedText = text
                .replace(Regex("versionPatch\\s*=\\s*\\d+"), "versionPatch=$nextPatch")
                .replace(Regex("versionBuild\\s*=\\s*\\d+"), "versionBuild=$nextBuild")
            
            versionFile.writeText(updatedText)

            git("commit", "-q", "-am", "chore: bump version to $nextPatch.$nextBuild [skip ci]")
            try {
                git("push", "-q", "origin", "HEAD:master")
                println("VERSION_BUILD=$nextBuild")
                println("VERSION_PATCH=$nextPatch")
                break
            } catch (e: GradleException) {
                if (attempt == maxAttempts) throw e
                println("Push attempt $attempt failed (master probably moved) - retrying.")
                Thread.sleep((attempt * 3000).toLong())
            }
        }
    }
}
