package mods.eln.sim.mna.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.VoltageState
import net.minecraft.nbt.NBTTagCompound

class ResistorSwitchNbtEdgeTest {
    @Test
    fun invalidNbtResistanceDefaultsToOffResistance() {
        val a = VoltageState()
        val b = VoltageState()
        val sw = ResistorSwitch("sw", a, b)
        sw.setOffResistance(50.0)

        val nbt = NBTTagCompound()
        nbt.setDouble("pfxswR", 0.0)
        nbt.setBoolean("pfxswState", true)
        sw.readFromNBT(nbt, "pfx")

        assertEquals(50.0, sw.resistance)
        assertTrue(sw.getState())
    }

    @Test
    fun highImpedanceUsesOffResistance() {
        val a = VoltageState()
        val b = VoltageState()
        val sw = ResistorSwitch("sw", a, b)
        sw.setOffResistance(MnaConst.ultraImpedance)
        sw.highImpedance()
        assertEquals(MnaConst.ultraImpedance, sw.resistance)
    }
}
