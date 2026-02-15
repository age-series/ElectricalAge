package mods.eln.sim.mna.component

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.VoltageState

class DelayUtilityTest {
    @Test
    fun setImpedanceUpdatesConductanceAndCurrent() {
        mods.eln.disableLog4jJmx()
        val a = VoltageState()
        val b = VoltageState()
        val delay = Delay()
        delay.setImpedance(4.0)
        delay.connectTo(a, b)

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(delay)
        subSystem.generateMatrix()

        a.state = 2.0
        b.state = 0.0
        delay.simProcessI(subSystem)

        assertEquals(0.5, delay.current)
    }
}
