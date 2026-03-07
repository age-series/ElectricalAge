package mods.eln.api.v1.electrical

import kotlin.test.Test
import kotlin.test.assertEquals

class ElectricalIntegrationRelativeBlockFaceTest {
    @Test
    fun resolvesRelativeFacesAgainstHorizontalFront() {
        assertEquals(
            ElectricalIntegration.BlockFace.XN,
            ElectricalIntegration.RelativeBlockFace.LEFT.resolve(ElectricalIntegration.BlockFace.ZP)
        )
        assertEquals(
            ElectricalIntegration.BlockFace.XP,
            ElectricalIntegration.RelativeBlockFace.RIGHT.resolve(ElectricalIntegration.BlockFace.ZP)
        )
        assertEquals(
            ElectricalIntegration.BlockFace.YN,
            ElectricalIntegration.RelativeBlockFace.DOWN.resolve(ElectricalIntegration.BlockFace.ZP)
        )
    }

    @Test
    fun resolvesRelativeFacesAgainstVerticalFront() {
        assertEquals(
            ElectricalIntegration.BlockFace.XP,
            ElectricalIntegration.RelativeBlockFace.UP.resolve(ElectricalIntegration.BlockFace.YP)
        )
        assertEquals(
            ElectricalIntegration.BlockFace.XN,
            ElectricalIntegration.RelativeBlockFace.DOWN.resolve(ElectricalIntegration.BlockFace.YP)
        )
        assertEquals(
            ElectricalIntegration.BlockFace.ZP,
            ElectricalIntegration.RelativeBlockFace.RIGHT.resolve(ElectricalIntegration.BlockFace.YP)
        )
    }
}
