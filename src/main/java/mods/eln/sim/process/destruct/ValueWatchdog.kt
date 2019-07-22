package mods.eln.sim.process.destruct

import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.misc.Utils
import mods.eln.sim.core.IProcess

abstract class ValueWatchdog : IProcess {

    internal var destructable: IDestructable? = null

    var min: Double = 0.toDouble()
    var max: Double = 0.toDouble()

    var timeoutReset = 2.0

    internal var timeout = 0.0
    internal var boot = true
    internal var joker = true

    internal var rand = Utils.rand(0.5, 1.5)

    abstract fun getValue(): Double?

    override fun process(time: Double) {
        if (boot) {
            boot = false
            timeout = timeoutReset
        }
        val value = getValue() ?: 0.0
        var overflow = Math.max(value - max, min - value)
        var rawOverflow = overflow
        if (overflow > 0) {
            if (joker) {
                joker = false
                overflow = 0.0
            }
        } else {
            joker = true
        }

        timeout -= time * overflow * rand
        if (timeout > timeoutReset) {
            timeout = timeoutReset
        }
        if (timeout < 0) {
            DP.println(DPType.SIM, "${javaClass.name} destroying ${destructable?.describe()} for being at value $value when max, min is $max, $min. Also, timeout: $timeout and overflow $rawOverflow")
            destructable?.destructImpl()
        }
    }

    fun disable() {
        this.max = 100000000.0
        this.min = -max
        this.timeoutReset = 10000000.0
    }

    fun reset() {
        boot = true
    }

    fun set(destructable: IDestructable) {
        this.destructable = destructable
    }
}
