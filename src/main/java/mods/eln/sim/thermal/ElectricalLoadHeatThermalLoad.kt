package mods.eln.sim.thermal

import mods.eln.sim.mna.state.ElectricalLoad
import mods.eln.sim.core.IProcess
import mods.eln.sim.thermal.ThermalLoad

class ElectricalLoadHeatThermalLoad(internal var r: ElectricalLoad, internal var load: ThermalLoad) : IProcess {

    override fun process(time: Double) {
        if (r.isNotSimulated()) return
        val I = r.i
        load.movePowerTo(I * I * r.rs * 2.0)
    }
}
