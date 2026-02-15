package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.ISubSystemProcessFlush
import mods.eln.sim.mna.state.VoltageState

class SubSystemFlushProcessTest {
    @Test
    fun removeProcessStopsFlushCallback() {
        disableLog4jJmx()
        val subSystem = SubSystem(null, 0.1)
        val node = VoltageState()
        val resistor = Resistor(node, null).apply { resistance = 10.0 }
        subSystem.addState(node)
        subSystem.addComponent(resistor)
        subSystem.generateMatrix()

        var flushCount = 0
        val flush = ISubSystemProcessFlush { flushCount++ }
        subSystem.addProcess(flush)
        subSystem.stepFlush()
        assertEquals(1, flushCount)

        subSystem.removeProcess(flush)
        subSystem.stepFlush()
        assertEquals(1, flushCount)
    }
}
