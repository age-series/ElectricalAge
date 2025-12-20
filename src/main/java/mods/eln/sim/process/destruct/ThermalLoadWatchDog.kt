package mods.eln.sim.process.destruct

import mods.eln.misc.Utils.println
import mods.eln.sim.ThermalLoad
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.ThermalLoadInitializerByPowerDrop

class ThermalLoadWatchDog(var state: ThermalLoad): ValueWatchdog() {
    private var lastTemperature = state.temperature
    private var lastDeltaPerSecond = 0.0

    override fun getValue(): Double {
        return state.temperature
    }

    override fun process(time: Double) {
        if (time > 0) {
            lastDeltaPerSecond = (state.temperature - lastTemperature) / time
        } else {
            lastDeltaPerSecond = 0.0
        }
        lastTemperature = state.temperature
        super.process(time)
    }

    override fun onDestroy(value: Double, overflow: Double) {
        println(
            "Thermal watchdog trip at %.2f°C (limits %.2f/%.2f, overflow %.2f°C, dT %.2f°C/s, Pc %.1fW, heatCapacity %.1fJ/K)",
            value,
            max,
            min,
            overflow,
            lastDeltaPerSecond,
            state.Pc,
            state.heatCapacity
        )
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
