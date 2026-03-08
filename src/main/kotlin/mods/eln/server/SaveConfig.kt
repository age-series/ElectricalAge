package mods.eln.server

import mods.eln.Eln
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.WorldSavedData

class SaveConfig(par1Str: String) : WorldSavedData(par1Str) {
    override fun readFromNBT(nbt: NBTTagCompound) {
        Eln.wind.readFromNBT(nbt, "wind")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        Eln.wind.writeToNBT(nbt, "wind")
    }

    override fun isDirty(): Boolean {
        return true
    }

    companion object {
        @JvmField
        var instance: SaveConfig? = null
    }

    init {
        instance = this
    }
}
