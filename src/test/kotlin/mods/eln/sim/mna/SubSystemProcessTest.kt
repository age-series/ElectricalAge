package mods.eln.sim.mna

import mods.eln.disableLog4jJmx
import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.state.VoltageState

class SubSystemProcessTest {
    @Test
    fun addProcessRegistersAndRuns() {
        disableLog4jJmx()
        val subSystem = SubSystem(null, 0.1)
        val node = VoltageState()
        val source = VoltageSource("V").apply { connectTo(node, null) }
        source.voltage = 1.0
        val resistor = Resistor(node, null).apply { resistance = 10.0 }
        var processCount = 0

        val process = ISubSystemProcessI {
            processCount++
        }

        subSystem.addState(node)
        subSystem.addComponent(source)
        subSystem.addComponent(resistor)
        subSystem.addProcess(process)

        subSystem.step()
        assertEquals(1, processCount)

        subSystem.removeProcess(process)
        subSystem.step()
        assertEquals(1, processCount)
    }
}
