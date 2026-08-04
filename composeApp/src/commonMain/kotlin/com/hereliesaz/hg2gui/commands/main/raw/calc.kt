package com.hereliesaz.hg2gui.commands.main.raw

import com.hereliesaz.hg2gui.commands.CommandAbstraction
import com.hereliesaz.hg2gui.commands.ExecutePack
import com.hereliesaz.hg2gui.tuils.CalculationEngine

class calc : CommandAbstraction {
    override fun exec(pack: ExecutePack): String? {
        val expression = pack.getString() ?: return null
        return try {
            CalculationEngine.eval(expression).toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    override fun argType(): IntArray = intArrayOf(CommandAbstraction.PLAIN_TEXT)

    override fun priority(): Int = 5

    override fun helpRes(): Int = 0 

    override fun onArgNotFound(pack: ExecutePack, indexNotFound: Int): String? = null

    override fun onNotArgEnough(pack: ExecutePack, nArgs: Int): String? = null
}
