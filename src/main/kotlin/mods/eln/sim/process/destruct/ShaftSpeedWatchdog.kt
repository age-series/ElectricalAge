package mods.eln.sim.process.destruct

import mods.eln.mechanical.ShaftElement

class ShaftSpeedWatchdog(private val shaftElement: ShaftElement, max: Double) : ValueWatchdog() {
    override val watchdogType = WatchdogType.SHAFT_SPEED

    init {
        this.max = max
    }

    override fun getValue(): Double {
        var max = 0.0
        shaftElement.shaftConnectivity.forEach {
            val shaft = shaftElement.getShaft(it)
            if(shaft != null) max = Math.max(max, shaft.rads)
        }
        return max
    }
}
