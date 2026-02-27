package mods.eln.sim.mna

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class RootSystemLifecycleTest {
    @Test
    fun generateBuildsSubsystemAndBreakRestoresRootSets() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b).setResistance(5.0)

        root.addState(a)
        root.addState(b)
        root.addComponent(resistor)

        root.generate()
        assertEquals(1, root.subSystemCount)
        val subSystem = root.systems.single()
        assertTrue(subSystem.states.contains(a))
        assertTrue(subSystem.states.contains(b))
        assertTrue(subSystem.component.contains(resistor))
        assertTrue(root.addStates.isEmpty())
        assertTrue(root.addComponents.isEmpty())

        root.breakSystems(subSystem)
        assertEquals(0, root.subSystemCount)
        assertTrue(root.addStates.contains(a))
        assertTrue(root.addStates.contains(b))
        assertTrue(root.addComponents.contains(resistor))
        assertNull(a.subSystem)
        assertNull(b.subSystem)
        assertNull(resistor.subSystem)

        assertEquals(false, subSystem.breakSystem())
    }
}
