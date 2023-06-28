package mods.eln.sim.nbt;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.NBTTagCompound;

public class NbtThermalLoad extends ThermalLoad implements INBTTReady {

    String name;

    public NbtThermalLoad(String name, double Tc, double Rp, double Rs, double C) {
        super(Tc, Rp, Rs, C);
        this.name = name;
    }

    public NbtThermalLoad(String name) {
        super();
        this.name = name;
    }

    public void readFromNBT(NBTTagCompound nbttagcompound, String str) {
        temperatureCelsius = nbttagcompound.getFloat(str + name + "Tc");
        if (Double.isNaN(temperatureCelsius)) temperatureCelsius = 0;
        if (temperatureCelsius == Float.NEGATIVE_INFINITY) temperatureCelsius = 0;
        if (temperatureCelsius == Float.POSITIVE_INFINITY) temperatureCelsius = 0;
    }

    public void writeToNBT(NBTTagCompound nbttagcompound, String str) {
        nbttagcompound.setFloat(str + name + "Tc", (float) temperatureCelsius);
    }
}
