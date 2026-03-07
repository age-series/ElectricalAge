package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.GhostNode
import mods.eln.node.NodeManager
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
    private val observerCoordinate = Coordinate(origin)
    private val ghostUuid = Utils.uuid

    private val target = Coordinate(offset).apply {
        applyTransformation(front, origin)
        dimension = origin.dimension
    }

    private val facing = Direction.rotateLocalDirection(front, localFacing)

    private var connectionSide: Direction = facing
    private var shaft: ShaftNetwork = ShaftNetwork()

    val ownerConnectionSide: Direction
        get() = ownerSide

    val ghostConnectionSide: Direction
        get() = connectionSide

    fun sharesOwnerWith(other: GhostShaftNode): Boolean = owner === other.owner

    fun placeGhost() {
        // Temporary shaft placement logging.
        // Utils.println(
        //     "GhostShaft.place: target=%s facing=%s owner=%s ownerSide=%s ghostUuid=%d",
        //     target,
        //     facing,
        //     owner.javaClass.simpleName,
        //     ownerSide,
        //     ghostUuid
        // )
        val existingNode = NodeManager.instance?.getNodeFromCoordonate(target)
        if (existingNode is GhostShaftNode) {
            // Utils.println("GhostShaft.place: removing stale ghost shaft at %s before re-placement", target)
            existingNode.onBreakBlock()
        }
        Eln.ghostManager.createGhost(target, observerCoordinate, ghostUuid)
        onBlockPlacedBy(target, facing, null, null)
    }

    /**
     * After [initialize] the owner's shaft network must be merged so that this ghost node
     * shares the same ShaftNetwork instance.
     */
    fun attachToOwnerNetwork() {
        val ownerNet = owner.getShaft(ownerSide) ?: return
        // Utils.println(
        //     "GhostShaft.attach: ghostCoord=%s owner=%s ownerSide=%s ghostNet=%d ownerNet=%d ghostRads=%f ownerRads=%f",
        //     target,
        //     owner.javaClass.simpleName,
        //     ownerSide,
        //     System.identityHashCode(shaft),
        //     System.identityHashCode(ownerNet),
        //     shaft.rads,
        //     ownerNet.rads
        // )
        if (shaft !== ownerNet) {
            ownerNet.mergeShafts(shaft, owner)
            shaft = ownerNet
        }
    }

    override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
        connectionSide = front
        shaft = ShaftNetwork(this, shaftConnectivity.iterator())
        // Utils.println("GhostShaft.init: coord=%s connectionSide=%s net=%d", coordinate, connectionSide, System.identityHashCode(shaft))
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
        // Utils.println("GhostShaft.break: coord=%s net=%d rads=%f", coordinate, System.identityHashCode(shaft), shaft.rads)
        shaft.disconnectShaft(this)
        super.onBreakBlock()
        owner.needPublish()
    }

    override fun isShaftElementDestructing(): Boolean {
        return this.isDestructing || owner.isShaftElementDestructing()
    }

    override fun linkedShaftParts(fromSide: Direction): Iterable<ShaftPart> {
        return if (fromSide == connectionSide) listOf(ShaftPart(owner, ownerSide)) else emptyList()
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int = 0

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? = null

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? = null

}
