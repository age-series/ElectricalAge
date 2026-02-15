package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.NBTTagCompound

class PowerSourceNbtTest {
    @Test
    fun nbtRoundTripPreservesSettings() {
        val node = VoltageState()
        val source = PowerSource("ps", node)
        source.setPower(4.0)
        source.setMaximumVoltage(120.0)
        source.setMaximumCurrent(2.5)
        source.setVoltage(48.0)
        source.currentState.state = -0.5

        val nbt = NBTTagCompound()
        source.writeToNBT(nbt, "pfx")

        val other = PowerSource("ps", node)
        other.readFromNBT(nbt, "pfx")

        assertEquals(4.0, other.power)
        assertEquals(120.0, nbt.getDouble("pfxpsUmax"))
        assertEquals(2.5, nbt.getDouble("pfxpsImax"))
        assertEquals(48.0, other.voltage)
        assertEquals(-0.5, other.currentState.state)
        assertEquals(source.effectivePower, other.effectivePower)
    }

    @Test
    fun setMaximumsUpdatesNbt() {
        val node = VoltageState()
        val source = PowerSource("ps", node)
        source.setMaximums(11.0, 12.0)

        val nbt = NBTTagCompound()
        source.writeToNBT(nbt, "pfx")

        assertEquals(11.0, nbt.getDouble("pfxpsUmax"))
        assertEquals(12.0, nbt.getDouble("pfxpsImax"))
    }
}
