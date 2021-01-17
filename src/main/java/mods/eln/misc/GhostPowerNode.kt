package mods.eln.misc

import mods.eln.node.GhostNode
import mods.eln.node.NodeBase
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

class GhostPowerNode(origin: Coordinate, front: Direction, offset: Coordinate, val load: ElectricalLoad, val mask: Int = NodeBase.maskElectricalPower): GhostNode() {

    val coord = Coordinate(offset).apply {
        applyTransformation(front, origin)
        dimension = origin.dimension
    }

    fun initialize() {
        onBlockPlacedBy(coord, Direction.XN, null, null)
    }

    override fun initializeFromThat(front: Direction?, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        connect()
    }

    override fun initializeFromNBT() {}

    override fun getSideConnectionMask(directionA: Direction, lrduA: LRDU) = mask

    override fun getThermalLoad(directionA: Direction, lrduA: LRDU, mask: Int): ThermalLoad? = null

    override fun getElectricalLoad(directionB: Direction, lrduB: LRDU, mask: Int): ElectricalLoad = load
}
