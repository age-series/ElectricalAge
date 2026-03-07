package mods.eln.mechanical

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.LoaderState
import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.process.destruct.DelayedDestruction
import mods.eln.sim.process.destruct.ShaftSpeedWatchdog
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.nbt.NBTTagCompound
import java.util.*

// Speed above which shafts will (by default) explode.
val absoluteMaximumShaftSpeed = 250.0
// "Standard" drag, in J/t per rad.
val defaultDrag = 0.02
// Energy lost due to merging, proportional to *square* of delta speed ("friction")
val energyLostPerDeltaRad = 0.05

// Would merging these two networks cause an explosion?
fun wouldExplode(a: ShaftNetwork, b: ShaftNetwork): Boolean {
    return Math.abs(a.rads - b.rads) > (50.0 - 0.1 * Math.max(a.rads, b.rads))
}


/**
 * Represents the connection between all blocks that are part of the same shaft network.
 */
open class ShaftNetwork() : INBTTReady {
    val parts = HashSet<ShaftPart>()
    var elements = HashSet<ShaftElement>()
    var _mass = 0.0
    // This has to remain overridable/RO because of things like the fixed shaft  - Grissess
    open val mass: Double
        get() {
            return if (_mass.isFinite()) _mass else 0.0
        }
    fun updateCache() {
        pruneInvalidParts()
        elements.clear()
        _mass = 0.0
        parts.forEach { elements.add(it.element) }
        elements.forEach { _mass += it.shaftMass }
    }

    constructor(first: ShaftElement, side: Direction) : this() {
        parts.add(ShaftPart(first, side))
        first.setShaft(side, this)
        updateCache()
    }

    constructor(first: ShaftElement, sides: Iterator<Direction>) : this() {
        sides.forEach {
            parts.add(ShaftPart(first, it))
            first.setShaft(it, this)
        }
        updateCache()
    }

    constructor(other: ShaftNetwork) : this() {
        takeAll(other)
    }

    fun takeAll(other: ShaftNetwork) {
        other.parts.forEach { it.element.setShaft(it.side, this) }
        parts.addAll(other.parts)
        other.parts.clear()
        updateCache()
    }

    // Aggregate properties of the (current) shaft:
    var _rads = 0.0
    open var rads: Double
        get() = if (_rads.isFinite()) _rads else 0.0
        set(v) {
            if (v.isFinite())
                _rads = v
            afterSetRads()
        }
    var radsLastPublished = rads

    var energy: Double
        get() = if (mass.isFinite() && rads.isFinite()) mass * rads * rads * 0.5 * Eln.shaftEnergyFactor else 0.0
        set(value) {
            if(value < 0 || !value.isFinite())
                rads = 0.0
            else
                rads = Math.sqrt(2 * value / ((if(mass.isFinite()) mass else 0.0) * Eln.shaftEnergyFactor))
        }

    fun afterSetRads() {
        if (_rads < 0) _rads = 0.0
        if (radsLastPublished > _rads * 1.05 || radsLastPublished < _rads * 0.95) {
            elements.forEach { it.needPublish() }
            radsLastPublished = _rads
        }
    }

    open fun hasMergePrecedenceOver(other: ShaftNetwork) = false

