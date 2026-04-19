package mods.eln.sim.nbt;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.sim.SignalLoadSupport;

public class NbtElectricalGateInput extends NbtElectricalLoad {

    public NbtElectricalGateInput(String name) {
        super(name);
        Eln.instance.signalCableDescriptor.applyTo(this);
    }

    public String plot(String str) {
        return Utils.plotSignal(getVoltage());
    }

    public boolean stateHigh() {
        return getSignalVoltage() > Eln.SVU * 0.6;
    }

    public boolean stateLow() {
        return getSignalVoltage() < Eln.SVU * 0.2;
    }

    public double getNormalized() {
        return SignalLoadSupport.toNormalized(getVoltage());
    }

    public double getSignalVoltage() {
        return SignalLoadSupport.clampSignalVoltage(getVoltage());
    }
}
