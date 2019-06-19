package mods.eln.sim

import mods.eln.sim.mna.component.Bipole
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Line
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageStateLineReady

open class ElectricalLoad : VoltageStateLineReady() {

    init {
        name = "Electrical Load"
    }

    var rs = MnaConst.highImpedance
        set(Rs) {
            if (this.rs != Rs) {
                field = Rs
                for (c in connectedComponents) {
                    if (c is ElectricalConnection) {
                        c.notifyRsChange()
                    }
                }
            }
        }

    val i: Double
        get() {
            var i = 0.0
            for (c in connectedComponents) {
                if (c is Bipole && c !is Line)
                    i += Math.abs(c.getCurrent())
            }
            return i * 0.5
        }

    val current: Double
        get() = i

    fun highImpedance() {
        rs = MnaConst.highImpedance
    }

    companion object {
        val groundLoad: State? = null
    }
}
