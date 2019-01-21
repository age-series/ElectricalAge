package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.misc.Utils;

public class NbtElectricalGateInputOutput extends NbtElectricalLoad {

    public NbtElectricalGateInputOutput(String name) {
        super(name);
        Vars.signalCableDescriptor.applyTo(this);
    }

    public String plot(String str) {
        return str + " " + Utils.plotVolt("", getU()) + Utils.plotAmpere("", getCurrent());
    }

    public boolean isInputHigh() {
        return getU() > Vars.SVU * 0.6;
    }

    public boolean isInputLow() {
        return getU() < Vars.SVU * 0.2;
    }

    public double getInputNormalized() {
        double norm = getU() * Vars.SVUinv;
        if (norm < 0.0) norm = 0.0;
        if (norm > 1.0) norm = 1.0;
        return norm;
    }

    public double getInputBornedU() {
        double U = this.getU();
        if (U < 0.0) U = 0.0;
        if (U > Vars.SVU) U = Vars.SVU;
        return U;
    }
}
