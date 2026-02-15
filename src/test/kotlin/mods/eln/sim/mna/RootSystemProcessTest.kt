package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.ISubSystemProcessFlush

class RootSystemProcessTest {
    @Test
    fun processesRunDuringStep() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 3)
        var preCount = 0
        var flushCount = 0

        val pre = IRootSystemPreStepProcess { preCount++ }
        val flush = ISubSystemProcessFlush { flushCount++ }

        root.addProcess(pre)
        root.addProcess(flush)
        root.step()

        assertEquals(3, preCount)
        assertEquals(1, flushCount)

        root.removeProcess(pre)
        root.removeProcess(flush)
        root.step()

        assertEquals(3, preCount)
        assertEquals(1, flushCount)
    }
}
