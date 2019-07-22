package mods.eln.sim.thermal

import mods.eln.sim.core.IProcess
import mods.eln.sim.mna.passive.Resistor

class ElectricalResistorHeatThermalLoad(internal var electricalResistor: Resistor, internal var thermalLoad: ThermalLoad) : IProcess {

    override fun process(time: Double) {
        thermalLoad.PcTemp += electricalResistor.getPower()
    }
}
