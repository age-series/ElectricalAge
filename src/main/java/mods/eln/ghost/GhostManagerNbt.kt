package mods.eln.ghost

import mods.eln.Eln
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.WorldSavedData

class GhostManagerNbt(par1Str: String?) : WorldSavedData(par1Str) {
    override fun isDirty(): Boolean {
        return true
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        Eln.ghostManager.loadFromNBT(nbt)
    }

    override fun writeToNBT(nbt: NBTTagCompound) {}
}
