package mods.eln.sim.mna.process

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.disableLog4jJmx
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

class TransformerInterSystemProcessTest {
    @Test
    fun rootSystemPreStepUpdatesSourcesWithRatio() {
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

        val process = TransformerInterSystemProcess(aState, bState, aSrc, bSrc)
        process.setRatio(2.0)

        val aTh = aSystem.getTh(aState, aSrc)
        val bTh = bSystem.getTh(bState, bSrc)
        val ratio = process.ratio
        val expectedVoltage = (aTh.voltage * bTh.resistance + ratio * bTh.voltage * aTh.resistance) /
            (bTh.resistance + ratio * ratio * aTh.resistance)

        process.rootSystemPreStepProcess()

        assertEquals(expectedVoltage, aSrc.voltage, 1e-9)
        assertEquals(expectedVoltage * ratio, bSrc.voltage, 1e-9)
    }

    @Test
    fun getRatioReturnsConfiguredValue() {
        val aState = VoltageState()
        val bState = VoltageState()
        val aSrc = VoltageSource("a").apply { connectTo(aState, null) }
        val bSrc = VoltageSource("b").apply { connectTo(bState, null) }

        val process = TransformerInterSystemProcess(aState, bState, aSrc, bSrc)
        process.setRatio(4.0)
        assertEquals(4.0, process.getRatio())
    }

    @Test
    fun nanVoltagesFallbackToZero() {
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

        val process = TransformerInterSystemProcess(aState, bState, aSrc, bSrc)
        process.rootSystemPreStepProcess()

        assertEquals(0.0, aSrc.voltage, 1e-9)
        assertEquals(0.0, bSrc.voltage, 1e-9)
    }
}
