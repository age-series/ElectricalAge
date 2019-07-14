package mods.eln.sim.nbt

import mods.eln.misc.INBTTReady
import mods.eln.sim.FurnaceProcess
import mods.eln.sim.ThermalLoad
import net.minecraft.nbt.NBTTagCompound

class NbtFurnaceProcess(internal var name: String, load: ThermalLoad) : FurnaceProcess(load), INBTTReady {

    override fun readFromNBT(nbttagcompound: NBTTagCompound, str: String) {
        combustibleEnergy = nbttagcompound.getDouble(str + name + "Q")
        gain = nbttagcompound.getDouble(str + name + "gain")
    }

    override fun writeToNBT(nbttagcompound: NBTTagCompound, str: String) {
        nbttagcompound.setDouble(str + name + "Q", combustibleEnergy)
        nbttagcompound.setDouble(str + name + "gain", gain)
    }
}
