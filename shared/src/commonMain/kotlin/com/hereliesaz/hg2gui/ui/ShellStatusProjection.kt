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

/** Who and where the session's shell runs: the `user` and `host` of a `user@host` tag. */
data class ShellIdentity(val user: String, val host: String)

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

    /**
     * The identity question: answered with the user name on the first line and the host name on the
     * second (see [parseIdentity]). It never changes during a session, so it is asked at most once.
     * Like [GIT_STATUS_COMMAND] it is a question the [ShellProbe] recognises, not a command line
     * that is necessarily run through a shell; a probe that does not know it answers null.
     */
    const val IDENTITY_COMMAND = "id -un; hostname"

    /** Shown when HEAD is not on a branch. */
    const val DETACHED_LABEL = "detached"

    private const val PROBE_TIMEOUT_MS = 2_000L
    private const val MAX_IDENTITY_CHARS = 64
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
     * Parses the answer to [IDENTITY_COMMAND]: the first two non-blank lines, each one word with no
     * whitespace or control characters. Null when either is missing, so a half-known identity is
     * never shown as `user@`.
     */
    fun parseIdentity(output: String?): ShellIdentity? {
        val lines = output?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: return null
        if (lines.size < 2) return null
        val (user, host) = lines
        if (!isIdentityWord(user) || !isIdentityWord(host)) return null
        return ShellIdentity(user = user, host = host)
    }

    /** `user\nhost`, the shape [parseIdentity] reads; null when either part is unusable. */
    fun formatIdentity(user: String?, host: String?): String? {
        val u = user?.trim().orEmpty()
        val h = host?.trim().orEmpty()
        if (!isIdentityWord(u) || !isIdentityWord(h)) return null
        return "$u\n$h"
    }

    /**
     * Asks [probe] who this session is. Null when the probe cannot say, times out or fails; the
     * status line then simply omits user@host. Cancellation is propagated.
     */
    suspend fun probeIdentity(cwd: String, probe: ShellProbe): ShellIdentity? = try {
        parseIdentity(withTimeoutOrNull(PROBE_TIMEOUT_MS) { probe(cwd, IDENTITY_COMMAND) })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun isIdentityWord(word: String): Boolean =
        word.isNotEmpty() && word.length <= MAX_IDENTITY_CHARS && word.none { it.isWhitespace() || it.isISOControl() }

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

/**
 * Pure helpers behind the Activity's answer to [ShellStatusProjector.IDENTITY_COMMAND]: they pick the
 * best available user and host name from what the platform offers, in order, so the platform code
 * only gathers the raw values.
 */
object ShellIdentitySource {
    private const val USER_ID_RANGE = 100_000
    private const val FIRST_APP_ID = 10_000
    private const val LAST_APP_ID = 19_999

    /**
     * The name Android's own `id -un` prints for an app uid, e.g. 10123 -> `u0_a123`. Null for system
     * and isolated uids (the shell then has no such name and callers fall back to the environment).
     */
    fun androidAppUserName(uid: Int): String? {
        if (uid < 0) return null
        val appId = uid % USER_ID_RANGE
        if (appId !in FIRST_APP_ID..LAST_APP_ID) return null
        return "u${uid / USER_ID_RANGE}_a${appId - FIRST_APP_ID}"
    }

    /** The first usable user name: the app uid's name, then `$USER`, then the `user.name` property. */
    fun pickUser(uid: Int, envUser: String?, propertyUser: String?): String? =
        listOf(androidAppUserName(uid), envUser, propertyUser).firstNotNullOfOrNull { cleanWord(it) }

    /**
     * The first usable host name: the `hostname` command's output, then `$HOSTNAME`, then the device
     * model. Whitespace inside a name (a model like "Pixel 7") becomes a dash so it stays one word.
     */
    fun pickHost(hostnameOutput: String?, envHost: String?, deviceModel: String?): String? =
        listOf(hostnameOutput?.lineSequence()?.firstOrNull { it.isNotBlank() }, envHost, deviceModel)
            .firstNotNullOfOrNull { cleanWord(it) }

    private fun cleanWord(raw: String?): String? {
        val word = raw?.trim()?.replace(Regex("\\s+"), "-")?.filterNot { it.isISOControl() }.orEmpty()
        return word.takeIf { it.isNotEmpty() }
    }
}
