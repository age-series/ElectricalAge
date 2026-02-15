package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.InterSystem
import mods.eln.sim.mna.component.Line
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState
import mods.eln.sim.mna.state.VoltageStateLineReady

private class DummyComponent : Component() {
    override fun applyToSubsystem(s: SubSystem) {}
    override fun getConnectedStates(): Array<State> = emptyArray()
}

class RootSystemCoverageTest {
    @Test
    fun addRemoveComponentAndStateBreakSubsystems() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)
        val subSystem = SubSystem(root, 0.1)
        root.systems.add(subSystem)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(resistor)

        root.removeComponent(resistor)
        assertEquals(0, root.subSystemCount)
        assertTrue(!root.addComponents.contains(resistor))

        val subSystem2 = SubSystem(root, 0.1)
        val a2 = VoltageState()
        val b2 = VoltageState()
        val resistor2 = Resistor(a2, b2)
        root.systems.add(subSystem2)
        subSystem2.addState(a2)
        subSystem2.addComponent(resistor2)
        root.addState(a2)
        assertEquals(0, root.subSystemCount)
        assertTrue(root.addStates.contains(a2))

        val subSystem3 = SubSystem(root, 0.1)
        val a3 = VoltageState()
        val b3 = VoltageState()
        val resistor3 = Resistor(a3, b3)
        root.systems.add(subSystem3)
        subSystem3.addState(a3)
        subSystem3.addState(b3)
        subSystem3.addComponent(resistor3)
        root.removeState(a3)
        assertEquals(0, root.subSystemCount)
    }

    @Test
    fun generateLineCreatesLineComponent() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val sLine = VoltageStateLineReady().apply { setCanBeSimplifiedByLine(true) }
        val sA = VoltageState()
        val sB = VoltageState()
        val r1 = Resistor(sLine, sA).apply { resistance = 2.0 }
        val r2 = Resistor(sLine, sB).apply { resistance = 3.0 }

        root.addState(sLine)
        root.addState(sA)
        root.addState(sB)
        root.addComponent(r1)
        root.addComponent(r2)

        root.generate()

        val line = root.systems.flatMap { it.component }.filterIsInstance<Line>().firstOrNull()
        assertNotNull(line)
    }

    @Test
    fun generateLineHandlesDuplicateResistorEntry() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val sLine = VoltageStateLineReady().apply { setCanBeSimplifiedByLine(true) }
        val sA = VoltageState()
        val r1 = Resistor(sLine, sA).apply { resistance = 2.0 }
        sLine.addComponent(r1)

        root.addState(sLine)
        root.addState(sA)
        root.addComponent(r1)

        root.generate()

        val line = root.systems.flatMap { it.component }.filterIsInstance<Line>().firstOrNull()
        assertNull(line)
    }

    @Test
    fun generateSystemsHandlesPrivateAndExistingSubsystems() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val privateState = VoltageState().setAsPrivate()
        val publicState = VoltageState()
        val resistor = Resistor(privateState, publicState)
        root.addState(privateState)
        root.addState(publicState)
        root.addComponent(resistor)

        val externalSystem = SubSystem(root, 0.1)
        root.systems.add(externalSystem)
        val externalState = VoltageState()
        externalState.setSubsystem(externalSystem)
        val skipped = Resistor(publicState, externalState)
        root.addComponent(skipped)

        root.generate()

        assertTrue(root.subSystemCount >= 2)
    }

    @Test
    fun generateSystemsSplitsLargeInterSystemNetworks() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val states = Array(105) { VoltageState() }
        for (idx in 0 until states.size - 1) {
            val inter = InterSystem().apply { connectTo(states[idx], states[idx + 1]) }
            root.addComponent(inter)
        }
        states.forEach(root::addState)

        root.generate()
        assertTrue(root.subSystemCount > 1)
    }

    @Test
    fun generateInterSystemsCoversCastingAndNullPins() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val nonResistor = DummyComponent()
        root.addComponent(nonResistor)

        val a = VoltageState()
        val b = VoltageState()
        val resistorWithNull = Resistor(a, null)
        root.addComponent(resistorWithNull)

        root.generateInterSystems()
        assertTrue(root.addComponents.contains(resistorWithNull))
    }

    @Test
    fun generateInterSystemsHandlesNullSubsystems() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)
        root.addComponent(resistor)

        root.generateInterSystems()
        assertTrue(root.addComponents.isEmpty())
    }

    @Test
    fun findSubSystemWithUsesContainedState() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val subSystem = SubSystem(root, 0.1)
        val state = VoltageState()
        subSystem.addState(state)
        root.systems.add(subSystem)

        val found = root.findSubSystemWith(state)
        assertEquals(subSystem, found)
    }

    @Test
    fun breakSystemsRecursesThroughConnectivity() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val a = SubSystem(root, 0.1)
        val b = SubSystem(root, 0.1)
        root.systems.add(a)
        root.systems.add(b)
        a.interSystemConnectivity.add(b)
        b.interSystemConnectivity.add(a)

        root.breakSystems(a)
        assertEquals(0, root.subSystemCount)
    }
}
