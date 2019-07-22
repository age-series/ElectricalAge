package mods.eln.sim.thermal

import mods.eln.sim.core.IProcess
import mods.eln.sim.thermal.ThermalLoad
import mods.eln.sim.mna.passive.Resistor

class DiodeHeatThermalLoad(internal var r: Resistor, internal var load: ThermalLoad) : IProcess {
    internal var lastR: Double = 0.toDouble()

    init {
        lastR = r.r
    }

    override fun process(time: Double) {
        if (r.r == lastR) {
            load.movePowerTo(r.getPower())
        } else {
            lastR = r.r
        }
    }
}
