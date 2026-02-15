package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

class SubSystemTheveninTest {
    @Test
    fun isHighImpedanceMatchesThreshold() {
        val th = SubSystem.Thevenin()
        th.resistance = 1e7
        assertTrue(!th.isHighImpedance)
        th.resistance = 1e9
        assertTrue(th.isHighImpedance)
    }

    @Test
    fun getThHandlesInfiniteResistance() {
        disableLog4jJmx()
        val subSystem = SubSystem(null, 0.1)
        val node = VoltageState()
        val source = VoltageSource("V").apply { connectTo(node, null) }
        subSystem.addState(node)
        subSystem.addComponent(source)

        val th = subSystem.getTh(node, source)
        assertEquals(1e19, th.resistance)
        assertEquals(0.0, th.voltage)
    }

    @Test
    fun getThHandlesNaNVoltage() {
        disableLog4jJmx()
        val subSystem = SubSystem(null, 0.1)
        val node = VoltageState()
        node.state = Double.NaN
        val source = VoltageSource("V").apply { connectTo(node, null) }
        subSystem.addState(node)
        subSystem.addComponent(source)

        val th = subSystem.getTh(node, source)
        assertTrue(th.voltage.isNaN())
        assertEquals(mods.eln.sim.mna.misc.MnaConst.highImpedance, th.resistance)
    }
}
