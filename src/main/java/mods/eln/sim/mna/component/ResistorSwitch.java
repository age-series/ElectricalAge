package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class ResistorSwitch extends Resistor implements INBTTReady {

    String name;

    boolean state = false;

    protected double baseResistance = 1;

    protected double offResistance = MnaConst.highImpedance;

    public ResistorSwitch(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    public void setState(boolean state) {
        this.state = state;
        setResistance(baseResistance);
    }

    public void setOffResistance(double resistance) {
        offResistance = resistance;
        setResistance(baseResistance);
    }

    @Override
    public void highImpedance() {
        setResistance(offResistance);
    }

    @Override
    public Resistor setResistance(double resistance) {
        baseResistance = resistance;
        return super.setResistance(state ? resistance : offResistance);
    }

    public boolean getState() {
        return state;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        str += name;
        setResistance(nbt.getDouble(str + "R"));
        if (!Double.isFinite(baseResistance) || baseResistance == 0) {
            highImpedance();
        }
        setState(nbt.getBoolean(str + "State"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "R", baseResistance);
        nbt.setBoolean(str + "State", getState());
    }
}
