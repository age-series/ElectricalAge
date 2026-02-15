package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import net.minecraft.nbt.NBTTagCompound

class VoltageSourceNbtTest {
    @Test
    fun nbtRoundTripPreservesVoltageAndCurrentState() {
        val source = VoltageSource("v")
        source.voltage = 3.5
        source.currentState.state = -0.25

        val nbt = NBTTagCompound()
        source.writeToNBT(nbt, "pfx")

        val other = VoltageSource("v")
        other.readFromNBT(nbt, "pfx")

        assertEquals(3.5, other.voltage)
        assertEquals(-0.25, other.currentState.state)
        assertEquals(source.power, other.power)
    }
}
