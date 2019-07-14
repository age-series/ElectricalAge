package mods.eln.sim.process.destruct

import mods.eln.sim.mna.component.Resistor

class ResistorPowerWatchdog(internal val resistor: Resistor) : ValueWatchdog() {

    fun setPmax(Pmax: Double) {
        this.max = Pmax
        this.min = -1.0
        this.timeoutReset = Pmax * 0.20 * 5.0
    }

    override fun getValue(): Double? = resistor.getPower()

}
