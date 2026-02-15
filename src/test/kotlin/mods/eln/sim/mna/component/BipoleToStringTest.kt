package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertTrue
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.State
import mods.eln.sim.mna.state.VoltageState

private class ToStringBipole : Bipole() {
    override fun applyToSubsystem(s: SubSystem) {}
    override fun getCurrent(): Double = 0.0
}

class BipoleToStringTest {
    @Test
    fun toStringIncludesPinsAndClass() {
        val a = VoltageState()
        val b = VoltageState()
        a.id = 1
        b.id = 2
        a.state = 3.0
        b.state = 1.0

        val bipole = ToStringBipole().connectTo(a, b)
        val description = bipole.toString()
        assertTrue(description.contains("ToStringBipole"))
        assertTrue(description.contains("(1,VoltageState)"))
        assertTrue(description.contains("(2,VoltageState)"))
    }
}
