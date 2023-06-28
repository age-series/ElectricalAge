package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class CurrentSource extends Bipole implements ISubSystemProcessI, INBTTReady {
    double current;
    String name;

    public CurrentSource(String name) { this.name = name; }

    public CurrentSource(String name, State pinA, State pinB) {
        super(pinA,pinB);
        this.name = name;
    }

    public CurrentSource setCurrent(double i) {
        current = i;
        return this;
    }

    @Override
    public double getCurrent() {
        return current;
    }

    @Override
    public void applyToSubsystem(SubSystem s) {}

    @Override
    public void addToSubsystem(SubSystem s) {
        s.addProcess(this);
    }

    @Override
    public void quitSubSystem() {
        if (subSystem != null)
            subSystem.removeProcess(this);
    }

    @Override
    public void simProcessI(SubSystem s) {
        s.addToI(aPin, current);
        s.addToI(bPin, -current);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        str += name;
        current = nbt.getDouble(str + "I");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "I", current);
    }
}
