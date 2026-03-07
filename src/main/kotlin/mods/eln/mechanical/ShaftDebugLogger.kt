package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import java.util.Collections
import java.util.IdentityHashMap

object ShaftDebugLogger {
    private var lastSecond = Long.MIN_VALUE
    private val dumpedNetworks = Collections.newSetFromMap(IdentityHashMap<ShaftNetwork, Boolean>())

    fun logElement(element: ShaftElement) {
        if (!Eln.debugEnabled) return

        val coordinate = element.coordonate()
        if (!coordinate.worldExist) return

        val world = coordinate.world()
        val second = world.totalWorldTime / 20L
        if (second != lastSecond) {
            lastSecond = second
            dumpedNetworks.clear()
            Eln.logger.info("[shaft-debug] ---- second={} dim={} ----", second, coordinate.dimension)
        }

        val uniqueNetworks = Collections.newSetFromMap(IdentityHashMap<ShaftNetwork, Boolean>())
        element.shaftConnectivity.forEach { side ->
            element.getShaft(side)?.let { uniqueNetworks.add(it) }
        }

        uniqueNetworks.forEach { network ->
            if (dumpedNetworks.add(network)) {
                dumpNetwork(network)
            }
        }
    }

    private fun dumpNetwork(network: ShaftNetwork) {
        val parts = network.parts.sortedWith(
            compareBy<ShaftPart>(
                { it.element.coordonate().dimension },
                { it.element.coordonate().x },
                { it.element.coordonate().y },
                { it.element.coordonate().z },
                { it.side.int }
            )
        )

        Eln.logger.info(
            "[shaft-debug] network id={} parts={} elements={} mass={} rads={} energy={}",
            System.identityHashCode(network),
            parts.size,
            network.elements.size,
            fmt(network.mass),
            fmt(network.rads),
            fmt(network.energy)
        )

        parts.forEach { part ->
            val coordinate = part.element.coordonate()
            val partNetwork = part.element.getShaft(part.side)
            Eln.logger.info(
                "[shaft-debug]   part coord={} side={} type={} ghost={} net={} partRads={} sides={} neighbours={}",
                formatCoordinate(coordinate),
                part.side,
                part.element.javaClass.simpleName,
                if (part.element is GhostShaftNode) "ghost" else "real",
                System.identityHashCode(partNetwork),
                fmt(partNetwork?.rads ?: 0.0),
                part.element.shaftConnectivity.joinToString(prefix = "[", postfix = "]"),
                describeNeighbours(part.element)
            )
        }
    }

    private fun describeNeighbours(element: ShaftElement): String {
        return element.shaftConnectivity.joinToString(prefix = "[", postfix = "]") { side ->
            val neighbour = findNeighbour(element, side)
            if (neighbour == null) {
                "${side}=none"
            } else {
                "${side}=${formatCoordinate(neighbour.coordonate())}:${neighbour.javaClass.simpleName}"
            }
        }
    }

    private fun findNeighbour(element: ShaftElement, side: Direction): ShaftElement? {
        val coordinate = Coordinate()
        coordinate.copyFrom(element.coordonate())
        coordinate.move(side)
        val node = NodeManager.instance?.getNodeFromCoordonate(coordinate) ?: return null
        val candidate = if (node is TransparentNode) {
            node.element as? ShaftElement
        } else {
            node as? ShaftElement
        } ?: return null

        return if (candidate.shaftConnectivity.any { it.inverse == side }) candidate else null
    }

    private fun formatCoordinate(coordinate: Coordinate): String {
        return "(${coordinate.x},${coordinate.y},${coordinate.z},dim=${coordinate.dimension})"
    }

    private fun fmt(value: Double): String = String.format("%.4f", value)
}
