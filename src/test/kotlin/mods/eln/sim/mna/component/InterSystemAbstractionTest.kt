package mods.eln.sim.mna.component

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.VoltageState

class InterSystemAbstractionBehaviorTest {
    @Test
    fun dirtyRecalibratesDelaysAndResistors() {
        mods.eln.disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val aSystem = SubSystem(root, 0.1)
        val bSystem = SubSystem(root, 0.1)
        root.systems.add(aSystem)
        root.systems.add(bSystem)

        val aState = VoltageState()
        val bState = VoltageState()
        aSystem.addState(aState)
        bSystem.addState(bState)

        val interResistor = Resistor(aState, bState).apply { resistance = 4.0 }
        val abstraction = InterSystemAbstraction(root, interResistor)

        aState.state = 6.0
        bState.state = 2.0
        abstraction.dirty(interResistor)

        assertEquals(4.0, abstraction.aNewDelay.voltage)
        assertEquals(4.0, abstraction.bNewDelay.voltage)
        assertEquals(2.0, abstraction.aNewResistor.resistance)
        assertEquals(2.0, abstraction.bNewResistor.resistance)
    }

    @Test
    fun rootSystemPreStepProcessKeepsDelayVoltagesAligned() {
        mods.eln.disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val aSystem = SubSystem(root, 0.1)
        val bSystem = SubSystem(root, 0.1)
        root.systems.add(aSystem)
        root.systems.add(bSystem)

        val aState = VoltageState()
        val bState = VoltageState()
        aSystem.addState(aState)
        bSystem.addState(bState)

        val interResistor = Resistor(aState, bState).apply { resistance = 8.0 }
        val abstraction = InterSystemAbstraction(root, interResistor)

        aState.state = 5.0
        bState.state = 1.0
        abstraction.rootSystemPreStepProcess()

        assertEquals(abstraction.aNewDelay.voltage, abstraction.bNewDelay.voltage)
    }

    @Test
    fun abstractorSubsystemAndNaNVoltageFallback() {
        mods.eln.disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val aSystem = SubSystem(root, 0.1)
        val bSystem = SubSystem(root, 0.1)
        root.systems.add(aSystem)
        root.systems.add(bSystem)

        val aState = VoltageState()
        val bState = VoltageState()
        aSystem.addState(aState)
        bSystem.addState(bState)

        val interResistor = Resistor(aState, bState).apply { resistance = 8.0 }
        val abstraction = InterSystemAbstraction(root, interResistor)

        assertEquals(aSystem, abstraction.abstractorSubSystem)

        aState.state = Double.NaN
        bState.state = Double.NaN
        abstraction.rootSystemPreStepProcess()

        assertEquals(0.0, abstraction.aNewDelay.voltage)
        assertEquals(0.0, abstraction.bNewDelay.voltage)
    }
}
