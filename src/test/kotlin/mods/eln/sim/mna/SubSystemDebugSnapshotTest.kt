package mods.eln.sim.mna

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.state.VoltageState

class SubSystemDebugSnapshotTest {
    @Test
    fun snapshotCapturesOwnersLabelsAndConnections() {
        disableLog4jJmx()
        val a = VoltageState().setOwner("node-a")
        val b = VoltageState().setOwner("node-b")
        a.state = 3.25
        b.state = 1.0
        val resistor = Resistor(a, b).setOwner("R1")

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(resistor)

        val snapshot = subSystem.captureDebugSnapshot()

        assertTrue(snapshot.isSingular)
        assertEquals(2, snapshot.conductanceMatrix.size)
        assertTrue(snapshot.stateLabels.any { it.contains("VoltageState") })
        assertTrue(snapshot.stateLabels.any { it.contains("node-a") })
        assertTrue(snapshot.stateLabels.any { it.contains("node-b") })

        val owners = snapshot.stateOwners.toSet()
        assertTrue(owners.contains("node-a"))
        assertTrue(owners.contains("node-b"))

        val componentLabel = snapshot.componentLabels.single()
        assertTrue(componentLabel.contains("Resistor"))
        assertTrue(componentLabel.contains("R1"))

        val connections = snapshot.componentConnections.single()
        assertEquals(setOf(a.id, b.id), connections.toSet())
    }
}
