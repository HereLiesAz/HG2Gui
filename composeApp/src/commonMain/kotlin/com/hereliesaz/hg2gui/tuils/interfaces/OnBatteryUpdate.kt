package com.hereliesaz.hg2gui.tuils.interfaces

/**
 * Created by francescoandreuzzi on 04/08/2017.
 */
interface OnBatteryUpdate {
    fun update(percentage: Float)
    fun onCharging()
    fun onNotCharging()
}
