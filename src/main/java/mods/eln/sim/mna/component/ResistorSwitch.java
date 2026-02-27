package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class ResistorSwitch extends Resistor implements INBTTReady {

    String name;

    private boolean state = false;

    protected double baseResistance = 1;

    protected double offResistance = MnaConst.highImpedance;

    public ResistorSwitch(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    public void setState(boolean state) {
        this.state = state;
        super.setResistance(state ? baseResistance : offResistance);
    }

    public void setOffResistance(double resistance) {
        offResistance = resistance;
        super.setResistance(state ? baseResistance : offResistance);
    }

    @Override
    public void highImpedance() {
        super.setResistance(offResistance);
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
        double resistance = nbt.getDouble(str + "R");
        if (!Double.isFinite(resistance) || resistance == 0) {
            baseResistance = offResistance;
        } else {
            baseResistance = resistance;
        }
        state = nbt.getBoolean(str + "State");
        super.setResistance(state ? baseResistance : offResistance);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "R", baseResistance);
        nbt.setBoolean(str + "State", getState());
    }
}
