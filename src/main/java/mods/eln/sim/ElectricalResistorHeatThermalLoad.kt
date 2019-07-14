package mods.eln.sim

import mods.eln.sim.mna.component.Resistor

class ElectricalResistorHeatThermalLoad(internal var electricalResistor: Resistor, internal var thermalLoad: ThermalLoad) : IProcess {

    override fun process(time: Double) {
        thermalLoad.PcTemp += electricalResistor.getPower()
    }
}
