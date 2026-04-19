package mods.eln.node

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeBaseSignalMaskTest {
    @Test
    fun signalInputAndOutputMasksAcceptPowerAndSignalCables() {
        assertEquals(NodeBase.maskElectricalAll, NodeBase.maskElectricalInputGate)
        assertEquals(NodeBase.maskElectricalAll, NodeBase.maskElectricalOutputGate)
    }
}
