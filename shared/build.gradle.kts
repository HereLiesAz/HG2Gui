import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidLibrary {
        namespace = "com.hereliesaz.hg2gui.shared"
        compileSdk = 37
        minSdk = 24
        
        withHostTest {}

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
