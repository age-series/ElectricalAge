package mods.eln.node.partnode

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.Node
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad

/**
 * A node occupying a multipart coordinate.  Unlike [mods.eln.node.simple.SimpleNode],
 * one PartNode can own several independently simulated part elements.
 */
abstract class PartNode : Node() {
    private val elements = mutableListOf<PartNodeElement>()

    fun addElement(element: PartNodeElement) {
        elements += element
        reconnect()
    }

    fun removeElement(element: PartNodeElement) {
        elements -= element
        reconnect()
    }

    fun isEmpty() = elements.isEmpty()

    override fun connectJob() {
        super.connectJob()
        elements.forEach { it.connectJob() }
    }

    override fun disconnectJob() {
        elements.forEach { it.disconnectJob() }
        super.disconnectJob()
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int =
        elements.fold(0) { mask, element -> mask or element.getSideConnectionMask(side, lrdu) }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? =
        elements.firstNotNullOfOrNull { it.getElectricalLoad(side, lrdu, mask) }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? =
        elements.firstNotNullOfOrNull { it.getThermalLoad(side, lrdu, mask) }
}

/** Server-side simulation contribution made by one multipart at a PartNode coordinate. */
interface PartNodeElement {
    fun connectJob()
    fun disconnectJob()
    fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int
    fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad?
    fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad?
}
