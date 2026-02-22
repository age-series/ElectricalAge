package mods.eln.sim.process.destruct

import mods.eln.sim.mna.component.Resistor

class ResistorPowerWatchdog(var resistor: Resistor) : ValueWatchdog() {

    fun setMaximumPower(maximumPower: Double): ResistorPowerWatchdog {
        max = maximumPower
        min = -1.0
        timeoutReset = maximumPower
        return this
    }

    override fun getValue(): Double {
        return resistor.power
    }
}
