import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

val autoIncrementVersion = tasks.register("autoIncrementVersion") {
    doLast {
        val versionFile = rootProject.file("version.properties")
        if (versionFile.exists()) {
            val props = Properties()
            versionFile.inputStream().use { props.load(it) }

            val patch = (props.getProperty("versionPatch")?.toInt() ?: 0) + 1
            val build = (props.getProperty("versionBuild")?.toInt() ?: 0) + 1

            val content = versionFile.readText()
            val updatedContent = content
                .replace(Regex("versionPatch\\s*=\\s*\\d+"), "versionPatch=$patch")
                .replace(Regex("versionBuild\\s*=\\s*\\d+"), "versionBuild=$build")

            versionFile.writeText(updatedContent)
            println("Auto-incremented version.properties: patch=$patch, build=$build")
        }
    }
}

subprojects {
    tasks.configureEach {
        val taskName = name.lowercase()
        if ((taskName.contains("compile") || taskName.contains("assemble") || taskName.contains("bundle") || taskName.contains("kapt"))
            && name != "autoIncrementVersion"
        ) {
            dependsOn(autoIncrementVersion)
        }
    }
}
