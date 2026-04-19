package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.misc.INBTTReady;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.nbt.NBTTagCompound;

public abstract class NodeElectricalGateInputHysteresisProcess implements IProcess, INBTTReady {

    NbtElectricalGateInput gate;
    String name;
    double lowThreshold;
    double highThreshold;

    boolean state = false;

    public NodeElectricalGateInputHysteresisProcess(String name, NbtElectricalGateInput gate) {
        this(name, gate, Eln.SVU * 0.3, Eln.SVU * 0.7);
    }

    public NodeElectricalGateInputHysteresisProcess(String name, NbtElectricalGateInput gate, double lowThreshold, double highThreshold) {
        this.gate = gate;
        this.name = name;
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
    }

    protected abstract void setOutput(boolean value);

    @Override
    public void process(double time) {
        if (state) {
            if (gate.getVoltage() < lowThreshold) {
                state = false;
                setOutput(false);
            } else setOutput(true);
        } else {
            if (gate.getVoltage() > highThreshold) {
                state = true;
                setOutput(true);
            } else setOutput(false);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        state = nbt.getBoolean(str + name + "state");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setBoolean(str + name + "state", state);
    }
}
