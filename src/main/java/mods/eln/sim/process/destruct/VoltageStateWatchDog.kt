package mods.eln.sim.process.destruct

import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.sim.mna.state.VoltageState

class VoltageStateWatchDog : ValueWatchdog() {

    internal var state: VoltageState? = null

    override fun getValue(): Double? = state?.u

    fun set(state: VoltageState): VoltageStateWatchDog {
        this.state = state
        return this
    }

    fun setUNominal(uNominal: Double): VoltageStateWatchDog {
        this.max = uNominal * 1.3
        this.min = -uNominal * 1.3
        this.timeoutReset = uNominal * 0.05 * 5.0
        return this
    }

    fun setUNominalMirror(uNominal: Double): VoltageStateWatchDog {
        this.max = uNominal * 1.3
        this.min = -max
        this.timeoutReset = uNominal * 0.05 * 5.0
        return this
    }

    fun setUMaxMin(uNominal: Double): VoltageStateWatchDog {
        this.max = uNominal * 1.3
        this.min = -uNominal * 1.3
        this.timeoutReset = uNominal * 0.05 * 5.0
        return this
    }

    companion object {
        @JvmStatic
        fun main(vars: Array<String>) {
            val watchdog = VoltageStateWatchDog()
            val voltageState = VoltageState("Basic Voltage State")
            watchdog.set(voltageState)
            watchdog.setUMaxMin(200.0)
            voltageState.u = 195.0
            for (i in 0 .. 50) {
                DP.println(DPType.SIM, "Voltage: ${voltageState.u} (should expl at ${200 * 1.3})")
                simulate(watchdog, 0.05)
                voltageState.u += 5
            }
            voltageState.u = -195.0
            for (i in 0 .. 50) {
                DP.println(DPType.SIM, "Voltage: ${voltageState.u} (should expl at ${-200 * 1.3})")
                simulate(watchdog, 0.05)
                voltageState.u -= 5
            }
        }

        @JvmStatic
        fun simulate(watchdog: VoltageStateWatchDog, time: Double) {
            watchdog.process(time)
        }
    }
}
