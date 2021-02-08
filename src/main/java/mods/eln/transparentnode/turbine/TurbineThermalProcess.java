package mods.eln.transparentnode.turbine;

import mods.eln.Eln;
import mods.eln.sim.IProcess;
import mods.eln.sim.thermal.ThermalConstants;
import mods.eln.sim.electrical.mna.component.VoltageSource;


public class TurbineThermalProcess implements IProcess {
    private final TurbineElement turbine;

    private double efficiency = 0.0;

    public TurbineThermalProcess(TurbineElement t) {
        this.turbine = t;
    }

    public double getEfficiency() {
        return efficiency;
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;

        VoltageSource src = turbine.electricalPowerSourceProcess;

        efficiency = Math.abs(1 - (turbine.coolLoad.Tc + ThermalConstants.AMBIENT_TEMPERATURE_KELVIN) / (turbine.warmLoad.Tc + ThermalConstants.AMBIENT_TEMPERATURE_KELVIN));
        if (efficiency < 0.05) efficiency = 0.05;

        double E = src.getP() * time / Eln.instance.heatTurbinePowerFactor;

        double Pout = E / time;
        double Pin = descriptor.PoutToPin.getValue(Pout) / efficiency;
        turbine.warmLoad.movePowerTo(-Pin);
        turbine.coolLoad.movePowerTo(Pin);
    }
}
