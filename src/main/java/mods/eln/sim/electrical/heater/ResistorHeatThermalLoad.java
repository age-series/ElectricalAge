package mods.eln.sim.electrical.heater;

import mods.eln.sim.IProcess;
import mods.eln.sim.thermal.ThermalLoad;
import mods.eln.sim.electrical.mna.component.Resistor;

public class ResistorHeatThermalLoad implements IProcess {

    Resistor r;
    ThermalLoad load;

    public ResistorHeatThermalLoad(Resistor r, ThermalLoad load) {
        this.r = r;
        this.load = load;
    }

    @Override
    public void process(double time) {
        load.movePowerTo(r.getP());
    }
}
