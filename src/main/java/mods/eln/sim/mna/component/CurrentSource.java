package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class CurrentSource extends Bipole implements ISubSystemProcessI, INBTTReady {
    double currentToSource;
    String name;

    public CurrentSource(String name) { this.name = name; }

    public CurrentSource(String name, State pinA, State pinB) {
        super(pinA,pinB);
        this.name = name;
    }

    public CurrentSource setCurrent(double i) {
        currentToSource = i;
        return this;
    }

    @Override
    public double getCurrent() {
        return currentToSource;
    }

    @Override
    public void applyTo(SubSystem s) {}

    @Override
    public void addedTo(SubSystem s) {
        s.addProcess(this);
    }

    @Override
    public void quitSubSystem() {
        if (subSystem != null)
            subSystem.removeProcess(this);
    }

    @Override
    public void simProcessI(SubSystem s) {
        s.addToI(aPin, currentToSource);
        s.addToI(bPin, -currentToSource);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        str += name;
        currentToSource = nbt.getDouble(str + "I");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "I", currentToSource);
    }
}
