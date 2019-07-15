package mods.eln.sim

import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.sim.mna.component.InterSystem

class ElectricalConnection(internal var L1: ElectricalLoad, internal var L2: ElectricalLoad) : InterSystem() {

    init {
        if (L1 === L2) DP.println(DPType.MNA, "WARNING: Attempt to connect load to itself?")
    }

    fun notifyRsChange() {
        r = (aPin as ElectricalLoad).rs + (bPin as ElectricalLoad).rs
    }

    override fun onAddToRootSystem() {
        this.connectTo(L1, L2)
        notifyRsChange()
    }

    override fun onRemovefromRootSystem() {
        this.breakConnection()
    }
}
