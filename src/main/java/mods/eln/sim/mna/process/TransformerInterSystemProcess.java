package mods.eln.sim.mna.process;

import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.SubSystem.Th;

public class TransformerInterSystemProcess implements IRootSystemPreStepProcess {
    State aState, bState;
    VoltageSource aVoltgeSource, bVoltgeSource;

    double ratio = 1;

    public TransformerInterSystemProcess(State aState, State bState, VoltageSource aVoltgeSource, VoltageSource bVoltgeSource) {
        this.aState = aState;
        this.bState = bState;
        this.aVoltgeSource = aVoltgeSource;
        this.bVoltgeSource = bVoltgeSource;
    }

    @Override
    public void rootSystemPreStepProcess() {
        Th a = aVoltgeSource.getSubSystem().getTh(aState, aVoltgeSource);
        Th b = bVoltgeSource.getSubSystem().getTh(bState, bVoltgeSource);

        double aU = (a.U * b.R + ratio * b.U * a.R) / (b.R + ratio * ratio * a.R);
        if (Double.isNaN(aU)) {
            aU = 0;
        }

        aVoltgeSource.setU(aU);
        bVoltgeSource.setU(aU * ratio);
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public double getRatio() {
        return this.ratio;
    }
}
