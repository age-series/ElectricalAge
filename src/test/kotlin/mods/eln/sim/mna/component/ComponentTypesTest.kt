package mods.eln.sim.mna.component

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.SubSystemDebugSnapshot
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState
import mods.eln.sim.mna.state.VoltageStateLineReady
import net.minecraft.nbt.NBTTagCompound

private const val EPS = 1e-9

private fun assertClose(expected: Double, actual: Double, eps: Double = EPS) {
    assertTrue(abs(expected - actual) <= eps, "expected $expected but was $actual")
}

private fun matrixValue(snapshot: SubSystemDebugSnapshot, row: State, col: State): Double {
    return snapshot.conductanceMatrix[row.id][col.id]
}

private class TrackingSubSystem(root: RootSystem?, dt: Double) : SubSystem(root, dt) {
    var invalidated = false

    override fun invalidate() {
        invalidated = true
        super.invalidate()
    }
}

private class TestComponent(private val connected: Array<State>) : Component() {
    override fun applyToSubsystem(s: SubSystem) {}

    override fun getConnectedStates(): Array<State> = connected
}

private class TestBipole : Bipole() {
    override fun applyToSubsystem(s: SubSystem) {}
    override fun getCurrent(): Double = 0.0
}

private class TestMonopole : Monopole() {
    override fun applyToSubsystem(s: SubSystem) {}
}

private class TestAbstractor(private val subSystem: SubSystem) : IAbstractor {
    var dirtyComponent: Component? = null

    override fun dirty(component: Component) {
        dirtyComponent = component
    }

    override fun getAbstractorSubSystem(): SubSystem = subSystem
}

class ComponentBaseTest {
    @Test
    fun ownerAndDirtyRouting() {
        val component = TestComponent(emptyArray<State>())
        component.owner = "owner-a"
        assertEquals("owner-a", component.owner)

        val trackingSubSystem = TrackingSubSystem(null, 0.1)
        component.addToSubsystem(trackingSubSystem)
        component.dirty()
        assertTrue(trackingSubSystem.invalidated)

        val abstractor = TestAbstractor(SubSystem(null, 0.1))
        component.abstractedBy = abstractor
        component.dirty()
        assertEquals(component, abstractor.dirtyComponent)
    }
}

class BipoleTest {
    @Test
    fun connectAndBreakConnection() {
        val a = VoltageState()
        val b = VoltageState()
        val bipole = TestBipole()

        bipole.connectTo(a, b)
        assertTrue(a.connectedComponents.contains(bipole))
        assertTrue(b.connectedComponents.contains(bipole))

        bipole.breakConnection()
        assertFalse(a.connectedComponents.contains(bipole))
        assertFalse(b.connectedComponents.contains(bipole))
    }

    @Test
    fun connectGhostDoesNotTouchStateLists() {
        val a = VoltageState()
        val b = VoltageState()
        val bipole = TestBipole()

        bipole.connectGhostTo(a, b)
        assertFalse(a.connectedComponents.contains(bipole))
        assertFalse(b.connectedComponents.contains(bipole))
    }

    @Test
    fun voltageUsesPinStates() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 3.5
        b.state = 1.0

        val bipole = TestBipole().connectTo(a, b)
        assertClose(2.5, bipole.voltage)
    }
}

class MonopoleTest {
    @Test
    fun connectToRegistersComponent() {
        val state = VoltageState()
        val monopole = TestMonopole()

        monopole.connectTo(state)
        assertTrue(state.connectedComponents.contains(monopole))
        assertEquals(state, monopole.connectedStates.first())
    }
}

class ResistorTest {
    @Test
    fun resistanceAndCurrentBehavior() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 5.0
        b.state = 0.0

        val resistor = Resistor(a, b).apply { resistance = 10.0 }
        assertClose(10.0, resistor.resistance)
        assertClose(0.1, resistor.resistanceInverse)
        assertClose(0.5, resistor.current)

        resistor.setResistance(Double.NaN)
        assertClose(10.0, resistor.resistance)
    }

    @Test
    fun applyToSubsystemBuildsMatrix() {
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b).apply { resistance = 2.0 }

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(resistor)

        val snapshot = subSystem.captureDebugSnapshot()
        val g = 0.5
        assertClose(g, matrixValue(snapshot, a, a))
        assertClose(-g, matrixValue(snapshot, a, b))
        assertClose(g, matrixValue(snapshot, b, b))
        assertClose(-g, matrixValue(snapshot, b, a))
    }
}

class CapacitorTest {
    @Test
    fun energyAndMatrixBehavior() {
        val a = VoltageState()
        val b = VoltageState()
        a.state = 3.0
        b.state = 0.0

        val capacitor = Capacitor(a, b)
        capacitor.coulombs = 2.0
        assertClose(9.0, capacitor.energy)

        val subSystem = SubSystem(null, 0.5)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(capacitor)

        val snapshot = subSystem.captureDebugSnapshot()
        val g = 4.0
        assertClose(g, matrixValue(snapshot, a, a))
        assertClose(-g, matrixValue(snapshot, a, b))
        assertClose(g, matrixValue(snapshot, b, b))
        assertClose(-g, matrixValue(snapshot, b, a))
    }

