package mods.eln.sim.mna.process;

import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.mna.SubSystem.Thevenin;

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
        Thevenin a = aVoltgeSource.getSubSystem().getTh(aState, aVoltgeSource);
        Thevenin b = bVoltgeSource.getSubSystem().getTh(bState, bVoltgeSource);

        double voltage = (a.voltage * b.resistance + ratio * b.voltage * a.resistance) / (b.resistance + ratio * ratio * a.resistance);
        if (Double.isNaN(voltage)) {
            voltage = 0;
        }

        aVoltgeSource.setVoltage(voltage);
        bVoltgeSource.setVoltage(voltage * ratio);
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public double getRatio() {
        return this.ratio;
    }
}
