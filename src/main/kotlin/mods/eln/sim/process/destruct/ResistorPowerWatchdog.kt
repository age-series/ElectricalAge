package mods.eln.sim.process.destruct

import mods.eln.sim.mna.component.Resistor

class ResistorPowerWatchdog(var resistor: Resistor) : ValueWatchdog() {

    fun setMaximumPower(maximumPower: Double): ResistorPowerWatchdog {
        max = maximumPower
        min = -1.0
        // TODO: Abstract 0.2 as step time or seconds?
        timeoutReset = maximumPower * 0.20 * 5
        return this
    }

    override fun getValue(): Double {
        return resistor.power
    }
}
