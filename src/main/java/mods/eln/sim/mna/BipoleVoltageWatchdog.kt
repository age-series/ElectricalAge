package mods.eln.sim.mna

import mods.eln.sim.core.ValueWatchdog
import mods.eln.sim.mna.passive.Bipole

class BipoleVoltageWatchdog(internal val bipole: Bipole) : ValueWatchdog() {

    fun setUNominal(UNominal: Double) {
        this.max = UNominal * 1.3
        this.min = -max
        this.timeoutReset = UNominal * 0.10 * 5.0
    }

    override fun getValue(): Double {
        return bipole.getVoltage()
    }
}
