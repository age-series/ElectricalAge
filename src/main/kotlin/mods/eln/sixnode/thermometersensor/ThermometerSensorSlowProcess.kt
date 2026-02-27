package mods.eln.sixnode.thermometersensor

import mods.eln.Eln
import mods.eln.environment.BiomeClimateService
import mods.eln.misc.INBTTReady
import mods.eln.misc.RcInterpolator
import mods.eln.sim.IProcess
import net.minecraft.nbt.NBTTagCompound

class ThermometerSensorSlowProcess(private val element: ThermometerSensorElement) : IProcess, INBTTReady {
    private var timeCounter = 0.0
    private val rc = RcInterpolator(3f)

    override fun process(time: Double) {
        timeCounter += time
        if (timeCounter > REFRESH_PERIOD) {
            timeCounter -= REFRESH_PERIOD

            val node = element.sixNode ?: return
            val coord = node.coordinate
            var normalized = 0.0
            if (coord.worldExist) {
                val climate = BiomeClimateService.sample(coord.world(), coord.x, coord.y, coord.z)
                val span = (element.highValue - element.lowValue).coerceAtLeast(0.0001f)
                normalized = ((climate.temperatureCelsius.toFloat() - element.lowValue) / span).toDouble()
            }

            normalized = normalized.coerceIn(0.0, 1.0)
            rc.target = normalized.toFloat()
            rc.step(time.toFloat())
            element.outputGateProcess.setVoltage(rc.get().toDouble() * Eln.SVU)
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
