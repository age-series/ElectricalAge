package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.disableLog4jJmx
import mods.eln.sim.mna.state.VoltageState

class PowerSourceUtilityTest {
    @Test
    fun nanPowerFallsBackToZeroVoltage() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val subSystem = SubSystem(root, 0.1)
        root.systems.add(subSystem)

        val node = VoltageState()
        val source = PowerSource("ps", node)
        source.setPower(Double.NaN)
        source.setMaximumVoltage(100.0)
        source.setMaximumCurrent(10.0)

        subSystem.addState(node)
        subSystem.addComponent(Resistor(node, null).setResistance(10.0))
        subSystem.addComponent(source)

        source.rootSystemPreStepProcess()
        assertEquals(0.0, source.voltage)
    }

    @Test
    fun quitSubSystemRemovesRootProcess() {
        disableLog4jJmx()
        val root = RootSystem(0.1, 1)
        val subSystem = SubSystem(root, 0.1)
        root.systems.add(subSystem)

        val node = VoltageState()
        val source = PowerSource("ps", node)
        source.setPower(1.0)
        source.setMaximumVoltage(10.0)
        source.setMaximumCurrent(1.0)

        subSystem.addState(node)
        subSystem.addComponent(Resistor(node, null).setResistance(5.0))
        subSystem.addComponent(source)

        source.voltage = 4.0
        source.quitSubSystem()

        assertEquals(null, source.subSystem)
    }
}