    @Test
    fun simProcessIInjectsRhs() {
        val a = VoltageState()
        val b = VoltageState()
        val capacitor = Capacitor(a, b)
        capacitor.coulombs = 2.0

        val subSystem = SubSystem(null, 0.5)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(capacitor)
        subSystem.generateMatrix()

        a.state = 5.0
        b.state = 1.0
        capacitor.simProcessI(subSystem)

        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertClose(16.0, rhs[a.id])
        assertClose(-16.0, rhs[b.id])
    }
}

class InductorTest {
    @Test
    fun energyAndMatrixBehavior() {
        val a = VoltageState()
        val b = VoltageState()
        val inductor = Inductor("L1", a, b)
        inductor.inductance = 2.0
        inductor.currentState.state = 3.0
        assertClose(9.0, inductor.energy)

        val subSystem = SubSystem(null, 0.5)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(inductor)

        val snapshot = subSystem.captureDebugSnapshot()
        val currentState = inductor.currentState
        assertClose(-4.0, matrixValue(snapshot, currentState, currentState))
    }

    @Test
    fun simProcessISetsCurrentRhs() {
        val a = VoltageState()
        val b = VoltageState()
        val inductor = Inductor("L2", a, b)
        inductor.inductance = 2.0

        val subSystem = SubSystem(null, 0.5)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(inductor)
        subSystem.generateMatrix()

        inductor.currentState.state = 3.0
        inductor.simProcessI(subSystem)

        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertClose(-12.0, rhs[inductor.currentState.id])
    }
}

class CurrentSourceTest {
    @Test
    fun simProcessIInjectsCurrent() {
        val a = VoltageState()
        val b = VoltageState()
        val source = CurrentSource("I1", a, b).apply { current = 2.0 }

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(source)
        subSystem.generateMatrix()

        source.simProcessI(subSystem)
        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertClose(2.0, rhs[a.id])
        assertClose(-2.0, rhs[b.id])
    }
}

class VoltageSourceTest {
    @Test
    fun matrixAndRhsBehavior() {
        val a = VoltageState()
        val b = VoltageState()
        val source = VoltageSource("V1", a, b).apply { voltage = 12.0 }

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(source)

        val snapshot = subSystem.captureDebugSnapshot()
        val currentState = source.currentState
        assertClose(1.0, matrixValue(snapshot, a, currentState))
        assertClose(-1.0, matrixValue(snapshot, b, currentState))
        assertClose(1.0, matrixValue(snapshot, currentState, a))
        assertClose(-1.0, matrixValue(snapshot, currentState, b))

        subSystem.generateMatrix()
        source.simProcessI(subSystem)
        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertClose(12.0, rhs[currentState.id])

        source.currentState.state = -0.3
        assertClose(0.3, source.current)
    }
}

class DelayTest {
    @Test
    fun impedanceAndProcessBehavior() {
        val a = VoltageState()
        val b = VoltageState()
        val delay = Delay()
        delay.setImpedance(2.0)
        delay.connectTo(a, b)

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(delay)

        val snapshot = subSystem.captureDebugSnapshot()
        assertClose(0.5, matrixValue(snapshot, a, a))
        assertClose(0.5, matrixValue(snapshot, b, b))

        subSystem.generateMatrix()
        a.state = 2.0
        b.state = 0.0
        delay.simProcessI(subSystem)
        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertClose(0.0, rhs[a.id])
        assertClose(1.0, rhs[b.id])
        assertClose(1.0, delay.current)
    }
}

class ResistorSwitchTest {
    @Test
    fun stateControlsResistance() {
        val a = VoltageState()
        val b = VoltageState()
        val sw = ResistorSwitch("sw", a, b)
        // Use the explicit setter; property access hits the field and skips resistance updates.
        sw.setOffResistance(100.0)
        sw.setResistance(10.0)
        assertClose(100.0, sw.resistance)

        @Suppress("UsePropertyAccessSyntax")
        sw.setState(true)
        assertClose(10.0, sw.resistance)
        @Suppress("UsePropertyAccessSyntax")
        assertTrue(sw.getState())
    }

    @Test
    fun highImpedanceDoesNotLockSwitchState() {
        val a = VoltageState()
        val b = VoltageState()
        val sw = ResistorSwitch("sw", a, b)
        // Use the explicit setter; property access hits the field and skips resistance updates.
        sw.setResistance(25.0)
        sw.setOffResistance(150.0)
        @Suppress("UsePropertyAccessSyntax")
        sw.setState(true)

        sw.highImpedance()
        assertClose(150.0, sw.resistance)
        @Suppress("UsePropertyAccessSyntax")
        assertTrue(sw.getState())

        @Suppress("UsePropertyAccessSyntax")
        sw.setState(false)
        assertClose(150.0, sw.resistance)

        @Suppress("UsePropertyAccessSyntax")
        sw.setState(true)
        assertClose(25.0, sw.resistance)
    }

