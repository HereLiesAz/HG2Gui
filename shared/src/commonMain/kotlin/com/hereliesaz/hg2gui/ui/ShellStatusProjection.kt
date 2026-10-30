package com.hereliesaz.hg2gui.ui

import com.hereliesaz.hg2gui.terminal.ShellPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs one short, read-only shell command in [cwd] and returns its stdout, or null when it could
 * not run or failed (not a repository, git missing, timed out). The Activity supplies the real
 * implementation (it owns the shell runner); the UI layer only describes what to ask.
 */
typealias ShellProbe = suspend (cwd: String, command: String) -> String?

/** The default probe: nothing is available, so no repository state is shown. */
val NoShellProbe: ShellProbe = { _, _ -> null }

/** Branch and cleanliness of the repository containing a directory. */
data class GitState(val branch: String, val dirty: Boolean)

/**
 * Projects what a shell prompt framework would normally encode into decorated prompt text (cwd,
 * git branch and dirty flag, last exit status) into the [ShellPresentation] the composer shows and
 * hands to the completion bridge. The prompt itself stays the source of truth for interactive
 * shells (see FullScreenTerminalScreen); this covers the ordinary command-composer path, which has
 * no prompt to parse.
 *
 * Git probing is deliberately a lambda ([ShellProbe]) so it is testable and so platforms without a
 * shell (the desktop preview) simply show nothing.
 */
object ShellStatusProjector {
    /** One command answers both questions: `## branch...upstream` header plus one line per change. */
    const val GIT_STATUS_COMMAND = "git --no-optional-locks status --porcelain=v1 --branch"

    /** Shown when HEAD is not on a branch. */
    const val DETACHED_LABEL = "detached"

    private const val PROBE_TIMEOUT_MS = 2_000L
    private const val HEADER_PREFIX = "## "
    private const val NO_COMMITS_PREFIX = "No commits yet on "
    private const val INITIAL_COMMIT_PREFIX = "Initial commit on "
    private const val DETACHED_HEADER = "HEAD (no branch)"

    /** Parses `git status --porcelain=v1 --branch` output; null when it is not such output. */
    fun parseGitStatus(output: String?): GitState? {
        val lines = output?.lines()?.filter { it.isNotBlank() } ?: return null
        val header = lines.firstOrNull()?.takeIf { it.startsWith(HEADER_PREFIX) } ?: return null
        val name = header.removePrefix(HEADER_PREFIX).trim()
        val branch = when {
            name.startsWith(NO_COMMITS_PREFIX) -> name.removePrefix(NO_COMMITS_PREFIX).trim()
            name.startsWith(INITIAL_COMMIT_PREFIX) -> name.removePrefix(INITIAL_COMMIT_PREFIX).trim()
            name.startsWith(DETACHED_HEADER) -> DETACHED_LABEL
            else -> name.substringBefore("...").substringBefore(' ')
        }
        if (branch.isEmpty()) return null
        return GitState(branch = branch, dirty = lines.size > 1)
    }

    /**
     * [previous] updated for a command that just finished: the current [cwd], the repository state
     * the [probe] reports for it (cleared when it reports none), and [lastExitCode]. Fields the
     * composer cannot know (user, host, shell family) are carried over, so a caller that does know
     * them (an SSH session, a cooperative shell) keeps them.
     */
    suspend fun project(
        previous: ShellPresentation,
        cwd: String,
        lastExitCode: Int?,
        probe: ShellProbe
    ): ShellPresentation {
        val git = if (cwd.isBlank()) null else probeGit(cwd, probe)
        return previous.copy(
            cwd = cwd,
            gitBranch = git?.branch,
            gitDirty = git?.dirty ?: false,
            lastExitCode = lastExitCode
        )
    }

    private suspend fun probeGit(cwd: String, probe: ShellProbe): GitState? = try {
        parseGitStatus(withTimeoutOrNull(PROBE_TIMEOUT_MS) { probe(cwd, GIT_STATUS_COMMAND) })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // The status line is advisory: a probe that blows up must not break the command that just ran.
        null
    }
}
