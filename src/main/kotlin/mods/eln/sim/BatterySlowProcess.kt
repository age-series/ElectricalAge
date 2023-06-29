package mods.eln.sim

import mods.eln.server.SaveConfig

abstract class BatterySlowProcess(private var batteryProcess: BatteryProcess, var thermalLoad: ThermalLoad) : IProcess {

    var lifeNominalCurrent = 0.0
    var lifeNominalLost = 0.0

    override fun process(time: Double) {
        val voltage = batteryProcess.u
        if (voltage < -0.1 * batteryProcess.uNominal) {
            destroy()
            return
        }
        if (voltage > maximumVoltage) {
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

    val maximumVoltage: Double
        get() = 1.3 * batteryProcess.uNominal

    abstract fun destroy()
}
