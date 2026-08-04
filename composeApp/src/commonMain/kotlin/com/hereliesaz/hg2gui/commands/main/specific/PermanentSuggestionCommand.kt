package com.hereliesaz.hg2gui.commands.main.specific

import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.commands.ExecutePack

abstract class PermanentSuggestionCommand : CommandAbstraction {
    abstract fun permanentSuggestions(): Array<String>

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null
    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
