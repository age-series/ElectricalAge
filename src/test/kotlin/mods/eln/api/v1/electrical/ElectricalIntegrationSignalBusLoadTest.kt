package mods.eln.api.v1.electrical

import net.minecraft.nbt.NBTTagCompound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElectricalIntegrationSignalBusLoadTest {
    @Test
    fun channelAccessMatchesOperatorAndGetter() {
        val bus = ElectricalIntegration.SignalBusLoad("test.bus") {}
        assertEquals(
            bus.getChannel(ElectricalIntegration.SignalBusChannel.BLUE).name,
            bus[ElectricalIntegration.SignalBusChannel.BLUE].name
        )
    }

    @Test
    fun writeReadNbtUsesStablePerChannelPrefixes() {
        val bus = ElectricalIntegration.SignalBusLoad("test.bus.persist") {}
        val tag = NBTTagCompound()

        bus.writeToNbt(tag, "bus")

        assertTrue(tag.hasKey("bus.redtest.bus.persist.redUc"))
        assertTrue(tag.hasKey("bus.bluetest.bus.persist.blueUc"))
    }
}
