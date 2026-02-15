package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class RootSystemUtilityTest {
    @Test
    fun isRegistredTracksPendingAndSubsystemStates() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val load = ElectricalLoad()
        assertFalse(root.isRegistred(load))

        root.addState(load)
        assertTrue(root.isRegistred(load))

        val other = VoltageState()
        val resistor = Resistor(load, other)
        root.addState(other)
        root.addComponent(resistor)
        root.generate()

        assertTrue(root.isRegistred(load))
    }

    @Test
    fun removeStateAndComponentDropFromPendingSets() {
        val root = RootSystem(0.1, 1)
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)

        root.addState(a)
        root.addComponent(resistor)
        assertTrue(root.addStates.contains(a))
        assertTrue(root.addComponents.contains(resistor))

        root.removeComponent(resistor)
        root.removeState(a)
        assertFalse(root.addComponents.contains(resistor))
        assertFalse(root.addStates.contains(a))
        assertFalse(root.isRegistred(ElectricalLoad()))
    }
}
