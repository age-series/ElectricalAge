package mods.eln.sixnode.electricalhumiditysensor

import mods.eln.environment.BiomeClimateService
import mods.eln.misc.INBTTReady
import mods.eln.misc.RcInterpolator
import mods.eln.sim.IProcess
import net.minecraft.nbt.NBTTagCompound

class ElectricalHumiditySensorSlowProcess(private val element: ElectricalHumiditySensorElement) : IProcess, INBTTReady {
    private var timeCounter = 0.0
    private val rc = RcInterpolator(3f)

    override fun process(time: Double) {
        timeCounter += time
        if (timeCounter > REFRESH_PERIOD) {
            timeCounter -= REFRESH_PERIOD
            val node = element.sixNode ?: return
            val coord = node.coordinate
            var target = 0f
            if (coord.worldExist) {
                val climate = BiomeClimateService.sample(coord.world(), coord.x, coord.y, coord.z)
                target = climate.normalizedHumidity.toFloat()
                rc.target = target
            }
            rc.step(time.toFloat())
            element.outputGateProcess.setOutputNormalized(rc.get().toDouble())
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        rc.setValue(nbt.getFloat(str + "rc"))
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setFloat(str + "rc", rc.get())
    }

    companion object {
        private const val REFRESH_PERIOD = 0.2
    }
}