    /**
     * Merge two shaft networks.
     *
     * @param other The shaft network to merge into this one. Destroyed.
     */
    fun mergeShafts(other: ShaftNetwork, invoker: ShaftElement?): ShaftNetwork {
        assert(other != this)
        // Temporary merge/rebuild logging.
        // Utils.println(
        //     "SN.ms: merge this=%s(id=%d,r=%f,parts=%d) other=%s(id=%d,r=%f,parts=%d) invoker=%s",
        //     this,
        //     System.identityHashCode(this),
        //     rads,
        //     parts.size,
        //     other,
        //     System.identityHashCode(other),
        //     other.rads,
        //     other.parts.size,
        //     invoker
        // )

        // If the other class wants to take this merge, let it.
        // In particular, don't presume that:
        // (1) setShaft won't be called on the invoker during the merge, and
        // (2) that the invoker will have the same shaft afterward
        if(other.hasMergePrecedenceOver(this)) {
            Utils.println(String.format("SN.mS: merge prec %s over %s", other, this))
            return other.mergeShafts(this, invoker)
        }

        /* XXX (Grissess): While loading the map, shaft networks are repeatedly
        merged and deserialized, causing them to lose energy just as if the
        components were newly added. This can cause, in the worst case, saved
        networks to explode on load. Although a bit of a hack, asking the FML
        Loader about which state it's in seems to be the best workaround. At
        some point, consider serializing network connectivity properly.
         */

        val loadMerge = Loader.instance().loaderState == LoaderState.SERVER_ABOUT_TO_START
        // val loadMerge = false
        // Utils.println("SN.mS: state " + Loader.instance().loaderState.name)

        // Utils.println(String.format("SN.mS: Merging %s r=%f e=%f, %s r=%f e=%f, loading=%s", this, rads, energy, other, other.rads, other.energy, loadMerge))

        var deltaRads = 0.0
        var newEnergy = 0.0
        if(!loadMerge) {
            deltaRads = Math.abs(rads - other.rads)

            if (wouldExplode(this, other) && invoker != null) {
                Utils.println(String.format("SN.mS: Bad matching, %s will explode", invoker))
                DelayedDestruction(
                    WorldExplosion(invoker).machineExplosion(),
                    0.0  // Sooner than later, just not right now :)
                )
                // Continue, however. The networks will unmerge when a component disappears, but assume they might not.
            }

            newEnergy = energy + other.energy
        }


        for (part in other.parts) {
            parts.add(part)
            part.element.setShaft(part.side, this)
        }
        other.parts.clear()
        updateCache()
        other.updateCache()

        if(!loadMerge) {
            energy = newEnergy - energyLostPerDeltaRad * deltaRads * deltaRads
        }

        // Utils.println(
        //     "SN.ms: result survivor=%d rads=%f energy=%f parts=%d elements=%d",
        //     System.identityHashCode(this),
        //     rads,
        //     energy,
        //     parts.size,
        //     elements.size
        // )

        // Utils.println(String.format("SN.mS: Result %s r=%f e=%f", this, rads, energy))

        // Return the survivor
        return this
    }

    /**
     * Connect a ShaftElement to a shaft network, merging any relevant adjacent networks.
     * @param from The ShaftElement that changed.
     */
    fun connectShaft(from: ShaftElement, side: Direction) {
        assert(ShaftPart(from, side) in parts)
        pruneInvalidParts()
        // Utils.println(
        //     "SN.cS: element=%s coord=%s side=%s net=%d rads=%f",
        //     from.javaClass.simpleName,
        //     from.coordonate(),
        //     side,
        //     System.identityHashCode(this),
        //     rads
        // )
        val neighbours = getNeighbours(from)
        for (neighbour in neighbours) {
            if(neighbour.thisShaft != this) {
                Utils.println("SN.cS: WARNING: Connecting part with this != getShaft(side)")
                continue
            }
            if (neighbour.otherShaft != null && neighbour.otherShaft != this) {
                mergeShafts(neighbour.otherShaft, from)

                // Inform the neighbour and the element itself that its shaft connectivity has changed.
                neighbour.makeConnection()
            }
        }
    }

    /**
     * Disconnect from a shaft network, because an element is dying.
     * @param from The IShaftElement that's going away.
     */
    fun disconnectShaft(from: ShaftElement) {
        pruneInvalidParts()
        // Utils.println(
        //     "SN.dS: element=%s coord=%s net=%d rads=%f partsBefore=%d",
        //     from.javaClass.simpleName,
        //     from.coordonate(),
        //     System.identityHashCode(this),
        //     rads,
        //     parts.size
        // )
        // Inform all directly involved shafts about the change in connections.
        for (neighbour in getNeighbours(from)) {
            if(neighbour.thisShaft == this) {
                neighbour.breakConnection()
                // Going away momentarily, but...
                from.setShaft(neighbour.thisPart.side, ShaftNetwork(from, neighbour.thisPart.side))
            }
        }

        parts.removeIf {
            it.element == from
        }

        // This may have split the network.
        // At the moment there's no better way to figure this out than by exhaustively walking it to check for
        // partitions. Basically fine, as they don't get very large, but a possible target for optimization later on.
        rebuildNetwork()

    }

