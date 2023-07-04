package mods.eln.sim.process.destruct

import mods.eln.sim.ThermalLoad
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.ThermalLoadInitializerByPowerDrop

class ThermalLoadWatchDog(var state: ThermalLoad): ValueWatchdog() {

    override fun getValue(): Double {
        return state.temperature
    }

    fun setMaximumTemperature(maximumTemperature: Double): ThermalLoadWatchDog {
        max = maximumTemperature
        min = -40.0
        // TODO: Abstract 0.1 as step time or seconds?
        timeoutReset = maximumTemperature * 0.1 * 10
        return this
    }

    fun setThermalLoad(t: ThermalLoadInitializer): ThermalLoadWatchDog {
        max = t.maximumTemperature
        min = t.minimumTemperature
        timeoutReset = max * 0.1 * 10
        return this
    }

    fun setTemperatureLimits(maximumTemperature: Double, minimumTemperature: Double): ThermalLoadWatchDog {
        max = maximumTemperature
        min = minimumTemperature
        timeoutReset = max * 0.1 * 10
        return this
    }

    fun setTemperatureLimits(t: ThermalLoadInitializerByPowerDrop): ThermalLoadWatchDog {
        max = t.maximumTemperature
        min = t.minimumTemperature
        timeoutReset = max * 0.1 * 10
        return this
    }
}
