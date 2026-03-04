package mods.eln.sim.process.destruct

import mods.eln.Eln
import mods.eln.misc.Utils
import mods.eln.misc.Utils.println
import mods.eln.sim.IProcess

enum class WatchdogType {
    THERMAL,
    RESISTOR_HEAT,
    VOLTAGE,
    SHAFT_SPEED,
    OTHER
}

abstract class ValueWatchdog : IProcess {
    private var destructible: IDestructible? = null
    var min = 0.0
    var max = 0.0
    var timeoutReset = 2.0
    var timeout = 0.0
    var boot = true

    // TODO: Rename. Hysteresis?
    private var joker = true
    protected open val watchdogType: WatchdogType = WatchdogType.OTHER

    override fun process(time: Double) {
        if (boot) {
            boot = false
            timeout = timeoutReset
        }
        val value = getValue()
        var overflow = (value - max).coerceAtLeast(min - value)
        if (overflow > 0) {
            if (joker) {
                joker = false
                overflow = 0.0
            }
        } else {
            joker = true
        }
        timeout -= time * overflow * Utils.rand(0.5, 1.5)
        if (timeout > timeoutReset) {
            timeout = timeoutReset
        }
        if (timeout < 0) {
            onDestroy(value, overflow)
            if (isWatchdogDestructionEnabled()) {
                println(
                    "%s destroying %s",
                    javaClass.name,
                    destructible?.describe()?: "Null destructible"
                )
                destructible?.destructImpl()
            } else {
                println(
                    "%s tripped (destruction disabled) for %s",
                    javaClass.name,
                    destructible?.describe() ?: "Null destructible"
                )
            }
        }
    }

    private fun isWatchdogDestructionEnabled(): Boolean {
        return when (watchdogType) {
            WatchdogType.THERMAL -> Eln.watchdogThermalEnabled
            WatchdogType.RESISTOR_HEAT -> Eln.watchdogResistorHeatEnabled
            WatchdogType.VOLTAGE -> Eln.watchdogVoltageEnabled
            WatchdogType.SHAFT_SPEED -> Eln.watchdogShaftSpeedEnabled
            WatchdogType.OTHER -> Eln.watchdogOtherEnabled
        }
    }

    fun setDestroys(destructible: IDestructible): ValueWatchdog {
        this.destructible = destructible
        return this
    }

    abstract fun getValue(): Double
    protected open fun onDestroy(value: Double, overflow: Double) {}

    fun reset() {
        boot = true
    }
}
