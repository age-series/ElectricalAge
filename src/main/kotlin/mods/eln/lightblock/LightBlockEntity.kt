package mods.eln.lightblock

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.EnumSkyBlock
import net.minecraft.world.World

class LightBlockEntity : TileEntity() {

    companion object {
        @JvmField
        val observers: MutableList<LightBlockObserver> = mutableListOf()

        @JvmStatic
        fun addLight(w: World, x: Int, y: Int, z: Int, light: Int, timeout: Int) {
            val block = w.getBlock(x, y, z)

            if (block !== Eln.lightBlock) {
                if (block !== Blocks.air) return
                w.setBlock(x, y, z, Eln.lightBlock, light, 2)
            }

            val t = w.getTileEntity(x, y, z)

            if (t is LightBlockEntity) t.addLight(light, timeout)
            else Utils.println("Error in setting light at %d %d %d", x, y, z)
        }

        @JvmStatic
        fun addLight(coord: Coordinate, light: Int, timeout: Int) {
            addLight(coord.world(), coord.x, coord.y, coord.z, light, timeout)
        }
    }

    interface LightBlockObserver {

        fun lightBlockDestructor(coord: Coordinate)

    }

    private val lightList: MutableList<LightHandle> = mutableListOf()

    private fun addLight(light: Int, timeout: Int) {
        lightList.add(LightHandle(light, timeout))
    }

    internal class LightHandle(var value: Int = 0, var timeout: Int = 0) : INBTTReady {

        override fun readFromNBT(nbt: NBTTagCompound, str: String) {
            value = nbt.getInteger(str + "value")
            timeout = nbt.getInteger(str + "timeout")
        }

        override fun writeToNBT(nbt: NBTTagCompound, str: String) {
            nbt.setInteger(str + "value", value)
            nbt.setInteger(str + "timeout", timeout)
        }

    }

    override fun updateEntity() {
        if (worldObj.isRemote) return

        if (lightList.isEmpty()) {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord)
            Utils.println("Destroy light at %d %d %d", xCoord, yCoord, zCoord)
            return
        }

        var light = 0
        val iterator: MutableIterator<LightHandle> = lightList.iterator()

        while (iterator.hasNext()) {
            val l = iterator.next()
            if (light < l.value) light = l.value
            l.timeout--
            if (l.timeout <= 0) iterator.remove()
        }

        if (light != worldObj.getBlockMetadata(xCoord, yCoord, zCoord)) {
            worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, light, 2)
            worldObj.updateLightByType(EnumSkyBlock.Block, xCoord, yCoord, zCoord)
        }
    }

}