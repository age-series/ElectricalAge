package mods.eln.sim.process.destruct

import mods.eln.misc.Utils.println
import mods.eln.sim.MnaMatrixDebugger
import mods.eln.sim.ThermalLoad
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.ThermalLoadInitializerByPowerDrop
import java.util.Locale

class ThermalLoadWatchDog(var state: ThermalLoad): ValueWatchdog() {
    private var lastTemperature = state.temperature
    private var lastDeltaPerSecond = 0.0
    private var matrixDumpSupplier: (() -> Any?)? = null
    private var matrixDumpReason: String? = null
    private var ambientTemperatureProvider: (() -> Double)? = null

    override fun getValue(): Double {
        return state.temperature + (ambientTemperatureProvider?.invoke() ?: 0.0)
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
        val ambientCelsius = ambientTemperatureProvider?.invoke() ?: 0.0
        val thermalDeltaCelsius = state.temperature
        println(
            "Thermal watchdog trip at %.2f°C abs (%.2f°C delta, %.2f°C ambient; limits %.2f/%.2f, overflow %.2f°C, dT %.2f°C/s, Pc %.1fW, heatCapacity %.1fJ/K)",
            value,
            thermalDeltaCelsius,
            ambientCelsius,
            max,
            min,
            overflow,
            lastDeltaPerSecond,
            state.Pc,
            state.heatCapacity
        )
        matrixDumpSupplier?.let { supplier ->
            runCatching { supplier() }.getOrNull()?.let { target ->
                val reason = matrixDumpReason ?: String.format(
                    Locale.ROOT,
                    "Thermal watchdog %.1f°C",
                    value
                )
                MnaMatrixDebugger.dump(target, reason)
            }
        }
    }

    fun setMaximumTemperature(maximumTemperature: Double): ThermalLoadWatchDog {
        max = maximumTemperature
        min = -40.0
        timeoutReset = maximumTemperature
        return this
    }

    fun setThermalLoad(t: ThermalLoadInitializer): ThermalLoadWatchDog {
        max = t.maximumTemperature
        min = t.minimumTemperature
        timeoutReset = max
        return this
    }

    fun setTemperatureLimits(maximumTemperature: Double, minimumTemperature: Double): ThermalLoadWatchDog {
        max = maximumTemperature
        min = minimumTemperature
        timeoutReset = max
        return this
    }

    fun setTemperatureLimits(t: ThermalLoadInitializerByPowerDrop): ThermalLoadWatchDog {
        max = t.maximumTemperature
        min = t.minimumTemperature
        timeoutReset = max
        return this
    }

    fun dumpMatrixOnTrip(reason: String? = null, supplier: () -> Any?): ThermalLoadWatchDog {
        matrixDumpReason = reason
        matrixDumpSupplier = supplier
        return this
    }

    fun setAmbientTemperatureProvider(provider: () -> Double): ThermalLoadWatchDog {
        ambientTemperatureProvider = provider
        return this
    }
}
