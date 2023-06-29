package mods.eln.sim;

import mods.eln.sim.mna.component.Resistor;

public class RegulatorThermalLoadToElectricalResistor extends RegulatorProcess {

    ThermalLoad thermalLoad;
    Resistor electricalResistor;

    double minimumResistance;

    public void setMinimumResistance(double minimumResistance) {
        this.minimumResistance = minimumResistance;
    }

    public RegulatorThermalLoadToElectricalResistor(String name, ThermalLoad thermalLoad, Resistor electricalResistor) {
        super(name);
        this.thermalLoad = thermalLoad;
        this.electricalResistor = electricalResistor;
    }

    @Override
    protected double getHit() {
        return thermalLoad.temperatureCelsius;
    }

    @Override
    protected void setCmd(double cmd) {
        if (cmd <= 0.001) {
            electricalResistor.highImpedance();
        } else if (cmd >= 1.0) {
            electricalResistor.setResistance(minimumResistance);
        } else {
            electricalResistor.setResistance(minimumResistance / cmd);
        }
    }
}
