package mods.eln.transparentnode.turbine;

import mods.eln.Eln;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.VoltageSource;


public class TurbineThermalProcess implements IProcess {
    private final TurbineElement turbine;

    private double efficiency = 0.0;

    public TurbineThermalProcess(TurbineElement t) {
        this.turbine = t;
    }

    public double getEfficiency() {
        return efficiency;
    }

    static double computeEfficiency(double warmDeltaCelsius, double coolDeltaCelsius, double ambientKelvin) {
        double warmAbsoluteKelvin = warmDeltaCelsius + ambientKelvin;
        double coolAbsoluteKelvin = coolDeltaCelsius + ambientKelvin;
        if (warmAbsoluteKelvin <= 1e-6 || coolAbsoluteKelvin <= 1e-6) {
            return 0.05;
        }
        double computed = Math.abs(1 - coolAbsoluteKelvin / warmAbsoluteKelvin);
        return Math.max(0.05, computed);
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;

        VoltageSource src = turbine.electricalPowerSourceProcess;

        efficiency = computeEfficiency(
            turbine.warmLoad.temperatureCelsius,
            turbine.coolLoad.temperatureCelsius,
            turbine.getAmbientTemperatureKelvin()
        );

        double E = src.getPower() * time / Eln.instance.heatTurbinePowerFactor;

        double Pout = E / time;
        double Pin = descriptor.PoutToPin.getValue(Pout) / efficiency;
        turbine.warmLoad.movePowerTo(-Pin);
        turbine.coolLoad.movePowerTo(Pin);
    }
}
