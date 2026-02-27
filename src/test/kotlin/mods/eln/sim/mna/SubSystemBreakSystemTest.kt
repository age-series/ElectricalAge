package mods.eln.sim.mna

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.IDestructor
import mods.eln.sim.mna.state.VoltageState

class SubSystemBreakSystemTest {
    @Test
    fun breakSystemRunsDestructorsAndReturnsToRoot() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val subSystem = SubSystem(root, 0.1)
        root.systems.add(subSystem)

        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(resistor)

        var destructCount = 0
        subSystem.breakDestructor.add(IDestructor { destructCount++ })

        val result = subSystem.breakSystem()

        assertTrue(result)
        assertEquals(1, destructCount)
        assertTrue(root.addComponents.contains(resistor))
        assertTrue(root.addStates.contains(a))
        assertTrue(root.addStates.contains(b))
        assertTrue(root.systems.isEmpty())
    }
}
