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
val hasReleaseSigningEnv = !releaseKeystoreFile.isNullOrBlank() && !releaseKeystorePassword.isNullOrBlank() && !releaseKeyAlias.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()

// REQUIRE_SIGNING=true  -> configuring the build without the signing env is an error (as before).
// REQUIRE_SIGNING=false -> an unsigned release APK is acceptable (local experiments).
// unset                 -> on CI (the CI env var, which GitHub Actions sets) a task that PACKAGES a
//                          release (assemble/bundle/package ...Release) fails without the signing env,
//                          instead of silently producing an unsigned, un-updatable APK; locally it is
//                          permitted. Test/compile/lint tasks never need the keystore. Checked once the
//                          task graph is known (see the taskGraph hook below).
val requireSigningSetting: Boolean? = System.getenv("REQUIRE_SIGNING")?.trim()?.lowercase()?.let {
    when (it) {
        "true" -> true
        "false" -> false
        else -> null
    }
}
val runningOnCi = !System.getenv("CI").isNullOrEmpty()

if (requireSigningSetting == true && !hasReleaseSigningEnv) error("REQUIRE_SIGNING is set but one or more of KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD is missing.")

gradle.taskGraph.whenReady {
    val packagesRelease = allTasks.any { task ->
        task.project == project && Regex("^(assemble|bundle|package|publish)(.*)Release$").matches(task.name)
    }
    if (packagesRelease && !hasReleaseSigningEnv && requireSigningSetting == null && runningOnCi) {
        throw GradleException(
            "Refusing to package a release build without signing: KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD " +
                "are not all set. Set REQUIRE_SIGNING=false to build an unsigned release on purpose."
        )
    }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

val generatedLauncherJniDir = layout.buildDirectory.dir("generated/nativeLaunchers/jniLibs")

// The one place the NDK version is pinned: gradle/libs.versions.toml ("ndk"). AGP's ndkVersion and
// both native-launcher tasks below read it; .github/workflows/ci.yml reads the same toml line.
val pinnedNdkVersion: String = libs.versions.ndk.get()

// SDK location for the native-launcher tasks, resolved the way AGP resolves it: local.properties
// `sdk.dir` first (what Android Studio writes, so an IDE-launched build works with no env vars),
// then ANDROID_HOME / ANDROID_SDK_ROOT. Looked up when a task runs, not at configuration time.
fun resolveAndroidSdkRoot(): String {
    val localProps = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.isFile) file.inputStream().use { load(it) }
    }
    return localProps.getProperty("sdk.dir")?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANDROID_HOME")?.takeIf { it.isNotBlank() }
        ?: System.getenv("ANDROID_SDK_ROOT")?.takeIf { it.isNotBlank() }
        ?: error("Android SDK not found: set sdk.dir in local.properties, or ANDROID_HOME / ANDROID_SDK_ROOT")
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
        // The bundled Termux-derived binaries (src/main/jniLibs) and native launchers are
        // arm64-v8a only; without this filter the app would install on other ABIs and then
        // fail at first launch with missing libraries.
        ndk { abiFilters += "arm64-v8a" }
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
            // Explicit: this is the build the in-app updater distributes (AppUpdateChecker /
            // UpdatePolicy.kt), and it refuses debuggable APKs.
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigningEnv) signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        getByName("debug") {
            // The debug build type is NEVER signed with the release key. It used to be (so that
            // AppUpdateChecker's signer comparison passed for CI debug builds), which put a
            // debuggable, un-minified APK under the production signature on the update channel:
            // `adb shell run-as` could then read the app's data with no root. Debug builds use the
            // local debug keystore; the update channel ships assemblePlaystoreRelease output.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    flavorDimensions += "default"
    productFlavors { create("playstore") { dimension = "default" } }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    ndkVersion = pinnedNdkVersion
    buildToolsVersion = "37.0.0"
}

fun registerNativeLauncherTask(taskName: String, sourceName: String, outputName: String) = tasks.register(taskName) {
    val source = layout.projectDirectory.file("src/main/cpp/$sourceName")
    val output = layout.buildDirectory.file("generated/nativeLaunchers/jniLibs/arm64-v8a/$outputName")
    inputs.file(source)
    outputs.file(output)

    doLast {
        val sdkRoot = resolveAndroidSdkRoot()
        val ndkVersion = pinnedNdkVersion
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
        val sdkRoot = resolveAndroidSdkRoot()
        val ndkVersion = pinnedNdkVersion
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
