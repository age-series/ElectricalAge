package mods.eln.sim.mna

import mods.eln.disableLog4jJmx
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.CurrentSource
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

private const val EPS = 1e-6

private fun assertClose(expected: Double, actual: Double, eps: Double = EPS) {
    assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
}

class SubSystemMainExampleTest {
    @Test
    fun currentSourceResistorExampleMatchesExpected() {
        disableLog4jJmx()
        val subSystem = SubSystem(null, 0.1)
        val n1 = VoltageState()
        val cs1 = CurrentSource("cs1")
        cs1.setCurrent(0.01)
        cs1.connectTo(n1, null)
        val r1 = Resistor()
        r1.setResistance(10.0)
        r1.connectTo(n1, null)

        subSystem.addState(n1)
        subSystem.addComponent(cs1)
        subSystem.addComponent(r1)

        subSystem.step()

        assertClose(0.1, n1.state)
        assertClose(0.1, r1.voltage)
        assertClose(0.01, r1.current)
        assertClose(0.1, cs1.voltage)
    }
}
