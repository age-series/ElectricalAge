package mods.eln.sim.mna

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.VoltageState

private const val EPS = 1e-6

private fun assertClose(expected: Double, actual: Double, eps: Double = EPS) {
    assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
}

class SubSystemUtilityTest {
    @Test
    fun accessorsAndSolveMatchState() {
        disableLog4jJmx()
        val subSystem = SubSystem(null, 0.1)
        val node = VoltageState()
        val extra = VoltageState()
        val source = VoltageSource("V").apply { connectTo(node, null) }
        source.voltage = 1.0
        val resistor = Resistor(node, null)
        resistor.resistance = 10.0

        subSystem.addState(listOf(node, extra))
        subSystem.addComponent(listOf(source, resistor))
        subSystem.removeState(extra)

        assertTrue(subSystem.containe(node))
        assertEquals(2, subSystem.componentSize())
        assertEquals(0.1, subSystem.dt)

        subSystem.setX(node, 0.4)
        assertEquals(0.4, subSystem.getX(node))
        assertEquals(0.0, subSystem.getXSafe(null))

        subSystem.step()
        val solved = subSystem.solve(source.currentState)
        assertClose(source.currentState.state, solved)
        assertClose(1.0, node.state)

        val description = subSystem.toString()
        assertTrue(description.contains("Resistor"))
        subSystem.component.add(null)
        val descriptionWithNull = subSystem.toString()
        assertTrue(descriptionWithNull.contains("Resistor"))

        subSystem.removeComponent(resistor)
        assertEquals(2, subSystem.componentSize())
    }
}
