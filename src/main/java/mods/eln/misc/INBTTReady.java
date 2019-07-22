package mods.eln.misc;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTTReady {
    void readFromNBT(NBTTagCompound nbt, String str);
    void writeToNBT(NBTTagCompound nbt, String str);
}
