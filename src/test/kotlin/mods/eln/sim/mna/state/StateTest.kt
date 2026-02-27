package mods.eln.sim.mna.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.IAbstractor

class StateTest {
    @Test
    fun stateFlagsAndAbstractorBehavior() {
        val state = State()
        assertTrue(!state.isPrivateSubSystem)
        assertTrue(!state.mustBeFarFromInterSystem())

        state.owner = "owner-a"
        assertEquals("owner-a", state.owner)

        state.setAsPrivate()
        state.setAsMustBeFarFromInterSystem()
        assertTrue(state.isPrivateSubSystem)
        assertTrue(state.mustBeFarFromInterSystem())

        val subSystem = SubSystem(null, 0.1)
        state.setSubsystem(subSystem)
        assertEquals(subSystem, state.subSystem)

        val abstractedSub = SubSystem(null, 0.2)
        state.abstractedBy = object : IAbstractor {
            override fun dirty(component: Component) {}
            override fun getAbstractorSubSystem(): SubSystem = abstractedSub
        }

        assertTrue(state.isAbstracted)
        assertEquals(abstractedSub, state.getSubSystem())
    }

    @Test
    fun returnToRootSystemAddsState() {
        val root = RootSystem(0.1, 1)
        val state = State()
        state.returnToRootSystem(root)
        assertTrue(root.addStates.contains(state))
    }

    @Test
    fun isNotSimulatedTracksSubsystemState() {
        val state = State()
        assertTrue(state.isNotSimulated)

        val subSystem = SubSystem(null, 0.1)
        state.setSubsystem(subSystem)
        assertTrue(!state.isNotSimulated)

        state.quitSubSystem()
        assertTrue(state.isNotSimulated)

        val abstractedSub = SubSystem(null, 0.2)
        state.abstractedBy = object : IAbstractor {
            override fun dirty(component: Component) {}
            override fun getAbstractorSubSystem(): SubSystem = abstractedSub
        }
        assertTrue(!state.isNotSimulated)
        assertEquals(abstractedSub, state.getSubSystem())
    }
}
