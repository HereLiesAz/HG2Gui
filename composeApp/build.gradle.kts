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
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}

val legacyVersionCode = versionProps.getProperty("legacyVersionCode", "205").toInt()
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
val hasReleaseSigningEnv = !releaseKeystoreFile.isNullOrBlank() && !releaseKeystorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()

if (releaseRequireSigning && !hasReleaseSigningEnv) error("REQUIRE_SIGNING is set but one or more of KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD is missing.")

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

val generatedLauncherJniDir = layout.buildDirectory.dir("generated/nativeLaunchers/jniLibs")

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
    sourceSets.getByName("main").jniLibs.directories.add(generatedLauncherJniDir.get().asFile.absolutePath)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
        jniLibs { useLegacyPackaging = true }
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }
    flavorDimensions += "default"
    productFlavors { create("playstore") { dimension = "default" } }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    ndkVersion = "29.0.14206865"
    buildToolsVersion = "37.0.0"
}

fun registerNativeLauncherTask(taskName: String, sourceName: String, outputName: String) = tasks.register(taskName) {
    val source = layout.projectDirectory.file("src/main/cpp/$sourceName")
    val output = layout.buildDirectory.file("generated/nativeLaunchers/jniLibs/arm64-v8a/$outputName")
    inputs.file(source)
    outputs.file(output)

    doLast {
        val sdkRoot = System.getenv("ANDROID_SDK_ROOT")
            ?: System.getenv("ANDROID_HOME")
            ?: error("ANDROID_SDK_ROOT/ANDROID_HOME is not set")
        val ndkVersion = "29.0.14206865"
        val hostTag = when {
            System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows-x86_64"
            System.getProperty("os.name").startsWith("Mac", ignoreCase = true) ->
                if (System.getProperty("os.arch") == "aarch64") "darwin-arm64" else "darwin-x86_64"
            else -> "linux-x86_64"
        }
        val suffix = if (hostTag.startsWith("windows")) ".cmd" else ""
        val clang = file("$sdkRoot/ndk/$ndkVersion/toolchains/llvm/prebuilt/$hostTag/bin/aarch64-linux-android24-clang$suffix")
        check(clang.isFile) { "Android NDK clang not found: $clang" }

        val outFile = output.get().asFile
        outFile.parentFile.mkdirs()

        val process = ProcessBuilder(
            clang.absolutePath,
            source.asFile.absolutePath,
            "-O2",
            "-fPIE",
            "-pie",
            "-o",
            outFile.absolutePath
        )
            .inheritIO()
            .start()
        check(process.waitFor() == 0) { "Failed to compile $sourceName" }
    }
}

fun registerNativeSharedLibraryTask(taskName: String, sourceName: String, outputName: String) = tasks.register(taskName) {
    val source = layout.projectDirectory.file("src/main/cpp/$sourceName")
    val output = layout.buildDirectory.file("generated/nativeLaunchers/jniLibs/arm64-v8a/$outputName")
    inputs.file(source)
    outputs.file(output)

    doLast {
        val sdkRoot = System.getenv("ANDROID_SDK_ROOT")
            ?: System.getenv("ANDROID_HOME")
            ?: error("ANDROID_SDK_ROOT/ANDROID_HOME is not set")
        val ndkVersion = "29.0.14206865"
        val hostTag = when {
            System.getProperty("os.name").startsWith("Windows", ignoreCase = true) -> "windows-x86_64"
            System.getProperty("os.name").startsWith("Mac", ignoreCase = true) ->
                if (System.getProperty("os.arch") == "aarch64") "darwin-arm64" else "darwin-x86_64"
            else -> "linux-x86_64"
        }
        val suffix = if (hostTag.startsWith("windows")) ".cmd" else ""
        val clang = file("$sdkRoot/ndk/$ndkVersion/toolchains/llvm/prebuilt/$hostTag/bin/aarch64-linux-android24-clang$suffix")
        check(clang.isFile) { "Android NDK clang not found: $clang" }

        val outFile = output.get().asFile
        outFile.parentFile.mkdirs()

        val process = ProcessBuilder(
            clang.absolutePath,
            source.asFile.absolutePath,
            "-O2",
            "-fPIC",
            "-shared",
            "-Wl,-soname,$outputName",
            "-o",
            outFile.absolutePath
        )
            .inheritIO()
            .start()
        check(process.waitFor() == 0) { "Failed to compile $sourceName" }
    }
}

val buildAptKeyLauncher = registerNativeLauncherTask(
    "buildAptKeyLauncher",
    "apt_key_launcher.c",
    "libhg2gui_apt_key.so"
)
val buildDpkgLauncher = registerNativeLauncherTask(
    "buildDpkgLauncher",
    "dpkg_launcher.c",
    "libhg2gui_dpkg.so"
)
val buildMaintscriptLauncher = registerNativeLauncherTask(
    "buildMaintscriptLauncher",
    "maintscript_launcher.c",
    "libhg2gui_maintscript.so"
)
val buildHg2ExecPreload = registerNativeSharedLibraryTask(
    "buildHg2ExecPreload",
    "hg2exec_preload.c",
    "libhg2gui_exec_preload.so"
)

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(buildAptKeyLauncher, buildDpkgLauncher, buildMaintscriptLauncher, buildHg2ExecPreload)
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) output.outputFileName.set("hg2gui-${variant.flavorName}-${variant.buildType}-$resolvedVersionName.apk")
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
    implementation(libs.okhttp)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.ui.tooling)
    testImplementation(libs.junit)
}

tasks.register("printApplicationId") {
    doLast { println("APPLICATION_ID=" + android.defaultConfig.applicationId) }
}
