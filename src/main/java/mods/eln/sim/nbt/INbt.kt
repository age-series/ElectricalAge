package mods.eln.sim.nbt

import net.minecraft.nbt.NBTTagCompound

/**
 * Anything in Sim that does NBT related stuff, to save the state of those items.
 * This is more of a "look, there's NBT here" on source inspection, more than anything else.
 */
interface INbt {
    fun readFromNBT(nbt: NBTTagCompound, str: String)
    fun writeToNBT(nbt: NBTTagCompound, str: String)
}
