package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.sixnode.electricalcable.UtilityCableMaterial
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class RollerWheelDescriptor(name: String, private val materialName: String, iconName: String) : GenericItemUsingDamageDescriptor(name, iconName) {
    fun matchesMaterial(stack: ItemStack?, material: UtilityCableMaterial): Boolean {
        return stack != null && checkSameItemStack(stack) && material.label.equals(materialName, ignoreCase = true)
    }
}

class InsulationCompoundDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "ingotrubber") {
    companion object {
        const val METERS_PER_ITEM = 64.0
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Provides %1$ m of wire insulation", METERS_PER_ITEM.toInt()))
    }
}

class WoundWireBundleDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "coppercable") {
    companion object {
        private const val NBT_TARGET_LABEL = "wireBundleTargetLabel"
        private const val NBT_TARGET_METRIC = "wireBundleTargetMetric"
        private const val NBT_MATERIAL = "wireBundleMaterial"
        private const val NBT_CONDUCTOR_COUNT = "wireBundleConductors"
        private const val NBT_AREA = "wireBundleArea"
        private const val NBT_LENGTH = "wireBundleLengthMeters"
    }

    fun createBundleStack(
        targetLabel: String,
        targetMetricLabel: String,
        material: UtilityCableMaterial,
        conductorCount: Int,
        areaMm2: Double,
        lengthMeters: Double
    ): ItemStack {
        return newItemStack(1).apply {
            tagCompound = NBTTagCompound().apply {
                setString(NBT_TARGET_LABEL, targetLabel)
                setString(NBT_TARGET_METRIC, targetMetricLabel)
                setString(NBT_MATERIAL, material.label)
                setInteger(NBT_CONDUCTOR_COUNT, conductorCount)
                setDouble(NBT_AREA, areaMm2)
                setDouble(NBT_LENGTH, lengthMeters)
            }
        }
    }

    fun getTargetLabel(stack: ItemStack?): String? = stack?.tagCompound?.getString(NBT_TARGET_LABEL)?.takeIf { it.isNotEmpty() }
    fun getTargetMetricLabel(stack: ItemStack?): String? = stack?.tagCompound?.getString(NBT_TARGET_METRIC)?.takeIf { it.isNotEmpty() }
    fun getMaterial(stack: ItemStack?): UtilityCableMaterial? {
        val label = stack?.tagCompound?.getString(NBT_MATERIAL)?.takeIf { it.isNotEmpty() } ?: return null
        return UtilityCableMaterial.values().firstOrNull { it.label.equals(label, ignoreCase = true) }
    }
    fun getConductorCount(stack: ItemStack?): Int = stack?.tagCompound?.getInteger(NBT_CONDUCTOR_COUNT) ?: 0
    fun getAreaMm2(stack: ItemStack?): Double = stack?.tagCompound?.getDouble(NBT_AREA) ?: 0.0
    fun getLengthMeters(stack: ItemStack?): Double = stack?.tagCompound?.getDouble(NBT_LENGTH) ?: 0.0

    fun setLengthMeters(stack: ItemStack, meters: Double) {
        if (stack.tagCompound == null) stack.tagCompound = NBTTagCompound()
        stack.tagCompound.setDouble(NBT_LENGTH, meters.coerceAtLeast(0.0))
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        val target = getTargetLabel(itemStack) ?: return
        val material = getMaterial(itemStack)?.label ?: return
        list.add(tr("Target: %1$", target))
        list.add(tr("Material: %1$", material))
        list.add(tr("Length: %1$ m", "%.0f".format(getLengthMeters(itemStack))))
    }
}
