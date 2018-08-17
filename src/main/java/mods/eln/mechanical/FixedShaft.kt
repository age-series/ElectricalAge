package mods.eln.mechanical

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class FixedShaftDescriptor(name: String, override val obj: Obj3D) : SimpleShaftDescriptor(
    name, FixedShaftElement::class, ShaftRender::class, EntityMetaTag.Basic
) {
    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Shaft"))
    override val rotating = emptyArray<Obj3D.Obj3DPart>()

    override fun draw(angle: Double) {
        static.forEach { it.draw() }
    }
}

class FixedShaftElement(node: TransparentNode, desc_: TransparentNodeDescriptor) : SimpleShaftElement(node, desc_) {
    override val shaftMass = 10.0

    inner class FixedShaftProcess : IProcess {
        override fun process(time: Double) {
            shaft.rads = 0.0
        }
    }

    val process = FixedShaftProcess()

    init {
        slowProcessList.add(process)
    }

    override fun thermoMeterString(side: Direction?): String? = null
    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null
    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? = null
    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int = 0
    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean = false
}
