package mods.eln.api.v1.electrical

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
class ElectricalIntegrationKotlinApiSurfaceTest {
    @Test
    fun hostedBlockNodeFactoryAcceptsGeneralBlockFaceConnections() {
        val bus = ElectricalIntegration.SignalBusLoad("test.kotlin.bus") {}
        val statusLoad = ElectricalIntegration.Load("test.kotlin.status") {}
        val faceConnections: Map<ElectricalIntegration.BlockFace, ElectricalIntegration.BlockFaceConnection> = mapOf(
            ElectricalIntegration.BlockFace.XN to ElectricalIntegration.SignalBusFaceConnection(bus),
            ElectricalIntegration.BlockFace.YP to ElectricalIntegration.FaceConnection(statusLoad, ElectricalIntegration.ElectricalMasks.GATE)
        )

        val createCall: () -> ElectricalIntegration.BlockNode = {
            ElectricalIntegration.createHostedBlockNode(
                dimension = 0,
                x = 1,
                y = 2,
                z = 3,
                front = ElectricalIntegration.BlockFace.ZP,
                faceConnections = faceConnections
            )
        }

        assertEquals(2, faceConnections.size)
        assertEquals(0, createCall.javaClass.enclosingMethod?.parameterCount ?: 0)
        assertTrue(faceConnections[ElectricalIntegration.BlockFace.XN] is ElectricalIntegration.SignalBusFaceConnection)
        assertTrue(faceConnections[ElectricalIntegration.BlockFace.YP] is ElectricalIntegration.FaceConnection)
    }

    @Test
    fun objectStyleAccessAndOrderedChannelsAreStableFromKotlin() {
        val ordered = ElectricalIntegration.SignalBusChannel.ordered()
        assertEquals(ElectricalIntegration.SignalBusChannel.WHITE, ordered.first())
        assertEquals(ElectricalIntegration.SignalBusChannel.BLACK, ordered.last())
        assertEquals(ElectricalIntegration.getSignalVoltageLevel(), ElectricalIntegration.SignalLevels.MAX_VOLTAGE, 1e-12)
    }
}
