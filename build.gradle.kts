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

/**
 * Prints the anticipated version information (current + 1) to GITHUB_ENV if available,
 * or simply to stdout. This ensures CI workflows use the same version numbers as the build.
 */
tasks.register("printVersionEnv") {
    doLast {
        val versionFile = rootProject.file("version.properties")
        if (versionFile.exists()) {
            val props = Properties()
            versionFile.inputStream().use { props.load(it) }

            val major = props.getProperty("versionMajor", "0")
            val minor = props.getProperty("versionMinor", "0")
            val patch = (props.getProperty("versionPatch")?.toInt() ?: 0) + 1
            val build = (props.getProperty("versionBuild")?.toInt() ?: 0) + 1
            val versionName = "$major.$minor.$patch.$build"

            val githubEnv = System.getenv("GITHUB_ENV")
            if (githubEnv != null) {
                File(githubEnv).appendText(
                    """
                    VERSION_MAJOR=$major
                    VERSION_MINOR=$minor
                    VERSION_PATCH=$patch
                    VERSION_BUILD=$build
                    VERSION_NAME=$versionName
                    """.trimIndent() + "\n"
                )
            }
            println("VERSION_MAJOR=$major")
            println("VERSION_MINOR=$minor")
            println("VERSION_PATCH=$patch")
            println("VERSION_BUILD=$build")
            println("VERSION_NAME=$versionName")
        }
    }
}

tasks.register("incrementAndPushVersion") {
    doLast {
        val versionFile = rootProject.file("version.properties")
        val branch = project.findProperty("targetBranch")?.toString() ?: "master"
        val customMessage = project.findProperty("commitMessage")?.toString()
        
        fun git(vararg args: String) {
            val process = ProcessBuilder(listOf("git") + args)
                .directory(rootProject.projectDir)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().forEachLine { println(it) }
            val exit = process.waitFor()
            if (exit != 0) throw GradleException("git ${args.joinToString(" ")} failed (exit $exit)")
        }

        val maxAttempts = 5
        for (attempt in 1..maxAttempts) {
            try {
                git("fetch", "origin", branch, "--quiet")
                git("checkout", "origin/$branch", "--", "version.properties")
                
                val props = Properties()
                versionFile.inputStream().use { props.load(it) }
                val patch = (props.getProperty("versionPatch")?.toInt() ?: 0) + 1
                val build = (props.getProperty("versionBuild")?.toInt() ?: 0) + 1
                
                val content = versionFile.readText()
                val updatedContent = content
                    .replace(Regex("versionPatch\\s*=\\s*\\d+"), "versionPatch=$patch")
                    .replace(Regex("versionBuild\\s*=\\s*\\d+"), "versionBuild=$build")
                
                versionFile.writeText(updatedContent)
                
                git("config", "user.name", "github-actions[bot]")
                git("config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
                git("add", "version.properties")
                val message = customMessage ?: "chore: bump version to $patch.$build [skip ci]"
                git("commit", "-m", message)
                git("push", "origin", "HEAD:$branch")
                println("Successfully pushed version bump: patch=$patch, build=$build")
                break
            } catch (e: Exception) {
                if (attempt == maxAttempts) throw e
                println("Push attempt $attempt failed - retrying. Error: ${e.message}")
                Thread.sleep((attempt * 2000).toLong())
            }
        }
    }
}

subprojects {
    tasks.configureEach {
        val taskName = name.lowercase()
        if ((taskName.contains("compile") || taskName.contains("assemble") || taskName.contains("bundle") || taskName.contains("kapt"))
            && name != "autoIncrementVersion"
            && name != "incrementAndPushVersion"
            && name != "printVersionEnv"
        ) {
            dependsOn(autoIncrementVersion)
        }
    }
}
