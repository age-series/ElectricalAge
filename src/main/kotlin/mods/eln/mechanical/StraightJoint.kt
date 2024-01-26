package mods.eln.mechanical

import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.player.EntityPlayer

open class StraightJointDescriptor(baseName: String, obj: Obj3D) : SimpleShaftDescriptor(baseName,
    StraightJointElement::class, ShaftRender::class, EntityMetaTag.Basic) {
    override val obj = obj
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Shaft"))
}

open class StraightJointElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    override val shaftMass = 0.5

    override fun getWaila(): Map<String, String> {
        var info = mutableMapOf<String, String>()
        info.put(tr("Speed"), Utils.plotRads("", shaft.rads))
        info.put(tr("Energy"), Utils.plotEnergy("", shaft.energy))
        return info
    }

    override fun coordonate(): Coordinate {
        return node!!.coordinate
    }
}
