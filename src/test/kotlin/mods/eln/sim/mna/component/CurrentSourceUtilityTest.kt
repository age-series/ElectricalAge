package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.disableLog4jJmx
import mods.eln.sim.mna.state.VoltageState

class CurrentSourceUtilityTest {
    private class CurrentSourceWithLocal(name: String) : CurrentSource(name) {
        override fun addToSubsystem(s: SubSystem) {
            super.addToSubsystem(s)
            s.addProcess(this)
        }
    }

    @Test
    fun getCurrentReturnsConfiguredValue() {
        val source = CurrentSource("i").setCurrent(2.5)
        assertEquals(2.5, source.current)
    }

    @Test
    fun quitSubSystemRemovesProcess() {
        disableLog4jJmx()
        val a = VoltageState()
        val b = VoltageState()
        val source = CurrentSourceWithLocal("i").setCurrent(2.0).connectTo(a, b)

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(source)
        subSystem.generateMatrix()

        source.quitSubSystem()

        subSystem.step()
        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertEquals(0.0, rhs[a.id])
        assertEquals(0.0, rhs[b.id])
    }
}
