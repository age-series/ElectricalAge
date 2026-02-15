package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import net.minecraft.nbt.NBTTagCompound

class CurrentSourceNbtTest {
    @Test
    fun nbtRoundTripPreservesCurrent() {
        val source = CurrentSource("i")
        source.current = 1.25

        val nbt = NBTTagCompound()
        source.writeToNBT(nbt, "pfx")

        val other = CurrentSource("i")
        other.readFromNBT(nbt, "pfx")

        assertEquals(1.25, other.current)
    }
}
