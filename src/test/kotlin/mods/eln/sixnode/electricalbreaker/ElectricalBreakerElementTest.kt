package mods.eln.sixnode.electricalbreaker

import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalcable.UtilityCableElement
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import mods.eln.sixnode.electricalcable.UtilityCablePalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.LinkedHashMap

class ElectricalBreakerElementTest {
    @Test
    fun requestedColorUsesColorDataEvenWhenColorCareIsOff() {
        val breaker = ElectricalBreakerElement(
            SixNode(),
            Direction.ZP,
            ElectricalBreakerDescriptor("Test Breaker", null, 20.0)
        )
        val redWithoutColorCare = NodeBase.maskElectricalPower or (1 shl NodeBase.maskColorShift)

        assertEquals(1, breaker.extractRequestedColor(redWithoutColorCare, LRDU.Up))
    }

    @Test
    fun requestedColorStillUsesColorDataWhenColorCareIsOn() {
        val breaker = ElectricalBreakerElement(
            SixNode(),
            Direction.ZP,
            ElectricalBreakerDescriptor("Test Breaker", null, 20.0)
        )
        val redWithColorCare = NodeBase.maskElectricalPower or
            (1 shl NodeBase.maskColorShift) or
            (1 shl NodeBase.maskColorCareShift)

        assertEquals(1, breaker.extractRequestedColor(redWithColorCare, LRDU.Up))
    }

    @Test
    fun secondaryHotUsesRedBreakerLoadWhenConnectingMultiConductorCable() {
        val breaker = testBreaker()
        val cable = testCable()

        val redLoad = breaker.getLoadForColor(true, 1, cable)

        assertSame(breaker.aLoadRed, redLoad)
        assertNotSame(breaker.aLoad, redLoad)
    }

    @Test
    fun primaryHotUsesBlackBreakerLoadWhenConnectingMultiConductorCable() {
        val breaker = testBreaker()
        val cable = testCable()

        val blackLoad = breaker.getLoadForColor(true, 0, cable)

        assertSame(breaker.aLoad, blackLoad)
        assertNotSame(breaker.aLoadRed, blackLoad)
    }

    @Test
    fun multiConductorWailaShowsEachConductorVoltagePair() {
        val breaker = testBreaker()
        val cable = testCable()
        breaker.aLoad.setVoltage(220.0)
        breaker.bLoad.setVoltage(220.0)
        breaker.aLoadRed.setVoltage(-220.0)
        breaker.bLoadRed.setVoltage(-220.0)
        breaker.aLoadWhite.setVoltage(0.0)
        breaker.bLoadWhite.setVoltage(0.0)
        breaker.aLoadGround.setVoltage(0.0)
        breaker.bLoadGround.setVoltage(0.0)

        val info = LinkedHashMap<String, String>()
        breaker.putMultiConductorWaila(info, cable)

        assertEquals(breaker.formatVoltagePair(breaker.aLoad, breaker.bLoad), info["Black"])
        assertEquals(breaker.formatVoltagePair(breaker.aLoadRed, breaker.bLoadRed), info["Red"])
        assertEquals(breaker.formatVoltagePair(breaker.aLoadWhite, breaker.bLoadWhite), info["White"])
        assertEquals(breaker.formatVoltagePair(breaker.aLoadGround, breaker.bLoadGround), info["Green"])
    }

    private fun testBreaker() = ElectricalBreakerElement(
        SixNode(),
        Direction.ZP,
        ElectricalBreakerDescriptor("Test Breaker", null, 20.0)
    )

    private fun testCable(): UtilityCableElement {
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
        return UtilityCableElement(SixNode(), Direction.ZP, descriptor)
    }
}
