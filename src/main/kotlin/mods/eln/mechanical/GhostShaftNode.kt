package mods.eln.mechanical

import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.GhostNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack

/**
 * Ghost-backed shaft endpoint that delegates its mechanics to a real ShaftElement owner.
 *
 * The coordinate and facing are derived from the owner's placement coordinate and front,
 * allowing multiblock descriptors to expose shaft connections away from the origin block.
 */
class GhostShaftNode(
    origin: Coordinate,
    front: Direction,
    offset: Coordinate,
    private val owner: ShaftElement,
    private val ownerSide: Direction,
    localFacing: Direction
) : GhostNode(), ShaftElement {

    private val target = Coordinate(offset).apply {
        applyTransformation(front, origin)
        dimension = origin.dimension
    }

    private val facing = rotateFacing(front, localFacing)

    private var connectionSide: Direction = facing
    private var shaft: ShaftNetwork = ShaftNetwork()

    fun placeGhost() {
        onBlockPlacedBy(target, facing, null, null)
    }

    /**
     * After [initialize] the owner's shaft network must be merged so that this ghost node
     * shares the same ShaftNetwork instance.
     */
    fun attachToOwnerNetwork() {
        val ownerNet = owner.getShaft(ownerSide) ?: return
        if (shaft !== ownerNet) {
            ownerNet.mergeShafts(shaft, owner)
            shaft = ownerNet
        }
    }

    override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        connectionSide = front
        shaft = ShaftNetwork(this, shaftConnectivity.iterator())
        shaft.connectShaft(this, connectionSide)
        connect()
    }

    override fun initializeFromNBT() {}

    override val shaftMass = 0.0

    override val shaftConnectivity: Array<Direction>
        get() = arrayOf(connectionSide)

    override fun coordonate(): Coordinate = coordinate

    override fun getShaft(dir: Direction): ShaftNetwork? = if (dir == connectionSide) shaft else null

    override fun setShaft(dir: Direction, net: ShaftNetwork?) {
        if (dir == connectionSide && net != null) {
            shaft = net
        }
    }

    override fun connectedOnSide(direction: Direction, net: ShaftNetwork) {
        if (direction == connectionSide) {
            owner.connectedOnSide(ownerSide, net)
            owner.needPublish()
        }
    }

    override fun disconnectedOnSide(direction: Direction, net: ShaftNetwork?) {
        if (direction == connectionSide) {
            owner.disconnectedOnSide(ownerSide, net)
            owner.needPublish()
        }
    }

    override fun onBreakBlock() {
        shaft.disconnectShaft(this)
        super.onBreakBlock()
        owner.needPublish()
    }

    override fun isShaftElementDestructing(): Boolean {
        return this.isDestructing || owner.isShaftElementDestructing()
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int = 0

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? = null

    private fun rotateFacing(front: Direction, local: Direction): Direction {
        val vec = when (local) {
            Direction.XN -> intArrayOf(-1, 0, 0)
            Direction.XP -> intArrayOf(1, 0, 0)
            Direction.YN -> intArrayOf(0, -1, 0)
            Direction.YP -> intArrayOf(0, 1, 0)
            Direction.ZN -> intArrayOf(0, 0, -1)
            Direction.ZP -> intArrayOf(0, 0, 1)
        }
        front.rotateFromXN(vec)
        return when {
            vec[0] > 0 -> Direction.XP
            vec[0] < 0 -> Direction.XN
            vec[1] > 0 -> Direction.YP
            vec[1] < 0 -> Direction.YN
            vec[2] > 0 -> Direction.ZP
            else -> Direction.ZN
        }
    }
}
