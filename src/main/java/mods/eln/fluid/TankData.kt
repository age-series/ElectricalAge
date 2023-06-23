package mods.eln.fluid

import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidTank
import java.lang.Exception

data class TankData(val tank: FluidTank, val fluidWhitelist: MutableList<Fluid> = mutableListOf(), var fractionalDemandMb: Double = 0.0):
    INBTTReady {

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        tank.readFromNBT(nbt.getCompoundTag("${str}tank"))
        val fluidWhitelistNames = nbt.getString("${str}whitelist")?.split("|")!!
        fluidWhitelist.clear()
        fluidWhitelistNames.forEach {
            try {
                fluidWhitelist.add(FluidRegistry.getFluid(it))
            } catch (e: Exception) {
                Utils.println("Error, could not find fluid $it")
            }
        }
        fractionalDemandMb = nbt.getDouble("${str}demandMb")
        tank.capacity = nbt.getInteger("${str}capacity")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        val tag = NBTTagCompound()
        tank.writeToNBT(tag)
        nbt.setTag("${str}tank", tag)
        nbt.setString("${str}whitelist", fluidWhitelist.joinToString("|") { it.name })
        nbt.setDouble("${str}demandMb", fractionalDemandMb)
        nbt.setInteger("${str}capacity", tank.capacity)
    }

    override fun toString(): String {
        return "TankData(${tank.fluidAmount}/${tank.capacity}mB of ${tank.fluid}, whitelist: ${fluidWhitelist}, ${fractionalDemandMb}mB spare"
    }
}
