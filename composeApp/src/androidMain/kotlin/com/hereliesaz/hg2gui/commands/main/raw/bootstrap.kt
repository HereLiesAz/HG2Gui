package com.hereliesaz.hg2gui.commands.main.raw

import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.commands.ExecutePack
import com.hereliesaz.hg2gui.commands.StreamableCommand
import com.hereliesaz.hg2gui.commands.main.MainPack
import com.hereliesaz.hg2gui.terminal.DistroManager
import com.hereliesaz.hg2gui.tuils.Tuils
import kotlinx.coroutines.flow.Flow

class bootstrap : StreamableCommand {

    override fun exec(pack: ExecutePack): String? {
        // This won't be called if we use the flow-based execution
        return "Bootstrap initiated..."
    }
    
    override fun execStream(pack: ExecutePack): Flow<String> {
        val mPack = pack as MainPack
        return DistroManager.bootstrap(mPack.androidContext, mPack.client)
    }

    override fun argType(): IntArray = intArrayOf()

    override fun priority(): Int = 10

    override fun helpRes(): Int = 0 // Needs a string resource

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
