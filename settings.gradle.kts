pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    // Every Gradle plugin here is already on its latest release, yet they still pull vulnerable
    // build-time libraries (Dependabot alerts on settings.gradle.kts). Force the patched releases
    // onto the build classpath. Build tooling only - none of these ship in the APK.
    configurations.classpath {
        resolutionStrategy.eachDependency {
            when (requested.group) {
                "org.jdom" -> if (requested.name == "jdom2") useVersion("2.0.6.1")
                "org.bouncycastle" -> if (requested.name.endsWith("-jdk18on")) useVersion("1.86")
                "com.fasterxml.jackson.core" ->
                    if (requested.name == "jackson-core" || requested.name == "jackson-databind") useVersion("2.22.3")
                "org.bitbucket.b_c" -> if (requested.name == "jose4j") useVersion("0.9.7")
                "org.apache.commons" -> if (requested.name == "commons-lang3") useVersion("3.21.0")
            }
        }
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
        // JitPack builds arbitrary GitHub repos on demand, so it is consulted ONLY for its own
        // `com.github.<user>` coordinates: no other group (okhttp, kotlinx, androidx, ...) can ever be
        // resolved from it, which closes the dependency-confusion path a blanket jitpack.io entry opens.
        // Nothing in the catalog resolves from here today (CompareString2 is on Maven Central); the
        // entry stays for future `com.github.*` dependencies. Gradle dependency-verification metadata
        // (gradle/verification-metadata.xml) is NOT in place - it needs a networked
        // `./gradlew --write-verification-metadata sha256 ...` run on a developer machine.
        maven {
            url = uri("https://jitpack.io")
            content { includeGroupByRegex("""com\.github\..*""") }
        }
    }
}

rootProject.name = "HG2Gui"
include(":composeApp", ":shared", ":terminal-emulator")
