package mods.eln.sim.process.destruct

import mods.eln.sim.mna.component.Resistor
import kotlin.math.abs

class ResistorCurrentWatchdog(private val resistor: Resistor) : ValueWatchdog() {
    override val watchdogType = WatchdogType.CURRENT

    fun setMaximumCurrent(maximumCurrent: Double, spikeGraceSeconds: Double = 0.25): ResistorCurrentWatchdog {
        max = maximumCurrent
        min = -1.0
        timeoutReset = spikeGraceSeconds
        return this
    }

    override fun getValue(): Double = abs(resistor.current)
}
