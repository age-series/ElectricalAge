package mods.eln.transparentnode.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.misc.Th;

public class TurbineElectricalProcess implements IProcess, IRootSystemPreStepProcess {
    private final TurbineElement turbine;

    public TurbineElectricalProcess(TurbineElement turbine) {
        this.turbine = turbine;
    }

    @Override
    public void process(double time) {
        TurbineDescriptor descriptor = turbine.descriptor;
        double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
        double targetU = descriptor.TtoU.getValue(deltaT);

        Th th = mods.eln.sim.mna.misc.Th.getTh(turbine.positiveLoad, turbine.electricalPowerSourceProcess);
        double Ut;
        if (targetU < th.getU()) {
            Ut = th.getU();
        } else if (th.isHighImpedance()) {
            Ut = targetU;
        } else {
            double a = 1 / th.getR();
            double b = descriptor.powerOutPerDeltaU - th.getU() / th.getR();
            double c = -descriptor.powerOutPerDeltaU * targetU;
            Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        }

        double i = (Ut - th.getU()) / th.getR();
        double p = i * Ut;
        double pMax = descriptor.nominalP * 1.5;
        if (p > pMax) {
            Ut = (Math.sqrt(th.getU() * th.getU() + 4 * pMax * th.getR()) + th.getU()) / 2;
            Ut = Math.min(Ut, targetU);
            if (Double.isNaN(Ut)) Ut = 0;
            if (Ut < th.getU()) Ut = th.getU();
        }

        turbine.electricalPowerSourceProcess.setU(Ut);
    }

    @Override
    public void rootSystemPreStepProcess() {
        process(0);
    }
}
