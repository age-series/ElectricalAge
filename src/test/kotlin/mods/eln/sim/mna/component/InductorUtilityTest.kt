package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.disableLog4jJmx
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.NBTTagCompound

class InductorUtilityTest {
    @Test
    fun nameConstructorAndInductanceAccessor() {
        val inductor = Inductor("L1")
        inductor.inductance = 2.0
        assertEquals(2.0, inductor.inductance)
    }

    @Test
    fun quitSubSystemRemovesCurrentStateAndProcess() {
        disableLog4jJmx()
        val a = VoltageState()
        val b = VoltageState()
        val inductor = Inductor("L2", a, b)
        inductor.inductance = 1.0

        val subSystem = SubSystem(null, 0.1)
        subSystem.addState(a)
        subSystem.addState(b)
        subSystem.addComponent(inductor)
        subSystem.generateMatrix()

        val currentStateId = inductor.currentState.id
        inductor.quitSubSystem()

        assertEquals(false, subSystem.states.any { it.id == currentStateId })
    }

    @Test
    fun nbtRoundTripAndResetStates() {
        val inductor = Inductor("L3")
        inductor.currentState.state = 1.25

        val nbt = NBTTagCompound()
        inductor.writeToNBT(nbt, "pfx")

        val other = Inductor("L3")
        other.readFromNBT(nbt, "pfx")
        assertEquals(1.25, other.currentState.state)

        other.resetStates()
        assertEquals(0.0, other.currentState.state)
    }
}
