package mods.eln.transparentnode.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.mna.SubSystem.Thevenin;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;


public class TurbineElectricalProcess implements IProcess, IRootSystemPreStepProcess {
    private final TurbineElement turbine;

    public TurbineElectricalProcess(TurbineElement turbine) {
        this.turbine = turbine;
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;
        double deltaT = turbine.warmLoad.temperatureCelsius - turbine.coolLoad.temperatureCelsius;
        double targetU = descriptor.TtoU.getValue(deltaT);

        Thevenin th = turbine.positiveLoad.getSubSystem().getTh(turbine.positiveLoad, turbine.electricalPowerSourceProcess);
        double Ut;
        if (targetU < th.voltage) {
            Ut = th.voltage;
        } else if (th.isHighImpedance()) {
            Ut = targetU;
        } else {
            double a = 1 / th.resistance;
            double b = descriptor.powerOutPerDeltaU - th.voltage / th.resistance;
            double c = -descriptor.powerOutPerDeltaU * targetU;
            Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        }

        double i = (Ut - th.voltage) / th.resistance;
        double p = i * Ut;
        double pMax = descriptor.nominalP * 1.5;
        if (p > pMax) {
            Ut = (Math.sqrt(th.voltage * th.voltage + 4 * pMax * th.resistance) + th.voltage) / 2;
            Ut = Math.min(Ut, targetU);
            if (Double.isNaN(Ut)) Ut = 0;
            if (Ut < th.voltage) Ut = th.voltage;
        }

        turbine.electricalPowerSourceProcess.setVoltage(Ut);
    }

    @Override
    public void rootSystemPreStepProcess() {
        process(0);
    }
}
