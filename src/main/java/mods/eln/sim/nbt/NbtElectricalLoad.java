package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NbtElectricalLoad extends ElectricalLoad implements INBTTReady {

    String name;

    public NbtElectricalLoad(String name) {
        super();
        this.name = name;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
        setVoltage(nbttagcompound.getFloat(str + name + "Uc"));
        if (Double.isNaN(getVoltage())) setVoltage(0);
        if (getVoltage() == Float.NEGATIVE_INFINITY) setVoltage(0);
        if (getVoltage() == Float.POSITIVE_INFINITY) setVoltage(0);
    }

    public void writeToNBT(NBTTagCompound nbttagcompound, String str) {
        nbttagcompound.setFloat(str + name + "Uc", (float) getVoltage());
    }
}
