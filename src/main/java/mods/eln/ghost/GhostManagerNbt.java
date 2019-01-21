package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.Vars;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class GhostManagerNbt extends WorldSavedData {
    public GhostManagerNbt(String par1Str) {
        super(par1Str);
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        Vars.ghostManager.loadFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        //Eln.ghostManager.saveToNbt(nbt, Integer.MIN_VALUE);
    }
}
