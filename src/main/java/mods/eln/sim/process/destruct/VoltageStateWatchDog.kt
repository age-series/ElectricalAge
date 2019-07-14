package mods.eln.sim.process.destruct

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
}
