package mods.eln.sim.process.heater;

import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;

public class ElectricalLoadHeatThermalLoad implements IProcess {

    ElectricalLoad resistor;
    ThermalLoad load;

    public ElectricalLoadHeatThermalLoad(ElectricalLoad r, ThermalLoad load) {
        this.resistor = r;
        this.load = load;
    }

    @Override
    public void process(double time) {
        if (resistor.isNotSimulated()) return;
        double current = resistor.getCurrent();
        load.movePowerTo(current * current * resistor.getSerialResistance() * 2);
    }

	/*double powerMax = 100000;
    public void setDeltaTPerSecondMax(double deltaTPerSecondMax) {
		powerMax = deltaTPerSecondMax*load.C;
	}*/
}
