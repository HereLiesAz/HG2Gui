package com.hereliesaz.hg2gui.terminal

import com.hereliesaz.hg2gui.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual object PlatformCompletionBridge {
    actual suspend fun complete(request: CompletionRequest): List<CompletionCandidate> {
        val context = Utils.applicationContextOrNull() ?: return emptyList()
        return withContext(Dispatchers.IO) {
            ShellCompletionBridge.complete(context, request)
        }
    }
}
