package mods.eln.sim.process.heater

import mods.eln.sim.mna.state.ElectricalLoad
import mods.eln.sim.core.IProcess
import mods.eln.sim.thermal.ThermalLoad

class ElectricalLoadHeatThermalLoad(internal var r: ElectricalLoad, internal var load: ThermalLoad) : IProcess {

    override fun process(time: Double) {
        if (r.isNotSimulated()) return
        val I = r.i
        load.movePowerTo(I * I * r.rs * 2.0)
    }

    /*double powerMax = 100000;
    public void setDeltaTPerSecondMax(double deltaTPerSecondMax) {
		powerMax = deltaTPerSecondMax*load.C;
	}*/
}
