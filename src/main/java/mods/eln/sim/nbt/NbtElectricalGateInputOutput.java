package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.sim.SignalLoadSupport;

public class NbtElectricalGateInputOutput extends NbtElectricalLoad {

    public NbtElectricalGateInputOutput(String name) {
        super(name);
        Eln.instance.signalCableDescriptor.applyTo(this);
    }

    public String plot(String str) {
        return str + " " + Utils.plotVolt("", getVoltage()) + Utils.plotAmpere("", getCurrent());
    }

    public boolean isInputHigh() {
        return getInputVoltage() > Eln.SVU * 0.6;
    }

    public boolean isInputLow() {
        return getInputVoltage() < Eln.SVU * 0.2;
    }

    public double getInputNormalized() {
        return SignalLoadSupport.toNormalized(getVoltage());
    }

    public double getInputVoltage() {
        return SignalLoadSupport.clampSignalVoltage(getVoltage());
    }
}
