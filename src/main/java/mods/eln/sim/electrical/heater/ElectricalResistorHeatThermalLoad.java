package mods.eln.sim.electrical.heater;

import mods.eln.sim.IProcess;
import mods.eln.sim.electrical.mna.component.Resistor;
import mods.eln.sim.thermal.ThermalLoad;

public class ElectricalResistorHeatThermalLoad implements IProcess {

    Resistor electricalResistor;
    ThermalLoad thermalLoad;

    public ElectricalResistorHeatThermalLoad(Resistor electricalResistor, ThermalLoad thermalLoad) {
        this.electricalResistor = electricalResistor;
        this.thermalLoad = thermalLoad;
    }

    @Override
    public void process(double time) {
        thermalLoad.PcTemp += electricalResistor.getP();
    }
}
