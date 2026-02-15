package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState

class LineBehaviorTest {
    @Test
    fun addResistorTracksInterSystemAndResistance() {
        val a = VoltageState()
        val b = VoltageState()
        val line = Line()

        val r1 = Resistor(a, b).apply { resistance = 2.0 }
        val r2 = InterSystem().apply {
            connectTo(b, null)
            resistance = 3.0
        }

        assertFalse(line.canBeReplacedByInterSystem())
        line.addResistor(r1)
        line.addResistor(r2)
        line.recalculateResistance()

        assertEquals(5.0, line.resistance)
        assertTrue(line.canBeReplacedByInterSystem())
        assertTrue(line.canAddComponent(r1))
    }

    @Test
    fun returnToRootSystemRestoresResistorsAndStates() {
        val root = RootSystem(0.1, 1)
        val line = Line()
        val a = VoltageState()
        val b = VoltageState()
        val mid = VoltageState()
        val r1 = Resistor(a, mid).apply { resistance = 1.0 }
        val r2 = Resistor(mid, b).apply { resistance = 1.0 }

        line.resistors = java.util.LinkedList<Resistor>()
        line.resistors.add(r1)
        line.resistors.add(r2)
        line.states = java.util.LinkedList<State>()
        line.states.add(mid)
        line.connectTo(a, b)
        r1.abstractedBy = line
        r2.abstractedBy = line
        mid.abstractedBy = line

        root.addProcess(line)
        line.returnToRootSystem(root)

        assertTrue(root.addComponents.contains(r1))
        assertTrue(root.addComponents.contains(r2))
        assertTrue(root.addStates.contains(mid))
        assertEquals(null, r1.abstractedBy)
        assertEquals(null, r2.abstractedBy)
        assertEquals(null, mid.abstractedBy)
    }

    @Test
    fun addToSubsystemRegistersProcessAndQuitIsNoOp() {
        mods.eln.sim.mna.disableLog4jJmx()
        val a = VoltageState()
        val b = VoltageState()
        val line = object : Line() {
            var flushed = false

            override fun simProcessFlush() {
                flushed = true
                super.simProcessFlush()
            }
        }
        line.connectTo(a, b)
        line.resistors = java.util.LinkedList<Resistor>()
        line.states = java.util.LinkedList<State>()

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(line)
        subSystem.generateMatrix()
        subSystem.stepFlush()

        assertTrue(line.flushed)
        line.quitSubSystem()
    }

    @Test
    fun dirtyRecalculatesAndPropagatesToAbstractor() {
        val a = VoltageState()
        val b = VoltageState()
        val r1 = Resistor(a, b).apply { resistance = 2.0 }
        val r2 = Resistor(a, b).apply { resistance = 3.0 }
        val line = Line()
        line.resistors = java.util.LinkedList<Resistor>()
        line.resistors.add(r1)
        line.resistors.add(r2)
        line.recalculateResistance()

        var dirtyCalled = false
        val abstractor = object : IAbstractor {
            override fun dirty(component: Component) {
                dirtyCalled = true
            }

            override fun getAbstractorSubSystem(): SubSystem = SubSystem(null, 0.1)
        }
        line.abstractedBy = abstractor

        r1.resistance = 4.0
        line.dirty(r1)

        assertEquals(7.0, line.resistance)
        assertTrue(dirtyCalled)
    }

    @Test
    fun getAbstractorSubSystemReturnsSubsystem() {
        val line = Line()
        val subSystem = SubSystem(null, 0.1)
        line.addToSubsystem(subSystem)
        assertEquals(subSystem, line.abstractorSubSystem)
    }
}
