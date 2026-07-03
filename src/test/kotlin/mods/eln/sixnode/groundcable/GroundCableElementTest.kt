package mods.eln.sixnode.groundcable

import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableElement
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableElement
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCablePalette
import net.minecraft.nbt.NBTTagCompound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Test

class GroundCableElementTest {
    @Test
    fun multiConductorUtilityCableConnectionMaskSelectsGroundConductor() {
        val groundCable = GroundCableElement(SixNode(), Direction.ZP, GroundCableDescriptor("Test Ground Cable", null))
        val utilityCable = testCable()

        val mask = groundCable.connectionMaskForUtilityCable(utilityCable)

        assertEquals(2, mask shr NodeBase.maskColorShift and 0xF)
        assertEquals(NodeBase.maskColorCareData, mask and NodeBase.maskColorCareData)
    }

    @Test
    fun groundCableMaskSelectsGreenLoadNotBlackLoad() {
        val groundCable = GroundCableElement(SixNode(), Direction.ZP, GroundCableDescriptor("Test Ground Cable", null))
        val utilityCable = testCable()
        val mask = groundCable.connectionMaskForUtilityCable(utilityCable)

        val selectedLoad = utilityCable.getElectricalLoad(LRDU.Up, mask)
        val greenLoad = utilityCable.getElectricalLoad(LRDU.Up, utilityCable.maskForConductorColor(2))
        val blackLoad = utilityCable.getElectricalLoad(LRDU.Up, utilityCable.maskForConductorColor(0))

        assertSame(greenLoad, selectedLoad)
        assertNotSame(blackLoad, selectedLoad)
    }

    @Test
    fun internalSixNodeAdjacencySelectsGroundForEveryOrientation() {
        for (groundSide in Direction.values()) {
            for (groundLrdu in LRDU.values()) {
                val cableSide = groundSide.applyLRDU(groundLrdu)
                val cableLrdu = cableSide.getLRDUGoingTo(groundSide) ?: continue
                val node = SixNode()
                val groundCable = GroundCableElement(node, groundSide, GroundCableDescriptor("Test Ground Cable", null))
                val utilityCable = testCable(node, cableSide)
                node.sideElementList[groundSide.int] = groundCable
                node.sideElementList[cableSide.int] = utilityCable

                val mask = groundCable.getConnectionMask(groundLrdu)
                val selectedLoad = utilityCable.getElectricalLoad(cableLrdu, mask)
                val greenLoad = utilityCable.getElectricalLoad(cableLrdu, utilityCable.maskForConductorColor(2))
                val blackLoad = utilityCable.getElectricalLoad(cableLrdu, utilityCable.maskForConductorColor(0))

                assertEquals("ground side $groundSide lrdu $groundLrdu", 2, mask shr NodeBase.maskColorShift and 0xF)
                assertSame("ground side $groundSide lrdu $groundLrdu", greenLoad, selectedLoad)
                assertNotSame("ground side $groundSide lrdu $groundLrdu", blackLoad, selectedLoad)
            }
        }
    }

    @Test
    fun groundCableStillAttachesToBlackElectricalCable() {
        val node = SixNode()
        val groundSide = Direction.ZP
        val groundLrdu = LRDU.Up
        val cableSide = groundSide.applyLRDU(groundLrdu)
        val groundCable = GroundCableElement(node, groundSide, GroundCableDescriptor("Test Ground Cable", null))
        val blackCable = electricalCable(node, cableSide, color = 0)
        node.sideElementList[groundSide.int] = groundCable
        node.sideElementList[cableSide.int] = blackCable

        val groundMask = groundCable.getConnectionMask(groundLrdu)
        val blackMask = blackCable.getConnectionMask(LRDU.Up)

        assertTrue(NodeBase.compareConnectionMask(groundMask, blackMask))
    }

    @Test
    fun groundCableStillAttachesToGreenElectricalCable() {
        val node = SixNode()
        val groundSide = Direction.ZP
        val groundLrdu = LRDU.Up
        val cableSide = groundSide.applyLRDU(groundLrdu)
        val groundCable = GroundCableElement(node, groundSide, GroundCableDescriptor("Test Ground Cable", null))
        val greenCable = electricalCable(node, cableSide, color = 2)
        val cableLrdu = cableSide.getLRDUGoingTo(groundSide)!!
        node.sideElementList[groundSide.int] = groundCable
        node.sideElementList[cableSide.int] = greenCable

        val groundMask = groundCable.getConnectionMask(groundLrdu)
        val greenMask = greenCable.getConnectionMask(cableLrdu)

        assertTrue(NodeBase.compareConnectionMask(groundMask, greenMask))
    }

