package mods.eln.sim.mna.process

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.SubSystem
import mods.eln.disableLog4jJmx
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

class PowerSourceBipoleTest {
    @Test
    fun rootSystemPreStepUsesTheveninWhenVoltageLimited() {
        disableLog4jJmx()
        val aState = VoltageState()
        val bState = VoltageState()
        val aSrc = VoltageSource("a").apply { connectTo(aState, null) }
        val bSrc = VoltageSource("b").apply { connectTo(bState, null) }

        val aSystem = SubSystem(null, 0.1)
        aSystem.addState(aState)
        aSystem.addComponent(Resistor(aState, null).setResistance(10.0))
        aSystem.addComponent(aSrc)

        val bSystem = SubSystem(null, 0.1)
        bSystem.addState(bState)
        bSystem.addComponent(Resistor(bState, null).setResistance(20.0))
        bSystem.addComponent(bSrc)

        val process = PowerSourceBipole(aState, bState, aSrc, bSrc)
        process.setPower(5.0)
        process.setMaximumVoltage(0.0)
        process.setMaximumCurrent(100.0)

        val aTh = aSystem.getTh(aState, aSrc)
        val bTh = bSystem.getTh(bState, bSrc)

        process.rootSystemPreStepProcess()

        assertEquals(aTh.voltage, aSrc.voltage, 1e-9)
        assertEquals(bTh.voltage, bSrc.voltage, 1e-9)
    }

    @Test
    fun setMaximumsAndPowerAccessors() {
        val aState = VoltageState()
        val bState = VoltageState()
        val aSrc = VoltageSource("a").apply { connectTo(aState, null) }
        val bSrc = VoltageSource("b").apply { connectTo(bState, null) }
        val process = PowerSourceBipole(aState, bState, aSrc, bSrc)
        process.setPower(4.0)
        process.setMaximums(12.0, 3.0)

        assertEquals(4.0, process.power)
        assertEquals(12.0, process.maximumVoltage)
        assertEquals(3.0, process.maximumCurrent)
    }

    @Test
    fun rootSystemPreStepUsesComputedVoltageWhenBelowMaximum() {
        disableLog4jJmx()
        val aState = VoltageState()
        val bState = VoltageState()
        val aSrc = VoltageSource("a").apply { connectTo(aState, null) }
        val bSrc = VoltageSource("b").apply { connectTo(bState, null) }

        val aSystem = SubSystem(null, 0.1)
        aSystem.addState(aState)
        aSystem.addComponent(Resistor(aState, null).setResistance(10.0))
        aSystem.addComponent(aSrc)

        val bSystem = SubSystem(null, 0.1)
        bSystem.addState(bState)
        bSystem.addComponent(Resistor(bState, null).setResistance(20.0))
        bSystem.addComponent(bSrc)

        val process = PowerSourceBipole(aState, bState, aSrc, bSrc)
        process.setPower(1.0)
        process.setMaximumVoltage(10.0)
        process.setMaximumCurrent(10.0)

        val aTh = aSystem.getTh(aState, aSrc)
        val bTh = bSystem.getTh(bState, bSrc)
        val theveninVoltage = aTh.voltage - bTh.voltage
        val theveninResistance = aTh.resistance + bTh.resistance
        val expectedVoltage = (kotlin.math.sqrt(theveninVoltage * theveninVoltage + 4 * process.power * theveninResistance) +
            theveninVoltage) / 2
        val expectedCurrent = (theveninVoltage - expectedVoltage) / theveninResistance

        process.rootSystemPreStepProcess()

        assertEquals(aTh.voltage - expectedCurrent * aTh.resistance, aSrc.voltage, 1e-9)
        assertEquals(bTh.voltage + expectedCurrent * bTh.resistance, bSrc.voltage, 1e-9)
    }

    @Test
    fun nanTheveninValuesFallbackToHighImpedance() {
        disableLog4jJmx()
        val aState = VoltageState()
        val bState = VoltageState()
        val aSrc = VoltageSource("a").apply { connectTo(aState, null) }
        val bSrc = VoltageSource("b").apply { connectTo(bState, null) }

        val aSystem = SubSystem(null, 0.1)
        aSystem.addState(aState)
        aSystem.addComponent(aSrc)

        val bSystem = SubSystem(null, 0.1)
        bSystem.addState(bState)
        bSystem.addComponent(bSrc)

        aState.state = Double.NaN
        bState.state = Double.NaN

        val process = PowerSourceBipole(aState, bState, aSrc, bSrc)
        process.setPower(1.0)
        process.setMaximumVoltage(10.0)
        process.setMaximumCurrent(10.0)

        process.rootSystemPreStepProcess()

        assertEquals(5.0, aSrc.voltage, 1e-9)
        assertEquals(-5.0, bSrc.voltage, 1e-9)
    }

    @Test
    fun nbtRoundTripPreservesSettings() {
        val aState = VoltageState()
        val bState = VoltageState()
        val aSrc = VoltageSource("a").apply { connectTo(aState, null) }
        val bSrc = VoltageSource("b").apply { connectTo(bState, null) }
        val process = PowerSourceBipole(aState, bState, aSrc, bSrc)
        process.setPower(6.0)
        process.setMaximumVoltage(50.0)
        process.setMaximumCurrent(2.5)

        val nbt = net.minecraft.nbt.NBTTagCompound()
        process.writeToNBT(nbt, "pfx")

        val other = PowerSourceBipole(aState, bState, aSrc, bSrc)
        other.readFromNBT(nbt, "pfx")

        assertEquals(6.0, other.power)
        assertEquals(50.0, other.maximumVoltage)
        assertEquals(2.5, other.maximumCurrent)
    }
}
