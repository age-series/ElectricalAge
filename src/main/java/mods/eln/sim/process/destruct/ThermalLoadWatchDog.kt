package mods.eln.sim.process.destruct;

import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.ThermalLoadInitializerByPowerDrop;

public class ThermalLoadWatchDog extends ValueWatchdog {

    ThermalLoad state;

    @Override
    double getValue() {
        return state.getTemperature();
    }

    public ThermalLoadWatchDog setThermalLoad(ThermalLoad state) {
        this.state = state;
        return this;
    }

    public ThermalLoadWatchDog setMaximumTemperature(double maximumTemperature) {
        this.max = maximumTemperature;
        this.min = -40;
        // TODO: Abstract 0.1 as step time or seconds?
        this.timeoutReset = maximumTemperature * 0.1 * 10;
        return this;
    }

    public ThermalLoadWatchDog setThermalLoad(ThermalLoadInitializer t) {
        this.max = t.maximumTemperature;
        this.min = t.minimumTemperature;
        this.timeoutReset = max * 0.1 * 10;
        return this;
    }

    public ThermalLoadWatchDog setLimit(double maximumTemperature, double minimumTemperature) {
        this.max = maximumTemperature;
        this.min = minimumTemperature;
        this.timeoutReset = max * 0.1 * 10;
        return this;
    }

    public ThermalLoadWatchDog setLimit(ThermalLoadInitializerByPowerDrop t) {
        this.max = t.maximumTemperature;
        this.min = t.minimumTemperature;
        this.timeoutReset = max * 0.1 * 10;
        return this;
    }
}
