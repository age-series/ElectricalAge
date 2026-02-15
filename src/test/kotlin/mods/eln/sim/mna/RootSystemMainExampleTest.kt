package mods.eln.sim.mna

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.InterSystem
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

private const val EPS = 1e-6

private fun assertClose(expected: Double, actual: Double, eps: Double = EPS) {
    assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
}

class RootSystemMainExampleTest {
    @Test
    fun twoNetworkExampleMatchesExpectedVoltages() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)

        val n1 = VoltageState()
        val n2 = VoltageState()
        val u1 = VoltageSource("u1")
        u1.setVoltage(1.0)
        u1.connectTo(n1, null)
        val r1 = Resistor()
        r1.setResistance(10.0)
        r1.connectTo(n1, n2)
        val r2 = Resistor()
        r2.setResistance(20.0)
        r2.connectTo(n2, null)

        val n11 = VoltageState()
        val n12 = VoltageState()
        val u11 = VoltageSource("u11")
        u11.setVoltage(1.0)
        u11.connectTo(n11, null)
        val r11 = Resistor()
        r11.setResistance(10.0)
        r11.connectTo(n11, n12)
        val r12 = Resistor()
        r12.setResistance(30.0)
        r12.connectTo(n12, null)

        val i01 = InterSystem()
        i01.setResistance(10.0)
        i01.connectTo(n2, n12)

        root.addState(n1)
        root.addState(n2)
        root.addState(n11)
        root.addState(n12)
        root.addComponent(u1)
        root.addComponent(r1)
        root.addComponent(r2)
        root.addComponent(u11)
        root.addComponent(r11)
        root.addComponent(r12)
        root.addComponent(i01)

        repeat(50) { root.step() }

        assertClose(0.6896551724, n2.state)
        assertClose(0.7241379310, n12.state)

        val r13 = Resistor()
        r13.setResistance(30.0)
        r13.connectTo(n12, null)
        root.addComponent(r13)

        repeat(50) { root.step() }

        assertClose(0.6470588235, n2.state)
        assertClose(0.6176470588, n12.state)
    }
}
