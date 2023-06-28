package mods.eln.sim.process.heater;

import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;

public class DiodeHeatThermalLoad implements IProcess {

    Resistor resistor;
    ThermalLoad load;
    double lastResistance;

    public DiodeHeatThermalLoad(Resistor r, ThermalLoad load) {
        this.resistor = r;
        this.load = load;
        lastResistance = r.getResistance();
    }

    @Override
    public void process(double time) {
        if (resistor.getResistance() == lastResistance) {
            load.movePowerTo(resistor.getPower());
        } else {
            lastResistance = resistor.getResistance();
        }
    }
}
