package mods.eln.sixnode.signalinductor

import mods.eln.node.six.SixNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Inductor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.item.Item

class SignalInductorDescriptor(name: String?, var henri: Double, @JvmField var cable: ElectricalCableDescriptor) :
    SixNodeDescriptor(name, SignalInductorElement::class.java, SignalInductorRender::class.java) {
    var descriptor: String? = null

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
    }

    fun applyTo(load: ElectricalLoad?) {
        cable.applyTo(load)
    }

    fun applyTo(inductor: Inductor) {
        inductor.inductance = henri
    }
}
