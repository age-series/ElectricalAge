package mods.eln.item;

import net.minecraft.nbt.NBTTagCompound;

import java.util.Map;

public interface IConfigurable {
    void readConfigTool(NBTTagCompound compound);
    void writeConfigTool(NBTTagCompound compound);
}
