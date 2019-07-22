package mods.eln.sim.process.heater

import mods.eln.sim.core.IProcess
import mods.eln.sim.thermal.ThermalLoad
import mods.eln.sim.mna.passive.Resistor

class ResistorHeatThermalLoad(internal var r: Resistor, internal var load: ThermalLoad) : IProcess {

    override fun process(time: Double) {
        load.movePowerTo(r.getPower())
    }
}
