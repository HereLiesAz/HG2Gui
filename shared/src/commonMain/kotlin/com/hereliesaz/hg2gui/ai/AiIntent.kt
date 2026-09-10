package com.hereliesaz.hg2gui.ai

enum class AiIntentKind { COMMAND, PACKAGE_MUTATION, PACKAGE_POLICY, AUTHORITY_REQUEST, REMOTE_COMMAND }

data class AiIntent(
    val kind: AiIntentKind,
    val command: String,
    val authority: String = "app",
    val consequences: List<String> = emptyList()
)

/** Deterministic interpretation of a suggested command. The model never decides its own authority. */
object AiIntentAnalyzer {
    fun analyze(command: String): AiIntent {
        val normalized = command.trim()
        val words = shellWords(normalized)
        val first = words.firstOrNull().orEmpty()
        val second = words.getOrNull(1).orEmpty()
        return when {
            first == "hg2auth" -> AiIntent(
                kind = AiIntentKind.AUTHORITY_REQUEST,
                command = normalized,
                authority = when (second) { "adb" -> "adb"; "root" -> "root"; else -> "explicit" },
                consequences = listOf(
                    "Requests elevated authority explicitly; it is not inherited by ordinary commands.",
                    "Foreground approval remains required before elevated execution."
                )
            )
            first == "hg2package" -> AiIntent(
                kind = AiIntentKind.PACKAGE_POLICY,
                command = normalized,
                consequences = lifecycleConsequences(second)
            )
            first in setOf("pkg", "apt", "apt-get", "dpkg", "pip", "pipx", "npm", "gem") &&
                words.any { it in PACKAGE_MUTATIONS } -> AiIntent(
                kind = AiIntentKind.PACKAGE_MUTATION,
                command = normalized,
                consequences = listOf("Changes installed package state and may add, update, or remove dependencies.")
            )
            first == "ssh" -> AiIntent(
                kind = AiIntentKind.REMOTE_COMMAND,
                command = normalized,
                consequences = listOf("Runs against a remote SSH target; local package/isolation state does not describe the remote host.")
            )
            else -> AiIntent(AiIntentKind.COMMAND, normalized)
        }
    }

    private fun lifecycleConsequences(action: String): List<String> = when (action) {
        "isolate" -> listOf("Future runs use a private PRoot runtime and private HOME/XDG state.", "ADB/root authority is not inherited by the isolated package.")
        "release" -> listOf("Deletes the package's private sandbox state and returns future runs to the normal package backend.")
        "reset" -> listOf("Deletes HG2Gui-managed runtime state while keeping the package installed.")
        "snapshot" -> listOf("Creates a bounded restore point from HG2Gui-managed runtime state.")
        "restore" -> listOf("Replaces current managed runtime state with a selected restore point.")
        "remove", "purge" -> listOf("Removes installed package state; purge may also remove manager-owned configuration.")
        "disable" -> listOf("Keeps the package installed but blocks HG2Gui from launching its owned commands.")
        else -> emptyList()
    }

    private fun shellWords(line: String): List<String> = Regex("""(?:[^\s\"']+|\"[^\"]*\"|'[^']*')+""")
        .findAll(line)
        .map { it.value.trim().removeSurrounding("\"").removeSurrounding("'") }
        .toList()

    private val PACKAGE_MUTATIONS = setOf(
        "install", "uninstall", "remove", "purge", "update", "upgrade", "autoremove", "-i", "-r"
    )
}
