import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
    // Dev tooling only, never shipped to users - a desktop target purely so an AI agent (or a
    // human at Android Studio) can drive PillMenu/TerminalScreen live via this plugin's own MCP
    // server (see PreviewMain.kt's own doc comment), since the real app is Android-only and
    // Compose Hot Reload doesn't support that target directly yet.
    alias(libs.plugins.composeHotReload)
}

kotlin {
    android {
        namespace = "com.hereliesaz.hg2gui.shared"
        compileSdk = 37
        minSdk = 24
        
        withJava()
        withHostTest {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    // See the plugin comment above - this exists only to give Compose Hot Reload's MCP server
    // something to run, never built or shipped as part of the real app.
    jvm("desktop") {
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
            implementation(libs.androidx.uiautomator.shell)
            implementation(libs.anthropic.java)
            implementation(libs.androidx.localbroadcastmanager)
            implementation(libs.material)

            implementation(project(":terminal-emulator"))
            implementation(project(":termux-shared"))
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
            implementation(libs.mockito.core)
        }
        
        val androidHostTest by getting {
            dependsOn(commonTest.get())
        }
    }
}

// Only the mainClass matters here - PreviewMain never gets packaged/distributed (no
// nativeDistributions block), it just gives the desktop target's own `run`/hot-reload tasks
// something to launch.
compose.desktop {
    application {
        mainClass = "com.hereliesaz.hg2gui.preview.PreviewMainKt"
    }
}
