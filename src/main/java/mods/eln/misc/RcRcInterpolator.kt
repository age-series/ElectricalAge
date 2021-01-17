package mods.eln.misc

import net.minecraft.nbt.NBTTagCompound

class RcRcInterpolator(tao1: Float, tao2: Float) : INBTTReady {
    var c1: Float
    var c2: Float
    var target: Float
    var tao1Inv: Float = 1 / tao1
    var tao2Inv: Float = 1 / tao2
    fun step(deltaT: Float) {
        c1 += (target - c1) * tao1Inv * deltaT
        c2 += (c1 - c2) * tao2Inv * deltaT
    }

    fun get(): Float {
        return c2
    }

    fun setValue(value: Float) {
        c2 = value
        c1 = value
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        c1 = nbt.getFloat(str + "c1")
        c2 = nbt.getFloat(str + "c2")
        target = nbt.getFloat(str + "target")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setFloat(str + "c1", c1)
        nbt.setFloat(str + "c2", c2)
        nbt.setFloat(str + "target", target)
    }

    init {
        c1 = 0f
        c2 = 0f
        target = 0f
    }
}
