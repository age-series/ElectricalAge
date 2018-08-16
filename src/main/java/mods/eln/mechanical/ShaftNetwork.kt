package mods.eln.mechanical

import mods.eln.misc.Coordonate
import mods.eln.misc.Direction
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import mods.eln.node.NodeManager
import mods.eln.sim.process.destruct.ShaftSpeedWatchdog
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.nbt.NBTTagCompound
import java.util.*


// Speed above which shafts will (by default) explode.
val absoluteMaximumShaftSpeed = 1000.0
// "Standard" drag, in J/t per rad.
val defaultDrag = 0.02
// Energy lost due to merging, proportional to *square* of delta speed ("friction")
val energyLostPerDeltaRad = 0.05

// Would merging these two networks cause an explosion?
fun wouldExplode(a: ShaftNetwork, b: ShaftNetwork): Boolean {
    return Math.abs(a.rads - b.rads) > (250.0 - 0.1 * Math.max(a.rads, b.rads))
}


/**
 * Represents the connection between all blocks that are part of the same shaft network.
 */
class ShaftNetwork() : INBTTReady {
    val parts = HashSet<ShaftPart>()
    val elements: HashSet<ShaftElement>
        get() {
            var ret = HashSet<ShaftElement>()
            parts.forEach { ret.add(it.element) }
            return ret
        }

    constructor(first: ShaftElement, side: Direction) : this() {
        parts.add(ShaftPart(first, side))
    }

    constructor(first: ShaftElement, sides: Iterator<Direction>) : this() {
        sides.forEach {
            parts.add(ShaftPart(first, it))
        }
    }

    // Aggregate properties of the (current) shaft:
    val shapeFactor = 0.5
    val mass: Double
        get() {
            var sum = 0.0
            for (e in elements) {
                sum += e.shaftMass
            }
            return sum
        }
    var _rads = 0.0
    var rads: Double
        get() = _rads
        set(v) {
            _rads = v
            afterSetRads()
        }
    var radsLastPublished = rads

    val joulePerRad: Double
        get() = mass * mass * shapeFactor / 2

    var energy: Double
        get() = joulePerRad * rads
        set(value) {
            rads = value / joulePerRad
        }

    fun afterSetRads() {
        if (_rads < 0) _rads = 0.0
        if (radsLastPublished > _rads * 1.05 || radsLastPublished < _rads * 0.95) {
            elements.forEach { it.needPublish() }
            radsLastPublished = _rads
        }
    }

    /**
     * Merge two shaft networks.
     *
     * @param other The shaft network to merge into this one. Destroyed.
     */
    fun mergeShafts(other: ShaftNetwork, invoker: ShaftElement?) {
        assert(other != this)

        val deltaRads = Math.abs(rads - other.rads)

        if(wouldExplode(this, other) && invoker != null) {
            WorldExplosion(invoker.coordonate()).machineExplosion().destructImpl()
            // Continue, however. The networks will unmerge when a component disappears, but assume they might not.
        }

        val newEnergy = energy + other.energy

        for (part in other.parts) {
            parts.add(part)
            part.element.setShaft(part.side, this)
        }
        other.parts.clear()

        energy = newEnergy - energyLostPerDeltaRad * deltaRads * deltaRads
    }

    /**
     * Connect a ShaftElement to a shaft network, merging any relevant adjacent networks.
     * @param from The ShaftElement that changed.
     */
    fun connectShaft(from: ShaftElement, side: Direction) {
        assert(ShaftPart(from, side) in parts)
        val neighbours = getNeighbours(from)
        for (neighbour in neighbours) {
            if(neighbour.thisShaft != this) {
                Utils.println("SN.cS: WARNING: Connecting part with this != getShaft(side)")
                continue
            }
            if (neighbour.otherShaft != null && neighbour.otherShaft != this) {
                mergeShafts(neighbour.otherShaft, from)

                // Inform the neighbour and the element itself that its shaft connectivity has changed.
                neighbour.makeConnection(this)
            }
        }
    }

    /**
     * Disconnect from a shaft network, because an element is dying.
     * @param from The IShaftElement that's going away.
     */
    fun disconnectShaft(from: ShaftElement, side: Direction) {
        // Inform all directly involved shafts about the change in connections.
        for (neighbour in getNeighbours(from)) {
            neighbour.breakConnection()
        }

        parts.remove(ShaftPart(from, side))
        // Going away momentarily, but...
        from.setShaft(side, ShaftNetwork(from, side))
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
        val unseen = HashSet<ShaftPart>(parts)
        val queue = HashMap<ShaftPart,ShaftNetwork>()
        val seen = HashSet<ShaftPart>()
        var shaft = this;
        Utils.println("SN.rN ----- START -----")
        while (unseen.size > 0) {
            shaft.parts.clear();
            // Do a breadth-first search from an arbitrary element.
            val start = unseen.iterator().next()
            unseen.remove(start);
            if(!(start in seen)) queue.put(start, shaft)
            while (queue.size > 0) {
                val next = queue.iterator().next()
                queue.remove(next.key);
                seen.add(next.key)
                shaft = next.value
                shaft.parts.add(next.key);
                next.key.element.setShaft(next.key.side, shaft)
                Utils.println("SN.rN visit next = " + next + ", queue.size = " + queue.size)
                for(side in next.key.element.shaftConnectivity) {
                    val part = ShaftPart(next.key.element, side)
                    if(!(part in seen))
                        queue.put(part, next.key.element.getShaft(side) ?: shaft)
                }
                val neighbours = getNeighbours(next.key.element)
                for (neighbour in neighbours) {
                    unseen.remove(neighbour.otherPart)
                    if(!(neighbour.otherPart in seen)) {
                        queue.put(neighbour.otherPart, neighbour.thisShaft!!)
                    }
                }
            }

            Utils.println("SN.rN new shaft, unseen.size = " + unseen.size)
            // We ran out of network. Any elements remaining in unseen should thus form a new network.
            shaft = ShaftNetwork()
        }

        Utils.println("SN.rN ----- FINISH -----")
    }

    private fun getNeighbours(from: ShaftElement): ArrayList<ShaftNeighbour> {
        val c = Coordonate()
        val ret = ArrayList<ShaftNeighbour>(6)
        for (dir in from.shaftConnectivity) {
            c.copyFrom(from.coordonate())
            c.move(dir)
            val to = NodeManager.instance!!.getTransparentNodeFromCoordinate(c)
            if (to is ShaftElement) {
                for (dir2 in to.shaftConnectivity) {
                    if (dir2.inverse == dir) {
                        ret.add(ShaftNeighbour(
                            ShaftPart(from, dir),
                            from.getShaft(dir),
                            dir,
                            ShaftPart(to, dir2),
                            to.getShaft(dir2)
                        ))
                        break
                    }
                }
            }
        }
        return ret
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String?) {
        rads = nbt.getFloat(str + "rads").toDouble()
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String?) {
        nbt.setFloat(str + "rads", rads.toFloat())
    }

}

interface ShaftElement {
    val shaftMass: Double
    val shaftConnectivity: Array<Direction>
    fun coordonate(): Coordonate
    fun getShaft(dir: Direction): ShaftNetwork?
    fun setShaft(dir: Direction, net: ShaftNetwork?)
    fun isInternallyConnected(a: Direction, b: Direction): Boolean = true

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
    fun makeConnection(shaft: ShaftNetwork) {
        thisPart.element.setShaft(thisPart.side, shaft)
        otherPart.element.setShaft(otherPart.side, shaft)
        thisPart.element.connectedOnSide(thisPart.side, shaft)
        otherPart.element.connectedOnSide(otherPart.side, shaft)
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
