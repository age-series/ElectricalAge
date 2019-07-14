package mods.eln.sim.nbt

import mods.eln.Eln
import mods.eln.misc.Utils

class NbtElectricalGateOutput(name: String) : NbtElectricalLoad(name) {

    init {
        Eln.signalCableDescriptor.applyTo(this)
    }

    fun plot(str: String): String {
        return Utils.plotSignal(u, current)
    }
}
