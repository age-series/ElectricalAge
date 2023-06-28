package mods.eln.sim.process.heater;

import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;

public class ResistorHeatThermalLoad implements IProcess {

    Resistor resistor;
    ThermalLoad load;

    public ResistorHeatThermalLoad(Resistor r, ThermalLoad load) {
        this.resistor = r;
        this.load = load;
    }

    @Override
    public void process(double time) {
        load.movePowerTo(resistor.getPower());
    }
}
