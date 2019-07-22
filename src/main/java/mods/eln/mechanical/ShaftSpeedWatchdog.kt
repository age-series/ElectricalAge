package mods.eln.mechanical

import mods.eln.sim.core.ValueWatchdog

class ShaftSpeedWatchdog(val shaftElement: ShaftElement, max: Double) : ValueWatchdog() {

    init {
        this.max = max
    }

    override fun getValue(): Double? {
        var max = 0.0
        shaftElement.shaftConnectivity.forEach {
            val shaft = shaftElement.getShaft(it)
            if(shaft != null) max = Math.max(max, shaft.rads)
        }
        return max
    }
}
