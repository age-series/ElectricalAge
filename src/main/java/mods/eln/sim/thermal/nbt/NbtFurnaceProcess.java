package mods.eln.sim.thermal.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.thermal.process.FurnaceProcess;
import mods.eln.sim.thermal.ThermalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NbtFurnaceProcess extends FurnaceProcess implements INBTTReady {

    String name;

    public NbtFurnaceProcess(String name, ThermalLoad load) {
        super(load);
        this.name = name;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
        combustibleEnergy = nbttagcompound.getFloat(str + name + "Q");
        setGain(nbttagcompound.getDouble(str + name + "gain"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbttagcompound, String str) {
        nbttagcompound.setFloat(str + name + "Q", (float) combustibleEnergy);
        nbttagcompound.setDouble(str + name + "gain", getGain());
    }
}
