package mods.eln.sim.watchdogs;

import mods.eln.sim.thermal.ThermalLoad;
import mods.eln.sim.thermal.ThermalLoadInitializer;
import mods.eln.sim.thermal.ThermalLoadInitializerByPowerDrop;

public class ThermalLoadWatchDog extends ValueWatchdog {

    ThermalLoad state;

    @Override
    double getValue() {
        return state.getT();
    }

    public ThermalLoadWatchDog set(ThermalLoad state) {
        this.state = state;
        return this;
    }

    public ThermalLoadWatchDog setTMax(double tMax) {
        this.max = tMax;
        this.min = -40;
        this.timeoutReset = tMax * 0.1 * 10;
        return this;
    }

    public ThermalLoadWatchDog set(ThermalLoadInitializer t) {
        this.max = t.warmLimit;
        this.min = t.coolLimit;
        this.timeoutReset = max * 0.1 * 10;
        return this;
    }

    public ThermalLoadWatchDog setLimit(double thermalWarmLimit, double thermalCoolLimit) {
        this.max = thermalWarmLimit;
        this.min = thermalCoolLimit;
        this.timeoutReset = max * 0.1 * 10;
        return this;
    }

    public ThermalLoadWatchDog setLimit(ThermalLoadInitializerByPowerDrop t) {
        this.max = t.warmLimit;
        this.min = t.coolLimit;
        this.timeoutReset = max * 0.1 * 10;
        return this;
    }
}
