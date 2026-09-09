package com.hereliesaz.hg2gui.terminal

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayDeque
import java.util.Locale
import java.util.zip.GZIPInputStream

class Hg2PackageManager(
    private val context: Context,
    private val client: OkHttpClient,
    private val downloader: Hg2Downloader
) {
    private val prefix = DistroManager.prefixDir(context)
    private val cacheDir = File(context.cacheDir, "hg2-packages").apply { mkdirs() }
    private val stateDir = File(prefix, "var/lib/hg2pkg").apply { mkdirs() }
    private val packagesFile = File(stateDir, "Packages")
    private val dpkgLauncher = File(context.applicationInfo.nativeLibraryDir, "libhg2gui_dpkg.so")
    private val dpkgDeb = File(prefix, "bin/dpkg-deb")
    private val bash = File(prefix, "bin/bash")
    private val dpkgInfoDir = File(prefix, "var/lib/dpkg/info")
    private val repository = TermuxRepositoryClient(context, client)
    private var activeMirror: String? = null

    data class Relation(
        val name: String,
        val operator: String? = null,
        val version: String? = null
    ) {
        override fun toString(): String = if (operator != null && version != null) "$name ($operator $version)" else name
    }

    data class PackageRecord(
        val name: String,
        val version: String,
        val architecture: String,
        val filename: String,
        val sha256: String,
        val size: Long,
        val preDepends: List<List<Relation>>,
        val depends: List<List<Relation>>,
        val provides: List<Relation>,
        val conflicts: List<Relation>,
        val breaks: List<Relation>,
        val replaces: List<Relation>,
        val description: String
    )

    private data class Installed(
        val name: String,
        val version: String,
        val provides: List<Relation> = emptyList(),
        val conflicts: List<Relation> = emptyList(),
        val breaks: List<Relation> = emptyList()
    )

    private data class PreparedPackage(
        val pkg: PackageRecord,
        val archive: File,
        val scriptsDir: File
    )

    fun handles(line: String): Boolean = words(line).firstOrNull() in setOf("pkg", "hg2pkg")

    fun run(line: String): Flow<String> = channelFlow {
        val output: suspend (String) -> Unit = { send(it) }
        val args = words(line).drop(1)
        when (args.firstOrNull()) {
            "install", "in" -> install(requirePackages(args, "install"), forceRequested = false, emit = output)
            "update" -> updateIndex(output)
            "upgrade", "up" -> upgrade(output)
            "remove", "rm", "uninstall" -> remove(requirePackages(args, "remove"), purge = false, emit = output)
            "purge" -> remove(requirePackages(args, "purge"), purge = true, emit = output)
            "clean" -> clean(output)
            "search" -> search(args.drop(1).joinToString(" "), output)
            "show", "info" -> show(requirePackages(args, "show"), output)
            "list-installed" -> {
                val installed = readInstalled().values.sortedBy { it.name }
                send(if (installed.isEmpty()) "No installed packages recorded by dpkg." else installed.joinToString("\n") { "${it.name} ${it.version}" })
            }
            null -> send(USAGE)
            else -> send("HG2Gui package manager: unsupported operation '${args.first()}'.\n$USAGE")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun clean(emit: suspend (String) -> Unit) {
        val removable = cacheDir.listFiles().orEmpty().filter {
            it.isFile && (it.extension == "deb" || it.name.endsWith(".part")) ||
                it.isDirectory && (it.name.startsWith("prepare-") || it.name.startsWith("scripts-") || it.name.startsWith("installed-scripts-"))
        }
        removable.forEach { if (it.isDirectory) it.deleteRecursively() else it.delete() }
        emit("Removed ${removable.size} cached package item${if (removable.size == 1) "" else "s"}.")
    }

    private fun requirePackages(args: List<String>, operation: String): List<String> {
        val packages = args.drop(1).filterNot { it.startsWith("-") }
        if (packages.isEmpty()) error("usage: pkg $operation <package> [package…]")
        return packages
    }

    private suspend fun upgrade(emit: suspend (String) -> Unit) {
        updateIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        val upgrades = installed.values.mapNotNull { current ->
            val candidate = available[current.name] ?: return@mapNotNull null
            candidate.takeIf { isVersionLessThan(current.version, candidate.version) }
        }
        if (upgrades.isEmpty()) {
            emit("All installed packages are up to date.")
            return
        }
        emit("Upgrading ${upgrades.size} package${if (upgrades.size == 1) "" else "s"}:")
        upgrades.forEach { emit("  ${it.name}: ${installed.getValue(it.name).version} → ${it.version}") }
        install(upgrades.map { it.name }, forceRequested = true, emit = emit)
    }

    private suspend fun search(query: String, emit: suspend (String) -> Unit) {
        if (query.isBlank()) error("usage: pkg search <query>")
        ensureIndex(emit)
        val records = parsePackages(packagesFile.readText())
            .filter { it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
            .take(50)
        emit(if (records.isEmpty()) "No packages matching '$query'." else records.joinToString("\n") { "${it.name} ${it.version} — ${it.description.lineSequence().firstOrNull().orEmpty()}" })
    }

    private suspend fun show(names: List<String>, emit: suspend (String) -> Unit) {
        ensureIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        names.forEachIndexed { index, name ->
            val pkg = available[name] ?: error("Package '$name' was not found in the Termux repository")
            if (index > 0) emit("")
            emit(buildString {
                appendLine("Package: ${pkg.name}")
                appendLine("Version: ${pkg.version}")
                appendLine("Architecture: ${pkg.architecture}")
                appendLine("Installed: ${installed[name]?.version ?: "no"}")
                appendLine("Download size: ${formatBytes(pkg.size)}")
                if (pkg.preDepends.isNotEmpty()) appendLine("Pre-Depends: ${formatRelations(pkg.preDepends)}")
                if (pkg.depends.isNotEmpty()) appendLine("Depends: ${formatRelations(pkg.depends)}")
                if (pkg.provides.isNotEmpty()) appendLine("Provides: ${pkg.provides.joinToString(", ")}")
                if (pkg.conflicts.isNotEmpty()) appendLine("Conflicts: ${pkg.conflicts.joinToString(", ")}")
                if (pkg.breaks.isNotEmpty()) appendLine("Breaks: ${pkg.breaks.joinToString(", ")}")
                if (pkg.replaces.isNotEmpty()) appendLine("Replaces: ${pkg.replaces.joinToString(", ")}")
                append("Description: ${pkg.description}")
            })
        }
    }

    private suspend fun remove(names: List<String>, purge: Boolean, emit: suspend (String) -> Unit) {
        recoverInterruptedTransactions(emit)
        val installed = readInstalled()
        val present = names.filter { it in installed }
        val missing = names.filterNot { it in installed }
        if (missing.isNotEmpty()) emit("Not installed: ${missing.joinToString(" ")}")
        if (present.isEmpty()) return

        if (!bash.canExecute()) error("HG2Gui Bash is unavailable at ${bash.absolutePath}")
        emit("${if (purge) "Purging" else "Removing"}: ${present.joinToString(" ")}")

        val transaction = PackageTransactionGroup(context)
        try {
            for (name in present) {
                transaction.begin(name)
                val oldScripts = stageInstalledScripts(name)
                try {
                    runMaintainerScript(File(oldScripts, "prerm"), name, installed.getValue(name).version, listOf("remove"), emit)
                    assertDpkgHasNoMaintainerScripts(name)
                    runDpkg(listOf("--remove", name), emit)
                    runMaintainerScript(File(oldScripts, "postrm"), name, installed.getValue(name).version, listOf("remove"), emit)
                    if (purge) runDpkg(listOf("--purge", name), emit)
                    oldScripts.deleteRecursively()
                } catch (t: Throwable) {
                    restoreInstalledScripts(name, oldScripts)
                    throw t
                }
            }
            transaction.commit()
        } catch (t: Throwable) {
            throw rollbackFailure("package removal", transaction, t, emit)
        }
        emit("Done: ${present.joinToString(" ")}")
    }

    private suspend fun install(requested: List<String>, forceRequested: Boolean, emit: suspend (String) -> Unit) {
        recoverInterruptedTransactions(emit)
        ensureIndex(emit)
        val available = parsePackages(packagesFile.readText()).associateBy { it.name }
        val installed = readInstalled()
        val plan = resolve(requested, available, installed, forceRequested)
        if (plan.isEmpty()) {
            emit("Already satisfied: ${requested.joinToString(" ")}")
            return
        }

        if (!dpkgLauncher.canExecute()) error("HG2Gui dpkg launcher is unavailable in ${context.applicationInfo.nativeLibraryDir}")
        if (!bash.canExecute()) error("HG2Gui Bash is unavailable at ${bash.absolutePath}")

        val total = plan.sumOf { it.size.coerceAtLeast(0L) }
        emit("HG2Gui resolved ${plan.size} package${if (plan.size == 1) "" else "s"} (${formatBytes(total)}).")
        emit(plan.joinToString("\n") { "  ${it.name} ${it.version}" })

        val prepared = ArrayList<PreparedPackage>(plan.size)
        var overallDone = 0L
        for (pkg in plan) {
            emit("Downloading ${pkg.name} ${pkg.version}…")
            val output = File(cacheDir, "${pkg.name}_${pkg.version}_${pkg.architecture}.deb".replace('/', '_'))
            val result = repository.downloadVerifiedPackage(
                downloader = downloader,
                filename = pkg.filename,
                target = output,
                expectedSha256 = pkg.sha256,
                preferredMirror = activeMirror
            ) { packageDone, _ ->
                val combined = overallDone + packageDone
                val percent = if (total > 0L) ((combined * 100L) / total).coerceIn(0L, 100L) else 0L
                emit("$percent% [${pkg.name} ${formatBytes(packageDone)}/${formatBytes(pkg.size)}]")
            }
            emit("Preparing ${pkg.name} for HG2Gui prefix…")
            prepared += prepareArchive(result.file, pkg, emit)
            overallDone += pkg.size.coerceAtLeast(0L)
        }

        emit("Installing ${prepared.size} verified package archive${if (prepared.size == 1) "" else "s"}…")
        val transaction = PackageTransactionGroup(context)
        try {
            for (item in prepared) {
                transaction.begin(item.pkg.name)
                installPreparedPackage(item, installed[item.pkg.name], emit)
            }
            transaction.commit()
        } catch (t: Throwable) {
            throw rollbackFailure("package installation", transaction, t, emit)
        }
        emit("Installed: ${requested.joinToString(" ")}")
    }

    private suspend fun recoverInterruptedTransactions(emit: suspend (String) -> Unit) {
        val recovery = PackageTransactionJournal.recoverAbandoned(context)
        if (recovery.isEmpty()) return
        recovery.forEach { emit(it) }
        val failed = recovery.filter { it.startsWith("Could not recover") }
        if (failed.isNotEmpty()) {
            error("HG2Gui cannot safely continue package mutation until interrupted transaction recovery succeeds:\n${failed.joinToString("\n")}")
        }
    }

    private suspend fun rollbackFailure(
        operation: String,
        transaction: PackageTransactionGroup,
        cause: Throwable,
        emit: suspend (String) -> Unit
    ): Throwable {
        emit("$operation failed; rolling back the complete package plan…")
        val failures = transaction.rollback()
        return if (failures.isEmpty()) {
            emit("Rollback complete. Package payload and dpkg metadata were restored.")
            cause
        } else {
            val detail = failures.joinToString("\n")
            emit("Rollback was incomplete:\n$detail")
            IllegalStateException("${cause.message ?: operation} (rollback incomplete: $detail)", cause)
        }
    }

    private suspend fun installPreparedPackage(
        prepared: PreparedPackage,
        old: Installed?,
        emit: suspend (String) -> Unit
    ) {
        val pkg = prepared.pkg
        val oldScripts = stageInstalledScripts(pkg.name)
        try {
            if (old != null) {
                runMaintainerScript(File(oldScripts, "prerm"), pkg.name, old.version, listOf("upgrade", pkg.version), emit)
                runMaintainerScript(File(prepared.scriptsDir, "preinst"), pkg.name, pkg.version, listOf("upgrade", old.version), emit)
            } else {
                runMaintainerScript(File(prepared.scriptsDir, "preinst"), pkg.name, pkg.version, listOf("install"), emit)
            }

            assertDpkgHasNoMaintainerScripts(pkg.name)
            runDpkg(listOf("--unpack", prepared.archive.absolutePath), emit)

            if (old != null) {
                runMaintainerScript(File(oldScripts, "postrm"), pkg.name, old.version, listOf("upgrade", pkg.version), emit)
            }

            val postinstArgs = if (old != null) listOf("configure", old.version) else listOf("configure")
            runMaintainerScript(File(prepared.scriptsDir, "postinst"), pkg.name, pkg.version, postinstArgs, emit)
            assertDpkgHasNoMaintainerScripts(pkg.name)
            runDpkg(listOf("--configure", pkg.name), emit)
            installStoredScripts(pkg.name, prepared.scriptsDir)
            oldScripts.deleteRecursively()
        } catch (t: Throwable) {
            restoreInstalledScripts(pkg.name, oldScripts)
            throw t
        }
    }

    private suspend fun prepareArchive(
        archive: File,
        pkg: PackageRecord,
        emit: suspend (String) -> Unit
    ): PreparedPackage {
        if (!dpkgDeb.canExecute()) error("HG2Gui dpkg-deb is unavailable at ${dpkgDeb.absolutePath}")

        val safeStem = "${pkg.name}_${pkg.version}_${pkg.architecture}".replace(Regex("[^A-Za-z0-9._-]"), "_")
        val workDir = File(cacheDir, "prepare-$safeStem")
        val patched = File(cacheDir, "$safeStem.hg2.deb")
        val scriptsDir = File(cacheDir, "scripts-$safeStem")
        workDir.deleteRecursively()
        scriptsDir.deleteRecursively()
        patched.delete()
        workDir.mkdirs()
        scriptsDir.mkdirs()

        runTool(listOf(dpkgDeb.absolutePath, "-R", archive.absolutePath, workDir.absolutePath), "dpkg-deb extract")

        val relocated = relocateTermuxPayload(workDir)
        if (relocated > 0) {
            emit("Relocated $relocated top-level payload item${if (relocated == 1) "" else "s"} for ${pkg.name}.")
        }
        val symlinksRewritten = rewriteRelocatedSymlinks(workDir)

        val metadataRewritten = rewriteRelocatedControlMetadata(workDir)
        var rewritten = 0
        workDir.walkTopDown().filter { it.isFile && !Files.isSymbolicLink(it.toPath()) }.forEach { file ->
            if (!isRelocatedPathMetadata(workDir, file) && rewriteTextPrefix(file)) rewritten++
        }

        val extractedScripts = extractMaintainerScripts(workDir, scriptsDir)
        assertArchiveControlHasNoMaintainerScripts(workDir, pkg.name)
        val rewriteTotal = rewritten + metadataRewritten + symlinksRewritten
        emit("Rewrote $rewriteTotal file/path reference${if (rewriteTotal == 1) "" else "s"} and externalized $extractedScripts maintainer script${if (extractedScripts == 1) "" else "s"} for ${pkg.name}.")

        runTool(listOf(dpkgDeb.absolutePath, "-b", workDir.absolutePath, patched.absolutePath), "dpkg-deb build")
        if (!patched.isFile || patched.length() == 0L) error("Failed to rebuild ${pkg.name}")
        workDir.deleteRecursively()
        return PreparedPackage(pkg, patched, scriptsDir)
    }

    private fun extractMaintainerScripts(workDir: File, scriptsDir: File): Int {
        val debianDir = File(workDir, "DEBIAN")
        var count = 0
        for (name in MAINTAINER_SCRIPTS) {
            val source = File(debianDir, name)
            if (!source.isFile) continue
            moveFileVerified(source, File(scriptsDir, name), "externalize $name")
            count++
        }
        return count
    }

    private fun stageInstalledScripts(packageName: String): File {
        val safeName = packageName.replace(Regex("[^A-Za-z0-9._+-]"), "_")
        val dir = File(cacheDir, "installed-scripts-$safeName-${System.nanoTime()}")
        dir.mkdirs()
        for (name in MAINTAINER_SCRIPTS) {
            val source = File(dpkgInfoDir, "$packageName.$name")
            if (!source.isFile) continue
            moveFileVerified(source, File(dir, name), "stage $packageName.$name")
        }
        assertDpkgHasNoMaintainerScripts(packageName)
        return dir
    }

    private fun restoreInstalledScripts(packageName: String, scriptsDir: File) {
        if (!scriptsDir.isDirectory) return
        dpkgInfoDir.mkdirs()
        for (name in MAINTAINER_SCRIPTS) {
            val source = File(scriptsDir, name)
            if (source.isFile) source.copyTo(File(dpkgInfoDir, "$packageName.$name"), overwrite = true)
        }
    }

    private fun installStoredScripts(packageName: String, scriptsDir: File) {
        dpkgInfoDir.mkdirs()
        for (name in MAINTAINER_SCRIPTS) {
            val destination = File(dpkgInfoDir, "$packageName.$name")
            if (destination.exists() && !destination.delete()) {
                error("Cannot replace stored maintainer script ${destination.absolutePath}")
            }
            val source = File(scriptsDir, name)
            if (source.isFile) source.copyTo(destination, overwrite = true)
        }
        scriptsDir.deleteRecursively()
    }

    private fun moveFileVerified(source: File, destination: File, label: String) {
        destination.parentFile?.mkdirs()
        if (destination.exists() && !destination.delete()) {
            error("Cannot clear destination while trying to $label: ${destination.absolutePath}")
        }
        if (!source.renameTo(destination)) {
            source.copyTo(destination, overwrite = true)
            if (!source.delete()) {
                destination.delete()
                error("Cannot remove source while trying to $label: ${source.absolutePath}")
            }
        }
        if (source.exists()) error("Source still exists after $label: ${source.absolutePath}")
        if (!destination.isFile) error("Destination missing after $label: ${destination.absolutePath}")
    }

    private fun assertArchiveControlHasNoMaintainerScripts(workDir: File, packageName: String) {
        val debianDir = File(workDir, "DEBIAN")
        val remaining = MAINTAINER_SCRIPTS.map { File(debianDir, it) }.filter { it.exists() }
        if (remaining.isNotEmpty()) {
            error("Failed to externalize maintainer scripts for $packageName: ${remaining.joinToString { it.name }}")
        }
    }

    private fun assertDpkgHasNoMaintainerScripts(packageName: String) {
        val remaining = MAINTAINER_SCRIPTS.map { File(dpkgInfoDir, "$packageName.$it") }.filter { it.exists() }
        if (remaining.isNotEmpty()) {
            error("Failed to stage installed maintainer scripts for $packageName: ${remaining.joinToString { it.name }}")
        }
    }

    private suspend fun runMaintainerScript(
        script: File,
        packageName: String,
        packageVersion: String,
        args: List<String>,
        emit: suspend (String) -> Unit
    ) {
        if (!script.isFile) return
        rewriteTextPrefix(script)
        val interpreter = maintainerInterpreter(script)
        val process = ProcessBuilder(listOf(interpreter.absolutePath, script.absolutePath) + args)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply {
                applyPackageEnvironment(environment())
                environment()["DPKG_MAINTSCRIPT_NAME"] = script.name.substringAfterLast('.')
                environment()["DPKG_MAINTSCRIPT_PACKAGE"] = packageName
                environment()["DPKG_MAINTSCRIPT_PACKAGE_REFCOUNT"] = "1"
                environment()["DPKG_MAINTSCRIPT_ARCH"] = packageArchitecture()
                environment()["DPKG_MAINTSCRIPT_VERSION"] = packageVersion
            }
            .start()

        val tail = ArrayDeque<String>(DPKG_ERROR_TAIL_LINES)
        process.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (tail.size == DPKG_ERROR_TAIL_LINES) tail.removeFirst()
                tail.addLast(line)
                emit(line)
            }
        }
        val code = process.waitFor()
        if (code != 0) {
            val detail = tail.joinToString("\n").trim()
            error("${packageName}.${script.name} exited with code $code${if (detail.isBlank()) "" else ":\n$detail"}")
        }
    }

    private fun maintainerInterpreter(script: File): File {
        val shebang = runCatching { script.bufferedReader().use { it.readLine().orEmpty() } }.getOrDefault("")
        if (!shebang.startsWith("#!")) return bash
        val command = shebang.removePrefix("#!").trim()
        val words = command.split(Regex("\\s+")).filter(String::isNotBlank)
        val interpreterName = if (words.firstOrNull()?.substringAfterLast('/') == "env") {
            words.drop(1).firstOrNull { !it.startsWith("-") }?.substringAfterLast('/')
        } else {
            words.firstOrNull()?.substringAfterLast('/')
        }
        return when (interpreterName) {
            null, "", "sh", "bash", "dash" -> bash
            else -> error("Unsupported maintainer-script interpreter '$interpreterName' in ${script.name}; refusing to run it as Bash")
        }
    }

    private fun relocateTermuxPayload(workDir: File): Int {
        val sourceRoot = File(workDir, OLD_PREFIX.trimStart('/'))
        if (!sourceRoot.isDirectory) return 0

        val children = sourceRoot.listFiles().orEmpty()
        for (child in children) {
            val destination = File(workDir, child.name)
            if (destination.exists()) error("Cannot relocate package payload: ${destination.absolutePath} already exists")
            if (!child.renameTo(destination)) error("Cannot relocate package payload item ${child.absolutePath} to ${destination.absolutePath}")
        }

        var current: File? = sourceRoot
        while (current != null && current != workDir) {
            val parent = current.parentFile
            if (current.listFiles().isNullOrEmpty()) current.delete()
            current = parent
        }
        return children.size
    }

    private fun rewriteRelocatedSymlinks(workDir: File): Int {
        var rewritten = 0
        runCatching {
            Files.walk(workDir.toPath()).use { stream ->
                stream.filter(Files::isSymbolicLink).forEach { path ->
                    val target = runCatching { Files.readSymbolicLink(path).toString() }.getOrNull() ?: return@forEach
                    if (!target.startsWith(OLD_PREFIX)) return@forEach
                    val replacement = target.replaceFirst(OLD_PREFIX, prefix.absolutePath)
                    runCatching {
                        Files.delete(path)
                        Files.createSymbolicLink(path, Paths.get(replacement))
                        rewritten++
                    }.getOrElse { error("Could not rewrite relocated symlink $path → $target: ${it.message}") }
                }
            }
        }.getOrElse { error("Could not audit relocated symlinks: ${it.message}") }
        return rewritten
    }

    private fun rewriteRelocatedControlMetadata(workDir: File): Int {
        val debianDir = File(workDir, "DEBIAN")
        var rewritten = 0

        val conffiles = File(debianDir, "conffiles")
        if (conffiles.isFile) {
            val text = conffiles.readText()
            val updated = text.replace(OLD_PREFIX, "")
            if (updated != text) {
                conffiles.writeText(updated)
                rewritten++
            }
        }

        val md5sums = File(debianDir, "md5sums")
        if (md5sums.isFile) {
            val text = md5sums.readText()
            val oldRelativePrefix = OLD_PREFIX.trimStart('/') + "/"
            val updated = text.replace(oldRelativePrefix, "").replace(OLD_PREFIX, "")
            if (updated != text) {
                md5sums.writeText(updated)
                rewritten++
            }
        }

        return rewritten
    }

    private fun isRelocatedPathMetadata(workDir: File, file: File): Boolean {
        val debianDir = File(workDir, "DEBIAN")
        return file.parentFile == debianDir && file.name in setOf("conffiles", "md5sums")
    }

    private fun rewriteTextPrefix(file: File): Boolean {
        if (Files.isSymbolicLink(file.toPath())) return false
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        if (bytes.any { it == 0.toByte() }) return false
        val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull() ?: return false
        if (!text.contains(OLD_PREFIX)) return false
        return runCatching {
            file.writeText(text.replace(OLD_PREFIX, prefix.absolutePath))
            true
        }.getOrDefault(false)
    }

    private fun runTool(command: List<String>, label: String) {
        val process = ProcessBuilder(command)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        if (code != 0) {
            val detail = output.trim().takeLast(4000)
            error("$label exited with code $code${if (detail.isBlank()) "" else ":\n$detail"}")
        }
    }

    private suspend fun updateIndex(emit: suspend (String) -> Unit) {
        val architecture = packageArchitecture()
        emit("Authenticating Termux package index for $architecture…")
        val authenticated = repository.fetchAuthenticatedIndex(architecture, stateDir)
        val tmp = File(stateDir, "Packages.tmp")
        GZIPInputStream(authenticated.packagesGz.inputStream()).bufferedReader().use { reader ->
            tmp.writer().use { writer -> reader.copyTo(writer) }
        }
        if (!tmp.renameTo(packagesFile)) {
            tmp.copyTo(packagesFile, overwrite = true)
            tmp.delete()
        }
        activeMirror = authenticated.mirror
        emit("Authenticated repository: ${authenticated.mirror}")
        emit("HG2Gui package index ready: ${parsePackages(packagesFile.readText()).size} packages.")
    }

    private suspend fun ensureIndex(emit: suspend (String) -> Unit) {
        if (!packagesFile.isFile || packagesFile.length() == 0L) updateIndex(emit)
    }

    private fun resolve(
        requested: List<String>,
        available: Map<String, PackageRecord>,
        installed: Map<String, Installed>,
        forceRequested: Boolean
    ): List<PackageRecord> {
        val requestedSet = requested.toSet()
        val visiting = HashSet<String>()
        val planned = LinkedHashMap<String, PackageRecord>()
        val providers = buildMap<String, MutableList<PackageRecord>> {
            available.values.forEach { pkg ->
                pkg.provides.forEach { provided -> getOrPut(provided.name) { mutableListOf() }.add(pkg) }
            }
        }

        fun installedSatisfies(relation: Relation): Boolean = installed.values.any { item ->
            if (item.name == relation.name && versionSatisfies(item.version, relation)) return@any true
            item.provides.any { provided -> provided.name == relation.name && providedVersionSatisfies(provided, relation) }
        }

        fun plannedSatisfies(relation: Relation): Boolean = planned.values.any { item ->
            if (item.name == relation.name && versionSatisfies(item.version, relation)) return@any true
            item.provides.any { provided -> provided.name == relation.name && providedVersionSatisfies(provided, relation) }
        }

        fun candidateFor(relation: Relation): PackageRecord? {
            val exact = available[relation.name]
            if (exact != null && versionSatisfies(exact.version, relation)) return exact
            return providers[relation.name].orEmpty().firstOrNull { provider ->
                provider.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
            }
        }

        fun installedConflict(pkg: PackageRecord): String? {
            val relations = pkg.conflicts + pkg.breaks
            for (relation in relations) {
                val hit = installed.values.firstOrNull { item ->
                    item.name != pkg.name && (
                        (item.name == relation.name && versionSatisfies(item.version, relation)) ||
                            item.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                        )
                }
                if (hit != null) return "${pkg.name} ${if (relation in pkg.breaks) "breaks" else "conflicts with"} installed ${hit.name} (${relation})"
            }
            for (item in installed.values) {
                if (item.name == pkg.name) continue
                val reverse = (item.conflicts + item.breaks).firstOrNull { relation ->
                    (pkg.name == relation.name && versionSatisfies(pkg.version, relation)) ||
                        pkg.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                }
                if (reverse != null) return "installed ${item.name} conflicts with/breaks ${pkg.name} (${reverse})"
            }
            return null
        }

        fun plannedConflict(pkg: PackageRecord): String? {
            for (other in planned.values) {
                if (other.name == pkg.name) continue
                val forward = (pkg.conflicts + pkg.breaks).firstOrNull { relation ->
                    (other.name == relation.name && versionSatisfies(other.version, relation)) ||
                        other.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                }
                if (forward != null) return "${pkg.name} conflicts with/breaks planned ${other.name} ($forward)"
                val reverse = (other.conflicts + other.breaks).firstOrNull { relation ->
                    (pkg.name == relation.name && versionSatisfies(pkg.version, relation)) ||
                        pkg.provides.any { it.name == relation.name && providedVersionSatisfies(it, relation) }
                }
                if (reverse != null) return "planned ${other.name} conflicts with/breaks ${pkg.name} ($reverse)"
            }
            return null
        }

        lateinit var visitPackage: (PackageRecord) -> Unit

        fun visitRelation(alternatives: List<Relation>, owner: String, kind: String) {
            if (alternatives.any(::installedSatisfies) || alternatives.any(::plannedSatisfies)) return
            val candidate = alternatives.firstNotNullOfOrNull(::candidateFor)
                ?: error("$owner $kind unavailable alternative: ${alternatives.joinToString(" | ")}")
            visitPackage(candidate)
            if (!alternatives.any(::installedSatisfies) && !alternatives.any(::plannedSatisfies)) {
                error("$owner $kind was not satisfied after planning: ${alternatives.joinToString(" | ")}")
            }
        }

        visitPackage = fun(pkg: PackageRecord) {
            if (pkg.name in planned) return
            val forcedRequested = forceRequested && (
                pkg.name in requestedSet || requested.any { requestedName -> pkg.provides.any { it.name == requestedName } }
            )
            if (pkg.name in installed && !forcedRequested) return
            if (!visiting.add(pkg.name)) return
            installedConflict(pkg)?.let { error("Cannot install ${pkg.name}: $it. HG2Gui will not auto-remove conflicting packages.") }
            plannedConflict(pkg)?.let { error("Cannot install ${pkg.name}: $it") }
            pkg.preDepends.forEach { visitRelation(it, pkg.name, "pre-depends on") }
            pkg.depends.forEach { visitRelation(it, pkg.name, "depends on") }
            visiting.remove(pkg.name)
            planned[pkg.name] = pkg
        }

        fun visitRequested(name: String) {
            val relation = Relation(name.substringBefore(':'))
            if (installedSatisfies(relation) && !forceRequested) return
            val pkg = candidateFor(relation) ?: error("Package or provider '$name' was not found in the Termux repository")
            visitPackage(pkg)
        }

        requested.forEach(::visitRequested)
        return planned.values.toList()
    }

    private fun versionSatisfies(candidateVersion: String, relation: Relation): Boolean {
        val op = relation.operator ?: return true
        val wanted = relation.version ?: return true
        if (!dpkgLauncher.canExecute()) return false
        val process = ProcessBuilder(dpkgLauncher.absolutePath, "--compare-versions", candidateVersion, op, wanted)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()
        process.inputStream.close()
        return process.waitFor() == 0
    }

    private fun providedVersionSatisfies(provided: Relation, requested: Relation): Boolean {
        if (requested.operator == null) return true
        val providedVersion = provided.version ?: return false
        return versionSatisfies(providedVersion, requested)
    }

    private fun isVersionLessThan(installed: String, available: String): Boolean {
        if (installed == available) return false
        if (!dpkgLauncher.canExecute()) return false
        val process = ProcessBuilder(dpkgLauncher.absolutePath, "--compare-versions", installed, "lt", available)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()
        process.inputStream.close()
        return process.waitFor() == 0
    }

    private suspend fun runDpkg(args: List<String>, emit: suspend (String) -> Unit) {
        val process = ProcessBuilder(listOf(dpkgLauncher.absolutePath) + args)
            .directory(prefix)
            .redirectErrorStream(true)
            .apply { applyPackageEnvironment(environment()) }
            .start()

        val tail = ArrayDeque<String>(DPKG_ERROR_TAIL_LINES)
        process.inputStream.bufferedReader().use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                if (tail.size == DPKG_ERROR_TAIL_LINES) tail.removeFirst()
                tail.addLast(line)
                emit(line)
            }
        }
        val code = process.waitFor()
        if (code != 0) {
            val detail = tail.joinToString("\n").trim()
            error("dpkg exited with code $code${if (detail.isBlank()) "" else ":\n$detail"}")
        }
    }

    private fun applyPackageEnvironment(env: MutableMap<String, String>) {
        env["PREFIX"] = prefix.absolutePath
        env["HOME"] = DistroManager.homeDir(context).absolutePath
        env["PATH"] = "${prefix.absolutePath}/bin:/system/bin"
        env["LD_LIBRARY_PATH"] = "${prefix.absolutePath}/lib"
        env["TMPDIR"] = "${prefix.absolutePath}/tmp"
        env["DPKG_ROOT"] = prefix.absolutePath
        env["DPKG_ADMINDIR"] = File(prefix, "var/lib/dpkg").absolutePath
    }

    private fun readInstalled(): Map<String, Installed> {
        val status = File(prefix, "var/lib/dpkg/status")
        if (!status.isFile) return emptyMap()
        return status.readText().split("\n\n").mapNotNull { paragraph ->
            val fields = parseParagraph(paragraph)
            val name = fields["Package"] ?: return@mapNotNull null
            if (fields["Status"] != "install ok installed") return@mapNotNull null
            Installed(
                name = name,
                version = fields["Version"].orEmpty(),
                provides = parseRelationList(fields["Provides"].orEmpty()),
                conflicts = parseRelationList(fields["Conflicts"].orEmpty()),
                breaks = parseRelationList(fields["Breaks"].orEmpty())
            )
        }.associateBy { it.name }
    }

    private fun parsePackages(text: String): List<PackageRecord> = text.split("\n\n").mapNotNull { paragraph ->
        val fields = parseParagraph(paragraph)
        val name = fields["Package"] ?: return@mapNotNull null
        val filename = fields["Filename"] ?: return@mapNotNull null
        PackageRecord(
            name = name,
            version = fields["Version"].orEmpty(),
            architecture = fields["Architecture"].orEmpty(),
            filename = filename,
            sha256 = fields["SHA256"].orEmpty(),
            size = fields["Size"]?.toLongOrNull() ?: 0L,
            preDepends = parseRelationGroups(fields["Pre-Depends"].orEmpty()),
            depends = parseRelationGroups(fields["Depends"].orEmpty()),
            provides = parseRelationList(fields["Provides"].orEmpty()),
            conflicts = parseRelationList(fields["Conflicts"].orEmpty()),
            breaks = parseRelationList(fields["Breaks"].orEmpty()),
            replaces = parseRelationList(fields["Replaces"].orEmpty()),
            description = fields["Description"].orEmpty()
        )
    }

    private fun parseParagraph(paragraph: String): Map<String, String> {
        val fields = LinkedHashMap<String, String>()
        var current: String? = null
        for (line in paragraph.lineSequence()) {
            if (line.startsWith(' ') && current != null) {
                fields[current] = fields.getValue(current) + "\n" + line.trim()
                continue
            }
            val i = line.indexOf(':')
            if (i <= 0) continue
            current = line.substring(0, i)
            fields[current] = line.substring(i + 1).trim()
        }
        return fields
    }

    private fun parseRelationGroups(raw: String): List<List<Relation>> {
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { clause ->
            clause.split('|').mapNotNull(::parseRelation).takeIf { it.isNotEmpty() }
        }
    }

    private fun parseRelationList(raw: String): List<Relation> =
        if (raw.isBlank()) emptyList() else raw.split(',').mapNotNull(::parseRelation)

    private fun parseRelation(raw: String): Relation? {
        val withoutRestrictions = raw.replace(ARCH_RESTRICTION, "").replace(PROFILE_RESTRICTION, "").trim()
        val name = withoutRestrictions.substringBefore(' ').substringBefore(':').trim().takeIf(String::isNotBlank) ?: return null
        val match = VERSION_RELATION.find(withoutRestrictions)
        return Relation(
            name = name,
            operator = match?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank),
            version = match?.groupValues?.getOrNull(2)?.trim()?.takeIf(String::isNotBlank)
        )
    }

    private fun formatRelations(groups: List<List<Relation>>): String =
        groups.joinToString(", ") { it.joinToString(" | ") }

    private fun packageArchitecture(): String = when (android.os.Build.SUPPORTED_ABIS.firstOrNull()) {
        "arm64-v8a" -> "aarch64"
        "armeabi-v7a", "armeabi" -> "arm"
        "x86_64" -> "x86_64"
        "x86" -> "i686"
        else -> error("Unsupported Android ABI for Termux packages: ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun words(line: String): List<String> = Regex("""(?:[^\s\"']+|\"[^\"]*\"|'[^']*')+""")
        .findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GiB".format(Locale.US, bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.1f MiB".format(Locale.US, bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.1f KiB".format(Locale.US, bytes / 1024.0)
        else -> "$bytes B"
    }

    companion object {
        private const val OLD_PREFIX = "/data/data/com.termux/files/usr"
        private const val DPKG_ERROR_TAIL_LINES = 40
        private val MAINTAINER_SCRIPTS = listOf("preinst", "postinst", "prerm", "postrm")
        private val VERSION_RELATION = Regex("\\((<<|<=|=|>=|>>)\\s*([^)]+)\\)")
        private val ARCH_RESTRICTION = Regex("\\[[^]]*]")
        private val PROFILE_RESTRICTION = Regex("<[^>]*>")
        private const val USAGE = "HG2Gui package manager\nusage: pkg <install|update|upgrade|remove|purge|search|show|list-installed|clean> …"
    }
}
