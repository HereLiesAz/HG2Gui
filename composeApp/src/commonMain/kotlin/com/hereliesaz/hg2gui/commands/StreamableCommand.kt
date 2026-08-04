package com.hereliesaz.hg2gui.commands

import kotlinx.coroutines.flow.Flow

/**
 * Interface for commands that produce a continuous stream of output.
 */
interface StreamableCommand : CommandAbstraction {
    fun execStream(pack: ExecutePack): Flow<String>
}
