import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

val versionProps = Properties().apply {
    val versionPropsFile = rootProject.file("version.properties")
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

val legacyVersionCode = 205
val buildNumber = (project.findProperty("versionBuild")?.toString() ?: versionProps.getProperty("versionBuild", "0")).toInt()
val resolvedVersionCode = maxOf(buildNumber, legacyVersionCode + 1)

val resolvedVersionName = project.findProperty("versionName")?.toString() ?: String.format(
    "%s.%s.%s.%d",
    versionProps.getProperty("versionMajor", "0"),
    versionProps.getProperty("versionMinor", "0"),
    versionProps.getProperty("versionPatch", "0"),
    buildNumber
)

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            implementation(libs.okhttp)
            implementation(libs.htmlcleaner)
            implementation(libs.json.path)
            implementation(libs.jsoup)
            implementation(libs.comparestring2)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            // localbroadcastmanager is legacy, but keeping for now
            implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
            implementation("com.google.android.material:material:1.13.0")
            
            implementation(project(":terminal-emulator"))
            implementation(project(":terminal-view"))
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
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/androidMain/kotlin")
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
        }
    }
    
    flavorDimensions += "default"
    productFlavors {
        create("fdroid") {
            dimension = "default"
        }
        create("playstore") {
            dimension = "default"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