    /**
     * Walk the entire network, splitting as necessary.
     * Yes, this makes breaking a shaft block O(n). Not a problem right now.
     */
    internal fun rebuildNetwork() {
        pruneInvalidParts()
        // Utils.println(
        //     "SN.rN: rebuild start net=%d rads=%f parts=%d",
        //     System.identityHashCode(this),
        //     rads,
        //     parts.size
        // )
        val unseen = HashSet<ShaftPart>(parts)
        val queue = ArrayDeque<ShaftPart>()
        val seen = HashSet<ShaftPart>()
        val curRads = if(rads.isNaN()) 0.0 else rads
        var shaft = ShaftNetwork()
        shaft.rads = curRads
        // Utils.println("SN.rN ----- START -----")
        while (unseen.size > 0) {
            shaft.parts.clear();
            // Do a breadth-first search from an arbitrary element.
            val start = unseen.iterator().next()
            unseen.remove(start);
            if(!(start in seen)) queue.add(start)
            while (queue.size > 0) {
                val next = queue.removeFirst()
                unseen.remove(next)
                if (!seen.add(next)) continue
                if (!isResolvableShaftElement(next.element)) continue
                if(next.element.isShaftElementDestructing()) continue
                shaft.parts.add(next)
                next.element.setShaft(next.side, shaft)
                // Utils.println("SN.rN visit next = " + next + ", queue.size = " + queue.size)
                for(side in next.element.shaftConnectivity) {
                    if (side == next.side || !next.element.isInternallyConnected(next.side, side)) continue
                    val part = ShaftPart(next.element, side)
                    if(!(part in seen)) {
                        queue.add(part)
                    }
                }
                for (linked in next.element.linkedShaftParts(next.side)) {
                    if (!isResolvableShaftElement(linked.element)) continue
                    if (!(linked in seen)) {
                        queue.add(linked)
                    }
                }
                val neighbours = getNeighbours(next.element)
                for (neighbour in neighbours) {
                    unseen.remove(neighbour.otherPart)
                    if(!(neighbour.otherPart in seen)) {
                        queue.add(neighbour.otherPart)
                    }
                }
            }

            // Utils.println("SN.rN new shaft, unseen.size = " + unseen.size)
            // We ran out of network. Any elements remaining in unseen should thus form a new network.
            shaft.updateCache()
            // Utils.println(
            //     "SN.rN: partition net=%d rads=%f parts=%d elements=%d",
            //     System.identityHashCode(shaft),
            //     shaft.rads,
            //     shaft.parts.size,
            //     shaft.elements.size
            // )
            shaft.elements.forEach { it.needPublish() }
            shaft = ShaftNetwork()
            shaft.rads = curRads
        }

        // Before we exit, make sure the last shaft constructed has its cache rebuilt.
        shaft.updateCache()
        // Utils.println(
        //     "SN.rN: rebuild end trailingNet=%d rads=%f parts=%d elements=%d",
        //     System.identityHashCode(shaft),
        //     shaft.rads,
        //     shaft.parts.size,
        //     shaft.elements.size
        // )
        shaft.elements.forEach { it.needPublish() }

        // At this point, it's likely that `this` is no longer referenced by
        // any object and will be dropped on return.

        // Utils.println("SN.rN ----- FINISH -----")
    }

    private fun getNeighbours(from: ShaftElement): ArrayList<ShaftNeighbour> {
        val c = Coordinate()
        val ret = ArrayList<ShaftNeighbour>(6)
        if (!isResolvableShaftElement(from)) return ret
        for (dir in from.shaftConnectivity) {
            c.copyFrom(from.coordonate())
            c.move(dir)
            val candidate = findShaftElementAt(c)
            if (candidate != null && isResolvableShaftElement(candidate)) {
                if (from is GhostShaftNode && candidate is GhostShaftNode && from.sharesOwnerWith(candidate)) continue
                for (dir2 in candidate.shaftConnectivity) {
                    if (dir2.inverse == dir) {
                        ret.add(
                            ShaftNeighbour(
                                ShaftPart(from, dir),
                                from.getShaft(dir),
                                dir,
                                ShaftPart(candidate, dir2),
                                candidate.getShaft(dir2)
                            )
                        )
                        break
                    }
                }
            }
        }
        return ret
    }

