pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Compose Hot Reload's shared:desktop target needs a real JetBrains Runtime (JBR), not whatever
// JDK happens to be on PATH - this resolver is what lets Gradle's own toolchain support fetch one
// automatically instead of failing with "no matching toolchain".
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "HG2Gui"
include(":composeApp", ":shared", ":terminal-emulator", ":termux-shared")
