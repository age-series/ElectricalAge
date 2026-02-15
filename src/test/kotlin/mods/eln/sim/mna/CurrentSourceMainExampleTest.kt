package mods.eln.sim.mna

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState

private const val EPS = 1e-6

private fun assertClose(expected: Double, actual: Double, eps: Double = EPS) {
    assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
}

class CurrentSourceMainExampleTest {
    @Test
    fun voltageSourceResistorExampleMatchesExpected() {
        disableLog4jJmx()
        val s = SubSystem(null, 0.05)

        val n1: State = VoltageState()

        val vs = VoltageSource("voltage", n1, null)
        val r1 = Resistor(n1, null)

        r1.resistance = 10.0
        vs.voltage = 1.0

        s.addState(n1)
        s.addComponent(vs)
        s.addComponent(r1)

        s.step()
        s.step()

        assertClose(1.0, r1.voltage)
        assertClose(0.1, r1.current)
        assertClose(1.0, n1.state)
    }
}
