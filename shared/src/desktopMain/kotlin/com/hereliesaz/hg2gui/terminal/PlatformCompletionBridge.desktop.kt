package com.hereliesaz.hg2gui.terminal

actual object PlatformCompletionBridge {
    actual suspend fun complete(request: CompletionRequest): List<CompletionCandidate> = emptyList()
}
