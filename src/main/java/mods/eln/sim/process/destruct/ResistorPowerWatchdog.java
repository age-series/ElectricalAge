package mods.eln.sim.process.destruct;

import mods.eln.sim.mna.component.Resistor;

public class ResistorPowerWatchdog extends ValueWatchdog {

    Resistor resistor;

    public ResistorPowerWatchdog setResistor(Resistor resistor) {
        this.resistor = resistor;
        return this;
    }

    public ResistorPowerWatchdog setMaximumPower(double maximumPower) {
        this.max = maximumPower;
        this.min = -1;
        // TODO: Abstract 0.2 as step time or seconds?
        this.timeoutReset = maximumPower * 0.20 * 5;

        return this;
    }

    @Override
    double getValue() {
        return resistor.getPower();
    }
}
