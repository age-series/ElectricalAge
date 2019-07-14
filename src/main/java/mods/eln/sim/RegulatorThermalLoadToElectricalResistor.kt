package mods.eln.sim

import mods.eln.sim.mna.component.Resistor

class RegulatorThermalLoadToElectricalResistor(name: String, internal var thermalLoad: ThermalLoad, internal var electricalResistor: Resistor) : RegulatorProcess(name) {

    var Rmin: Double = 0.0

    override fun getHit(): Double {
        return thermalLoad.Tc
    }

    override fun setCmd(cmd: Double) {
        if (cmd <= 0.001) {
            electricalResistor.highImpedance()
        } else if (cmd >= 1.0) {
            electricalResistor.r = Rmin
        } else {
            electricalResistor.r = Rmin / cmd
        }
    }
}
