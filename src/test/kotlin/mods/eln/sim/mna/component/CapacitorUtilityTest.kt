package mods.eln.sim.mna.component

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.VoltageState

class CapacitorUtilityTest {
    @Test
    fun defaultConstructorHasZeroCurrentAndCoulombs() {
        val capacitor = Capacitor()
        assertEquals(0.0, capacitor.current)
        assertEquals(0.0, capacitor.coulombs)
    }

    @Test
    fun quitSubSystemRemovesProcess() {
        mods.eln.disableLog4jJmx()
        val a = VoltageState()
        val b = VoltageState()
        val capacitor = Capacitor(a, b)
        capacitor.setCoulombs(1.0)

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(capacitor)

        subSystem.generateMatrix()
        capacitor.quitSubSystem()

        subSystem.step()
        val rhs = subSystem.captureDebugSnapshot().rhsVector
        assertEquals(0.0, rhs[a.id])
        assertEquals(0.0, rhs[b.id])
    }
}
