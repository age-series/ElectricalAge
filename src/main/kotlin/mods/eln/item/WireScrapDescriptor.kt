package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class WireScrapDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "Copper Cable") {
    companion object {
        private const val nbtSourceName = "wireScrapSourceName"
        private const val nbtMaterial = "wireScrapMaterial"
        private const val nbtSize = "wireScrapSize"
        private const val nbtMetricSize = "wireScrapMetricSize"
        private const val nbtConductorCount = "wireScrapConductors"
        private const val nbtInsulated = "wireScrapInsulated"
    }

    fun createScrapStack(cable: UtilityCableDescriptor, count: Int = 1): ItemStack {
        val stack = newItemStack(count)
        stack.tagCompound = NBTTagCompound().apply {
            setString(nbtSourceName, cable.name)
            setString(nbtMaterial, cable.material.label)
            setString(nbtSize, cable.sizeLabel)
            setString(nbtMetricSize, cable.metricSizeLabel)
            setInteger(nbtConductorCount, cable.conductorCount)
            setBoolean(nbtInsulated, cable.insulated)
        }
        return stack
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        val nbt = itemStack?.tagCompound ?: return
        list.add(tr("Recovered from: %1$", nbt.getString(nbtSourceName)))
        list.add(tr("Material: %1$", nbt.getString(nbtMaterial)))
        list.add(
            tr(
                "Type: %1$ (%2$ mm2), %3$ conductors, %4$",
                nbt.getString(nbtSize),
                nbt.getString(nbtMetricSize),
                nbt.getInteger(nbtConductorCount),
                if (nbt.getBoolean(nbtInsulated)) tr("insulated") else tr("bare")
            )
        )
    }
}
