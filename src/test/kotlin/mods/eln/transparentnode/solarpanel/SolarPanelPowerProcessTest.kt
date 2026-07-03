package mods.eln.transparentnode.solarpanel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.CurrentSource
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.nbt.NBTTagCompound

class SolarPanelPowerProcessTest {
    @Test
    fun curvePassesThroughStcPoints() {
        val voc = 37.0
        val vmp = 30.0
        val imp = 8.0
        val isc = 8.6

        assertEquals(isc, SolarPanelPowerProcess.currentAtVoltage(0.0, 1.0, voc, vmp, imp, isc), 1.0e-9)
        assertEquals(imp, SolarPanelPowerProcess.currentAtVoltage(vmp, 1.0, voc, vmp, imp, isc), 1.0e-9)
        assertEquals(0.0, SolarPanelPowerProcess.currentAtVoltage(voc, 1.0, voc, vmp, imp, isc), 1.0e-9)
        assertEquals(240.0, vmp * imp, 1.0e-9)
    }

    @Test
    fun currentScalesWithLight() {
        val current = SolarPanelPowerProcess.currentAtVoltage(0.0, 0.5, 37.0, 30.0, 8.0, 8.6)

        assertEquals(4.3, current, 1.0e-9)
    }

    @Test
    fun loadedPanelDoesNotCollapseToMillivolts() {
        val root = RootSystem(0.1, 1)
        val positive = NbtElectricalLoad("positive")
        val negative = NbtElectricalLoad("negative")
        val source = CurrentSource("source", positive, negative)
        val shunt = Resistor(positive, negative)
        val load = Resistor(positive, negative).setResistance(30.0 / 8.0)
        val process = SolarPanelPowerProcess(
            positive,
            negative,
            source,
            shunt,
            37.0,
            30.0,
            8.0,
            8.6
        )
        process.setLightFactor(1.0)

        root.addState(positive)
        root.addState(negative)
        root.addComponent(source)
        root.addComponent(shunt)
        root.addComponent(load)
        root.addComponent(Resistor(negative, null).pullDown())
        root.addProcess(process)

        repeat(8) {
            root.step()
        }

        val panelVoltage = positive.voltage - negative.voltage
        assertTrue(panelVoltage > 20.0, "expected loaded solar panel voltage above 20V but was $panelVoltage")
    }

    @Test
    fun panelDoesNotCollapseThroughBreakerLikeClosedContact() {
        val root = RootSystem(0.1, 1)
        val positive = NbtElectricalLoad("positive")
        val negative = NbtElectricalLoad("negative")
        val breakerA = NbtElectricalLoad("breakerA")
        val breakerB = NbtElectricalLoad("breakerB")
        val source = CurrentSource("source", positive, negative)
        val shunt = Resistor(positive, negative)
        val process = SolarPanelPowerProcess(
            positive,
            negative,
            source,
            shunt,
            37.0,
            30.0,
            8.0,
            8.6
        )
        process.setLightFactor(1.0)

        root.addState(positive)
        root.addState(negative)
        root.addState(breakerA)
        root.addState(breakerB)
        root.addComponent(source)
        root.addComponent(shunt)
        root.addComponent(Resistor(positive, breakerA).setResistance(0.01))
        root.addComponent(Resistor(negative, null).pullDown())
        root.addComponent(Resistor(breakerA, breakerB).setResistance(0.01))
        root.addComponent(Resistor(breakerA, null).pullDown())
        root.addComponent(Resistor(breakerB, null).pullDown())
        root.addComponent(Resistor(breakerB, null).setResistance(30.0 / 8.0))
        root.addProcess(process)

        repeat(8) {
            root.step()
        }

        val panelVoltage = positive.voltage - negative.voltage
        assertTrue(panelVoltage > 20.0, "expected breaker-loaded solar panel voltage above 20V but was $panelVoltage")
    }

    @Test
    fun panelsInSeriesDoNotCollapseToShortCircuitCurrent() {
        val root = RootSystem(0.1, 1)
        val pins = Array(5) { NbtElectricalLoad("series$it") }
        val processes = Array(4) { idx ->
            val source = CurrentSource("source$idx", pins[idx + 1], pins[idx])
            val shunt = Resistor(pins[idx + 1], pins[idx])
            root.addComponent(source)
            root.addComponent(shunt)
            SolarPanelPowerProcess(
                pins[idx + 1],
                pins[idx],
                source,
                shunt,
                37.0,
                30.0,
                8.0,
                8.6
            ).also {
                it.setLightFactor(1.0)
                root.addProcess(it)
            }
        }

        pins.forEach(root::addState)
        root.addComponent(Resistor(pins[0], null).pullDown())
        root.addComponent(Resistor(pins[4], null).setResistance((30.0 * processes.size) / 8.0))

        repeat(16) {
            root.step()
        }

        val stringVoltage = kotlin.math.abs(pins[4].voltage - pins[0].voltage)
        assertTrue(stringVoltage > 80.0, "expected series solar string voltage above 80V but was $stringVoltage")
    }

    @Test
    fun panelsInSeriesThroughElectricalConnectionsDoNotCollapse() {
        val root = RootSystem(0.1, 1)
        val cablePins = Array(5) { NbtElectricalLoad("cable$it") }
        cablePins.forEach { it.serialResistance = 2.11544e-5 }
        val processes = Array(4) { idx ->
            val negative = NbtElectricalLoad("negative$idx")
            val positive = NbtElectricalLoad("positive$idx")
            negative.serialResistance = 0.028125
            positive.serialResistance = 0.028125
            val source = CurrentSource("source$idx", positive, negative)
            val shunt = Resistor(positive, negative)

            root.addState(negative)
            root.addState(positive)
            root.addComponent(source)
            root.addComponent(shunt)
            root.addComponent(ElectricalConnection(negative, cablePins[idx]))
            root.addComponent(ElectricalConnection(positive, cablePins[idx + 1]))

            SolarPanelPowerProcess(
                positive,
                negative,
                source,
                shunt,
                37.0,
                30.0,
                8.0,
                8.6
            ).also {
                it.setLightFactor(1.0)
                root.addProcess(it)
            }
        }

        cablePins.forEach(root::addState)
        root.addComponent(Resistor(cablePins[0], null).pullDown())
        root.addComponent(Resistor(cablePins[4], null).setResistance((30.0 * processes.size) / 8.0))

        repeat(16) {
            root.step()
        }

        val stringVoltage = cablePins[4].voltage - cablePins[0].voltage
        assertTrue(stringVoltage > 80.0, "expected connected series solar string voltage above 80V but was $stringVoltage")
    }

    @Test
    fun singleTilePanelStaysFloatingAfterNbtLoad() {
        val element = SolarPanelElement(TransparentNode(), testDescriptor(groundCoordinate = null))
        val nbt = NBTTagCompound()
        nbt.setByte("others", (Direction.XN.int + 8).toByte())

        element.readFromNBT(nbt)

        assertFalse(element.grounded)
        assertEquals(NodeBase.maskElectricalPower, element.getConnectionMask(element.front.right(), LRDU.Down))
    }

    private fun testDescriptor(groundCoordinate: Coordinate?) = SolarPanelDescriptor(
        "Test Solar Panel",
        null,
        CableRenderDescriptor("eln", "sprites/cable.png", 1.0f, 1.0f),
        null,
        0,
        0,
        0,
        groundCoordinate,
        37.0,
        30.0,
        8.0,
        8.6,
        0.0,
        Math.PI / 2,
        Math.PI / 2
    )
}
