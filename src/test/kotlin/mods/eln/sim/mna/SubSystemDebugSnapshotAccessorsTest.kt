package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class SubSystemDebugSnapshotAccessorsTest {
    @Test
    fun snapshotAccessorsExposeData() {
        disableLog4jJmx()
        val a = VoltageState()
        val b = VoltageState()
        val resistor = Resistor(a, b)

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(resistor)

        val snapshot = subSystem.captureDebugSnapshot()
        assertEquals(snapshot.conductanceMatrix, snapshot.getConductanceMatrix())
        assertEquals(snapshot.rhsVector, snapshot.getRhsVector())
        assertEquals(snapshot.stateLabels, snapshot.getStateLabels())
        assertEquals(snapshot.stateOwners, snapshot.getStateOwners())
        assertEquals(snapshot.componentLabels, snapshot.getComponentLabels())
        assertEquals(snapshot.componentOwners, snapshot.getComponentOwners())
        assertEquals(snapshot.componentConnections, snapshot.getComponentConnections())
        assertEquals(snapshot.isSingular, snapshot.isSingular())
    }
}
