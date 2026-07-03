package mods.eln.node

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ThermalConnection
import java.util.*

data class NodeConnectionEndpoint(
    val node: NodeBase,
    val side: Direction,
    val lrdu: LRDU,
    val element: Any?,
    val elementSide: Direction?,
    val elementLrdu: LRDU?
)

class NodeConnection(var N1: NodeBase, var dir1: Direction, var lrdu1: LRDU, var N2: NodeBase, var dir2: Direction, var lrdu2: LRDU) {
    var EC: MutableList<ElectricalConnection> = ArrayList()
    var TC: MutableList<ThermalConnection> = ArrayList()

    fun endpoint(isA: Boolean): NodeConnectionEndpoint {
        return if (isA) N1.getConnectionEndpoint(dir1, lrdu1) else N2.getConnectionEndpoint(dir2, lrdu2)
    }

    fun otherEndpoint(isA: Boolean): NodeConnectionEndpoint {
        return if (isA) N2.getConnectionEndpoint(dir2, lrdu2) else N1.getConnectionEndpoint(dir1, lrdu1)
    }

    fun destroy() {
        for (ec in EC) Eln.simulator.removeElectricalComponent(ec)
        for (tc in TC) Eln.simulator.removeThermalConnection(tc)
        N1.externalDisconnect(dir1, lrdu1)
        N2.externalDisconnect(dir2, lrdu2)
    }

    fun addConnection(ec: ElectricalConnection) {
        EC.add(ec)
    }

    fun addConnection(tc: ThermalConnection) {
        TC.add(tc)
    }
}
