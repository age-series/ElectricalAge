package mods.eln.misc

import net.minecraft.nbt.NBTTagCompound

class RcInterpolator(preTao: Float) : INBTTReady {
    var ff: Float = 1 / preTao
    var target: Float
    var factorFiltered: Float
    fun step(deltaT: Float) {
        factorFiltered += (target - factorFiltered) * ff * deltaT
    }

    fun get(): Float {
        return factorFiltered
    }

    fun setValue(value: Float) {
        factorFiltered = value
    }

    fun setValueFromTarget() {
        factorFiltered = target
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        target = nbt.getFloat(str + "factor")
        // Reverse compatibility. Leave this please.
        factorFiltered = if (nbt.hasKey("factorFiltred")) {
            nbt.getFloat(str + "factorFiltred")
        } else {
            nbt.getFloat(str + "factorFiltered")
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setFloat(str + "factor", target)
        nbt.setFloat(str + "factorFiltered", factorFiltered)
    }

    init {
        factorFiltered = 0f
        target = 0f
    }
}