    @Test
    fun offResistanceCanUseUltraImpedance() {
        val a = VoltageState()
        val b = VoltageState()
        val sw = ResistorSwitch("sw", a, b)
        sw.setResistance(10.0)
        sw.setOffResistance(mods.eln.sim.mna.misc.MnaConst.ultraImpedance)
        @Suppress("UsePropertyAccessSyntax")
        sw.setState(false)

        assertClose(mods.eln.sim.mna.misc.MnaConst.ultraImpedance, sw.resistance)
    }

    @Test
    fun nbtRoundTrip() {
        val a = VoltageState()
        val b = VoltageState()
        val sw = ResistorSwitch("sw", a, b)
        sw.setResistance(12.0)
        @Suppress("UsePropertyAccessSyntax")
        sw.setState(true)

        val nbt = NBTTagCompound()
        sw.writeToNBT(nbt, "pfx")

        val other = ResistorSwitch("sw", a, b)
        other.readFromNBT(nbt, "pfx")
        @Suppress("UsePropertyAccessSyntax")
        assertTrue(other.getState())
        assertClose(12.0, other.resistance)
    }
}

class PowerSourceTest {
    @Test
    fun rootPreStepSetsVoltageFromThevenin() {
        val root = RootSystem(0.1, 1)
        val subSystem = SubSystem(root, 0.1)
        root.systems.add(subSystem)

        val node = VoltageState()
        val resistor = Resistor(node, null).apply { resistance = 10.0 }
        val source = PowerSource("ps", node)
        source.power = 5.0
        source.setMaximumVoltage(100.0)
        source.setMaximumCurrent(100.0)

        subSystem.addState(node)
        subSystem.addComponent(resistor)
        subSystem.addComponent(source)

        val th = subSystem.getTh(node, source)
        val expected = (sqrt(th.voltage * th.voltage + 4.0 * source.power * th.resistance) + th.voltage) / 2.0

        source.rootSystemPreStepProcess()
        assertClose(expected, source.voltage)
    }
}

class TransformerTest {
    @Test
    fun ratioAffectsMatrix() {
        val a = VoltageState()
        val b = VoltageState()
        val transformer = Transformer(a, b)
        transformer.ratio = 2.0

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(transformer)

        val snapshot = subSystem.captureDebugSnapshot()
        val aCurrent = transformer.aCurrentState
        val bCurrent = transformer.bCurrentState

        assertClose(-2.0, matrixValue(snapshot, bCurrent, a))
        assertClose(-0.5, matrixValue(snapshot, aCurrent, b))
        assertClose(2.0, matrixValue(snapshot, aCurrent, bCurrent))
        assertClose(2.0, matrixValue(snapshot, bCurrent, bCurrent))
    }
}

class LineAndInterSystemTest {
    @Test
    fun lineAggregatesResistorsAndFlushes() {
        val root = RootSystem(0.1, 1)
        val s0 = VoltageStateLineReady()
        val s1 = VoltageStateLineReady()
        val s2 = VoltageStateLineReady()

        val r1 = Resistor(s0, s1).apply { resistance = 2.0 }
        val r2 = Resistor(s1, s2).apply { resistance = 2.0 }

        val resistors = java.util.LinkedList<Resistor>()
        resistors.add(r1)
        resistors.add(r2)
        val states = java.util.LinkedList<State>()
        states.add(s1)

        root.addComponents.addAll(resistors)
        root.addStates.addAll(states)

        Line.newLine(root, resistors, states)
        val line = root.addComponents.firstOrNull { it is Line } as Line?
        assertNotNull(line)
        assertClose(4.0, line.resistance)

        s0.state = 4.0
        s2.state = 0.0
        line.simProcessFlush()
        assertClose(2.0, s1.state)
    }

    @Test
    fun interSystemFlagsReplacement() {
        val interSystem = InterSystem()
        assertTrue(interSystem.canBeReplacedByInterSystem())
    }
}

class InterSystemAbstractionTest {
    @Test
    fun constructorCalibratesAndAbstracts() {
        val root = RootSystem(0.1, 1)
        val aSystem = SubSystem(root, 0.1)
        val bSystem = SubSystem(root, 0.1)

        val aState = VoltageState()
        val bState = VoltageState()
        aState.state = 2.0
        bState.state = 0.0
        aSystem.addState(aState)
        bSystem.addState(bState)

        val interSystemResistor = Resistor(aState, bState).apply { resistance = 4.0 }
        val abstraction = InterSystemAbstraction(root, interSystemResistor)

        assertNotNull(interSystemResistor.abstractedBy)
        assertTrue(aSystem.interSystemConnectivity.contains(bSystem))
        assertTrue(bSystem.interSystemConnectivity.contains(aSystem))
        assertClose(1.0, abstraction.aNewDelay.voltage)
        assertClose(1.0, abstraction.bNewDelay.voltage)
        assertClose(2.0, abstraction.aNewResistor.resistance)
        assertClose(2.0, abstraction.bNewResistor.resistance)

        abstraction.destruct()
        assertNull(interSystemResistor.abstractedBy)
    }
}
