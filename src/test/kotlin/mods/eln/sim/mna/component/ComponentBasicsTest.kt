package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.state.State

private class SimpleComponent : Component() {
    override fun applyToSubsystem(s: mods.eln.sim.mna.SubSystem) {}
    override fun getConnectedStates(): Array<State> = emptyArray()
}

class ComponentBasicsTest {
    @Test
    fun ownerToStringAndReturnToRoot() {
        val component = SimpleComponent().setOwner("owner-1")
        assertEquals("owner-1", component.owner)
        assertTrue(component.toString().contains("SimpleComponent"))
        assertFalse(component.canBeReplacedByInterSystem())

        val root = RootSystem(0.1, 1)
        component.returnToRootSystem(root)
        assertTrue(root.addComponents.contains(component))
    }

    @Test
    fun breakConnectionIsNoOpByDefault() {
        val component = SimpleComponent()
        component.breakConnection()
    }
}
