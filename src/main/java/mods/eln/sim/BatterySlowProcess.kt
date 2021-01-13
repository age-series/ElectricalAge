package mods.eln.sim

import mods.eln.server.SaveConfig

abstract class BatterySlowProcess(var batteryProcess: BatteryProcess, var thermalLoad: ThermalLoad) : IProcess {

    var lifeNominalCurrent = 0.0
    var lifeNominalLost = 0.0

    override fun process(time: Double) {
        val U = batteryProcess.u
        if (U < -0.1 * batteryProcess.uNominal) {
            destroy()
            return
        }
        if (U > uMax) {
            destroy()
            return
        }
        if (SaveConfig.instance!!.batteryAging) {
            var newLife = batteryProcess.life
            val normalisedCurrent = Math.abs(batteryProcess.dischargeCurrent) / lifeNominalCurrent
            newLife -= normalisedCurrent * normalisedCurrent * lifeNominalLost * time
            if (newLife < 0.1) newLife = 0.1
            batteryProcess.changeLife(newLife)
        }
    }

    val uMax: Double
        get() = 1.3 * batteryProcess.uNominal

    abstract fun destroy()
}
