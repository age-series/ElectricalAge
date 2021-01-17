package mods.eln.misc

import mods.eln.sim.IProcess
import net.minecraft.nbt.NBTTagCompound
import kotlin.math.pow

class WindProcess : IProcess, INBTTReady {
    var windHit = 5.0
    var windTarget = 5.0
    var windVariation = 0.0
    var windTargetNoose = 0.0
    var windTargetFiltered = RcInterpolator(60f)

    override fun process(time: Double) {
        val varF = 0.01
        windHit += windVariation * time
        windVariation += (target - windHit) * varF * time + (Math.random() * 2 - 1) * 0.1 * time
        windVariation *= 1 - 0.01 * time
        if (Math.random() < time / 1200) {
            newWindTarget()
        }
        if (Math.random() < time / 120) {
            windTargetNoose = (Math.random() * 2 - 1) * 1.2
        }
        windTargetFiltered.target = windTarget.toFloat()
        windTargetFiltered.step(time.toFloat())
    }

    fun newWindTarget() {
        val next = (Math.random().pow(3.0) * 20).toFloat()
        windTarget += (next - windTarget) * 0.7
    }

    val target: Double
        get() = windTargetNoose + windTargetFiltered.get()
    val targetNotFiltered: Double
        get() = windTargetNoose + windTargetFiltered.target

    fun getWind(y: Int): Double {
        return y.toDouble().coerceIn(windHit * y / 100.0, 100.0)
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        windHit = nbt.getDouble(str + "windHit")
        windTarget = nbt.getDouble(str + "windTarget")
        windVariation = nbt.getDouble(str + "windVariation")
        // NOTE: Please leave this for backwards compatibility
        if (nbt.hasKey(str + "windTargetFiltred")) {
            windTargetFiltered.setValue(nbt.getFloat(str + "windTargetFiltred"))
        } else {
            windTargetFiltered.setValue(nbt.getFloat(str + "windTargetFiltered"))
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + "windHit", windHit)
        nbt.setDouble(str + "windTarget", windTarget)
        nbt.setDouble(str + "windVariation", windVariation)
        nbt.setFloat(str + "windTargetFiltered", windTargetFiltered.get())
    }
}
