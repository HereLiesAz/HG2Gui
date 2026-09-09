package com.hereliesaz.hg2gui.terminal

/** Platform completion service consumed by common command-composition UI. */
expect object PlatformCompletionBridge {
    suspend fun complete(request: CompletionRequest): List<CompletionCandidate>
}
