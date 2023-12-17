package mods.eln.sixnode.signalinductor

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.plotAmpere
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Inductor
import mods.eln.sim.nbt.NbtElectricalLoad

class SignalInductorElement(sixNode: SixNode?, side: Direction?, descriptor: SixNodeDescriptor) : SixNodeElement(
    sixNode!!, side!!, descriptor
) {
    var descriptor: SignalInductorDescriptor
    var postiveLoad: NbtElectricalLoad = NbtElectricalLoad("postiveLoad")
    var negativeLoad: NbtElectricalLoad = NbtElectricalLoad("negativeLoad")
    var inductor: Inductor = Inductor("inductor", postiveLoad, negativeLoad)

    init {
        electricalLoadList.add(postiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalComponentList.add(inductor)
        postiveLoad.setAsMustBeFarFromInterSystem()
        this.descriptor = descriptor as SignalInductorDescriptor
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (front == lrdu) return postiveLoad
        if (front.inverse() == lrdu) return negativeLoad
        return null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (front == lrdu) return descriptor.cable.nodeMask
        if (front.inverse() == lrdu) return descriptor.cable.nodeMask
        return 0
    }

    override fun multiMeterString(): String {
        return plotAmpere("I", inductor.current)
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun initialize() {
        descriptor.applyTo(negativeLoad)
        descriptor.applyTo(postiveLoad)
        descriptor.applyTo(inductor)
    }
}
