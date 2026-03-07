package mods.eln.api.v1.electrical

import mods.eln.node.NodeBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectricalIntegrationSignalBusMasksTest {
    @Test
    fun encodesAndDecodesBusChannelMasks() {
        val redMask = ElectricalIntegration.SignalBusMasks.channelMask(ElectricalIntegration.SignalBusChannel.RED)
        assertEquals(
            ElectricalIntegration.SignalBusChannel.RED,
            ElectricalIntegration.SignalBusMasks.readChannel(redMask)
        )
        assertTrue(redMask and NodeBase.maskElectricalGate != 0)
    }

    @Test
    fun wildcardMaskAdvertisesBusWithoutPinningColor() {
        val wildcard = ElectricalIntegration.SignalBusMasks.wildcardMask()
        assertTrue(wildcard and NodeBase.maskElectricalGate != 0)
        assertEquals(
            ElectricalIntegration.SignalBusChannel.WHITE,
            ElectricalIntegration.SignalBusMasks.readChannel(wildcard)
        )
    }

    @Test
    fun busMasksMatchNativeSignalBusCableMaskFamily() {
        assertEquals(NodeBase.maskElectricalGate, ElectricalIntegration.SignalBusMasks.BUS)
        assertEquals(ElectricalIntegration.ElectricalMasks.GATE, ElectricalIntegration.ElectricalMasks.SIGNAL_BUS)
    }
}
