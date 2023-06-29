@file:Suppress("NAME_SHADOWING")
package mods.eln.node

import mods.eln.misc.Direction
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.EnumSkyBlock
import java.io.DataOutputStream
import java.io.IOException

abstract class Node : NodeBase() {
    private var lastLight = 0
    var lightValue: Int
        get() = lastLight
        set(light) {
            var light = light
            if (light > 15) light = 15
            if (light < 0) light = 0
            if (lastLight != light) {
                lastLight = light
                coordinate.world().updateLightByType(EnumSkyBlock.Block, coordinate.x, coordinate.y, coordinate.z)
                needPublish = true
            }
        }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        lastLight = nbt.getByte("lastLight").toInt()
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setByte("lastLight", lastLight.toByte())
    }

    var oldSendedRedstone = false
    override fun publishSerialize(stream: DataOutputStream) {
        super.publishSerialize(stream)
        try {
            val redstone = canConnectRedstone()
            stream.writeByte(lastLight or if (redstone) 0x10 else 0x00)
            if (redstone != oldSendedRedstone) needNotify = true
            oldSendedRedstone = redstone
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    val entity: NodeBlockEntity
        get() = coordinate.world().getTileEntity(coordinate.x, coordinate.y, coordinate.z) as NodeBlockEntity

    open fun isProvidingWeakPower(side: Direction?): Int {
        return 0
    }

    open fun canConnectRedstone(): Boolean {
        return false
    }
}
