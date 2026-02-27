package mods.eln.sim.mna.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.IAbstractor
import mods.eln.sim.mna.component.Resistor

class StateComponentListTest {
    @Test
    fun connectedComponentsRespectAbstraction() {
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)
        val all = a.connectedComponents
        assertTrue(all.contains(resistor))

        resistor.abstractedBy = object : IAbstractor {
            override fun dirty(component: Component) {}
            override fun getAbstractorSubSystem(): SubSystem = SubSystem(null, 0.1)
        }

        val nonAbstracted = a.connectedComponentsNotAbstracted
        assertEquals(0, nonAbstracted.size)
    }

    @Test
    fun toStringIncludesIdAndClass() {
        val state = State()
        state.id = 7
        val text = state.toString()
        assertTrue(text.contains("State"))
        assertTrue(text.contains("7"))
    }
}