    @Test
    fun utilityCableMapsGroundedSingleConductorCableToGroundConductor() {
        val node = SixNode()
        val cableSide = Direction.ZP
        val cableToUtility = LRDU.Up
        val cableToGround = LRDU.Right
        val utilitySide = cableSide.applyLRDU(cableToUtility)
        val groundSide = cableSide.applyLRDU(cableToGround)
        val utilityToCable = utilitySide.getLRDUGoingTo(cableSide)!!
        val blackCable = electricalCable(node, cableSide, color = 0)
        val utilityCable = testCable(node, utilitySide)
        val groundCable = GroundCableElement(node, groundSide, GroundCableDescriptor("Test Ground Cable", null))
        node.sideElementList[cableSide.int] = blackCable
        node.sideElementList[utilitySide.int] = utilityCable
        node.sideElementList[groundSide.int] = groundCable

        val remoteEndpoint = utilityCable.getAdjacentConnectionEndpoint(utilityToCable)!!
        val selectedLoad = utilityCable.getElectricalLoad(utilityToCable, blackCable.getConnectionMask(cableToUtility), remoteEndpoint)
        val greenLoad = utilityCable.getElectricalLoad(utilityToCable, utilityCable.maskForConductorColor(2))
        val blackLoad = utilityCable.getElectricalLoad(utilityToCable, utilityCable.maskForConductorColor(0))

        assertSame(greenLoad, selectedLoad)
        assertNotSame(blackLoad, selectedLoad)
    }

    @Test
    fun utilityCableMapsGroundedSingleConductorUtilityCableToGroundConductor() {
        val node = SixNode()
        val singleSide = Direction.ZP
        val singleToUtility = LRDU.Up
        val singleToGround = LRDU.Right
        val utilitySide = singleSide.applyLRDU(singleToUtility)
        val groundSide = singleSide.applyLRDU(singleToGround)
        val utilityToSingle = utilitySide.getLRDUGoingTo(singleSide)!!
        val singleCable = singleConductorUtilityCable(node, singleSide)
        val utilityCable = testCable(node, utilitySide)
        val groundCable = GroundCableElement(node, groundSide, GroundCableDescriptor("Test Ground Cable", null))
        node.sideElementList[singleSide.int] = singleCable
        node.sideElementList[utilitySide.int] = utilityCable
        node.sideElementList[groundSide.int] = groundCable

        val remoteEndpoint = utilityCable.getAdjacentConnectionEndpoint(utilityToSingle)!!
        val selectedLoad = utilityCable.getElectricalLoad(utilityToSingle, singleCable.getConnectionMask(singleToUtility), remoteEndpoint)
        val greenLoad = utilityCable.getElectricalLoad(utilityToSingle, utilityCable.maskForConductorColor(2))
        val blackLoad = utilityCable.getElectricalLoad(utilityToSingle, utilityCable.maskForConductorColor(0))

        assertSame(greenLoad, selectedLoad)
        assertNotSame(blackLoad, selectedLoad)
    }

    private fun testCable(node: SixNode = SixNode(), side: Direction = Direction.ZP): UtilityCableElement {
        val descriptor = UtilityCableDescriptor(
            "Test 12/3 Cable",
            CableRenderDescriptor("eln", "textures/test-cable.png", 4.0f, 4.0f),
            "Test cable",
            "12/3 AWG",
            "3.3 mm2",
            "3G3.3",
            UtilityCableMaterial.COPPER,
            13.2,
            4,
            true,
            600.0,
            false,
            UtilityCableMaterial.COPPER.meltingPointCelsius,
            false,
            arrayOf(UtilityCablePalette("12_3", "Black Red White Green", intArrayOf(0, 1, 15, 2))),
            false
        )
        return UtilityCableElement(node, side, descriptor)
    }

    private fun electricalCable(node: SixNode, side: Direction, color: Int): ElectricalCableElement {
        val cable = ElectricalCableElement(
            node,
            side,
            ElectricalCableDescriptor("Test Electrical Cable", null, "Test cable", false)
        )
        cable.readFromNBT(NBTTagCompound().also { it.setByte("color", (color + (1 shl 4)).toByte()) })
        return cable
    }

    private fun singleConductorUtilityCable(node: SixNode, side: Direction): UtilityCableElement {
        val descriptor = UtilityCableDescriptor(
            "Test 12 AWG Cable",
            CableRenderDescriptor("eln", "textures/test-cable.png", 4.0f, 4.0f),
            "Test single cable",
            "12 AWG",
            "3.3 mm2",
            "3.3",
            UtilityCableMaterial.COPPER,
            3.3,
            1,
            true,
            600.0,
            false,
            UtilityCableMaterial.COPPER.meltingPointCelsius,
            false,
            arrayOf(UtilityCablePalette("single", "Single", intArrayOf(0))),
            false
        )
        return UtilityCableElement(node, side, descriptor)
    }
}