    private fun isResolvableShaftElement(element: ShaftElement): Boolean {
        val resolved = findShaftElementAt(element.coordonate())
        return resolved == null || resolved === element
    }

    private fun pruneInvalidParts() {
        val iterator = parts.iterator()
        while (iterator.hasNext()) {
            val part = iterator.next()
            if (!isResolvableShaftElement(part.element) || part.element.getShaft(part.side) !== this) {
                iterator.remove()
            }
        }
    }

    private fun findShaftElementAt(coordinate: Coordinate): ShaftElement? {
        val node = NodeManager.instance?.getNodeFromCoordonate(coordinate) ?: return null
        if (node is TransparentNode) {
            val element = node.element
            if (element is ShaftElement) {
                return element
            }
        }
        return if (node is ShaftElement) node else null
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        rads = nbt.getFloat(str + "rads").toDouble()
        if(!rads.isFinite()) rads = 0.0
        // Utils.println(String.format("SN.rFN: load %s r=%f", this, rads))
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setFloat(str + "rads", rads.toFloat())
        // Utils.println(String.format("SN.wTN: save %s r=%f", this, rads))
    }

}

class StaticShaftNetwork() : ShaftNetwork() {

    constructor(elem: ShaftElement, dirs: Iterator<Direction>) : this() {
        dirs.forEach {
            parts.add(ShaftPart(elem, it))
            elem.setShaft(it, this)
        }
    }
    var fixedRads = 0.0

    override var rads
        get() = fixedRads
        set(_) {}

    // XXX This shouldn't matter...
    override val mass: Double
        get() = 1000.0

    override fun hasMergePrecedenceOver(other: ShaftNetwork) = other !is StaticShaftNetwork
}

interface ShaftElement {
    val shaftMass: Double
    val shaftConnectivity: Array<Direction>
    fun coordonate(): Coordinate
    fun getShaft(dir: Direction): ShaftNetwork?
    fun setShaft(dir: Direction, net: ShaftNetwork?)
    fun isInternallyConnected(a: Direction, b: Direction): Boolean = true
    fun isShaftElementDestructing(): Boolean
    fun linkedShaftParts(fromSide: Direction): Iterable<ShaftPart> = emptyList()

    fun initialize() {
        shaftConnectivity.forEach {
            val shaft = getShaft(it)
            if(shaft != null) shaft.connectShaft(this, it)
        }
    }

    fun needPublish()

    fun connectedOnSide(direction: Direction, net: ShaftNetwork) {}

    fun disconnectedOnSide(direction: Direction, net: ShaftNetwork?) {}
}

fun createShaftWatchdog(shaftElement: ShaftElement): ShaftSpeedWatchdog {
    return ShaftSpeedWatchdog(shaftElement, absoluteMaximumShaftSpeed)
}

data class ShaftPart(
    val element: ShaftElement,
    val side: Direction
)

data class ShaftNeighbour(
    val thisPart: ShaftPart,
    val thisShaft: ShaftNetwork?,
    val side: Direction,
    val otherPart: ShaftPart,
    val otherShaft: ShaftNetwork?
) {
    fun makeConnection() {
        val thisNet = thisPart.element.getShaft(thisPart.side)
        val otherNet = otherPart.element.getShaft(otherPart.side)
        if(thisNet != otherNet) Utils.println("ShaftNeighbour.makeConnection: WARNING: Not actually connected?")
        thisPart.element.connectedOnSide(thisPart.side, thisNet!!)
        otherPart.element.connectedOnSide(otherPart.side, otherNet!!)
    }

    fun breakConnection() {
        val thisNet = thisPart.element.getShaft(thisPart.side)
        val otherNet = otherPart.element.getShaft(otherPart.side)
        if(thisNet != otherNet) Utils.println("ShaftNeighbour.breakConnection: WARNING: Break already broken connection?")
        thisPart.element.disconnectedOnSide(thisPart.side, thisNet)
        otherPart.element.disconnectedOnSide(otherPart.side, otherNet)
        // TODO: Unmerge networks here eventually?
    }
}
