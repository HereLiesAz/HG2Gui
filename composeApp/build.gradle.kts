import com.android.build.api.variant.impl.VariantOutputImpl
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
        // DistroManager symlinks the bundled Termux bootstrap's real bin/bash (etc.) to its
        // exec-exempt counterpart under context.applicationInfo.nativeLibraryDir - which only
        // works if that directory holds real extracted files ProcessBuilder can exec directly.
        // AGP's newer default (useLegacyPackaging = false) instead ships native libs page-aligned
        // and uncompressed inside the APK itself, mmap'd on demand rather than extracted to disk;
        // nativeLibraryDir may not contain real files at all under that scheme. Forcing legacy
        // packaging guarantees the extraction this whole design depends on.
        jniLibs {
            useLegacyPackaging = true
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

// The default output name (composeApp-<flavor>-<buildType>.apk) carries no version at all - every
// build overwrites the same filename, and once one lands on the "latest-debug"/"latest" GitHub
// Release (see _reusable-build.yml's Publish Release step, which uploads this exact file) there's
// no way to tell which build a previously-downloaded APK actually is. VariantOutputImpl's
// outputFileName is the actual mutable Property; the public VariantOutput interface only exposes
// it read-only, so every AGP renaming setup - this one included - casts down to the impl class.
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) {
                output.outputFileName.set(
                    "hg2gui-${variant.flavorName}-${variant.buildType}-$resolvedVersionName.apk"
                )
            }
        }
    }
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

    // composeApp is a plain `com.android.application` module, not Kotlin Multiplatform - its
    // unit-test source root is src/test/, not the KMP-style src/androidUnitTest/ these test
    // files used to sit in (where they silently never compiled or ran at all: no testImplementation
    // dependency existed for them either, so even the source-set move alone wasn't enough).
    testImplementation(libs.junit)
}

tasks.register("printApplicationId") {
    doLast {
        println("APPLICATION_ID=" + android.defaultConfig.applicationId)
    }
}
