package mods.eln.item

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.IConfigSharing
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.sim.mna.component.Resistor
import mods.eln.sixnode.lampsocket.LampSocketType
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.pow

class LampDescriptor(
    name: String, iconName: String, @JvmField val type: LampTechnology, @JvmField val socket: LampSocketType,
    val nominalU: Double, val nominalP: Double, val nominalLifeHours: Double, val range: Int
) : GenericItemUsingDamageDescriptorUpgrade(name), IConfigSharing {

    companion object {
        const val MIN_LIGHT_VALUE: Int = 0
        const val MAX_LIGHT_VALUE: Int = 15
    }

    enum class LampTechnology {
        INCANDESCENT, FLUORESCENT, INFRARED, LED, HALOGEN
    }

    val nominalLight = MAX_LIGHT_VALUE / 15.0
    val vegetableGrowRate = if (type == LampTechnology.INFRARED) 0.5 else 0.0

    val minimalU = when (type) {
        LampTechnology.INCANDESCENT, LampTechnology.INFRARED, LampTechnology.HALOGEN -> nominalU * 0.5
        LampTechnology.FLUORESCENT, LampTechnology.LED -> nominalU * 0.75
    }

    val stableUNormalised = if (type == LampTechnology.FLUORESCENT) 0.75 else 0.0
    val stableU = if (type == LampTechnology.FLUORESCENT) nominalU * stableUNormalised else 0.0
    val stableTime = if (type == LampTechnology.FLUORESCENT) 4.0 else 0.0

    val resistance = nominalU.pow(2) / nominalP

    var serverNominalLife = 0.0

    init {
        setDefaultIcon(iconName)
        Eln.instance.configShared.add(this)
        voltageLevelColor = VoltageLevelColor.fromVoltage(nominalU)
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack())
    }

    fun getLifeInTag(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) stack.tagCompound = getDefaultNBT()

        return if (stack.tagCompound.hasKey("life")) {
            stack.tagCompound.getDouble("life")
        } else {
            32.0 * 3600 * 20.0 // 32 hours * 3600 seconds/hour * 20 ticks/second
        }
    }

    fun setLifeInTag(stack: ItemStack, life: Double) {
        if (!stack.hasTagCompound()) stack.tagCompound = getDefaultNBT()
        stack.tagCompound.setDouble("life", life)
    }

    override fun getDefaultNBT(): NBTTagCompound {
        val tag = NBTTagCompound()
        tag.setDouble("life", nominalLifeHours)
        return tag
    }

    fun applyTo(resistor: Resistor) {
        resistor.resistance = resistance
    }

    fun ageLamp(lampStack: ItemStack, voltage: Double, time: Double): Double {
        val ageFactor = (0.000008 * abs(voltage).pow(3.0)) - (0.003225 * abs(voltage).pow(2.0)) + (0.33 * abs(voltage))
        val lifeLost = (ageFactor * time) / 3600.0 // Life lost in hours, per tick

        val currentLife = this.getLifeInTag(lampStack)

        var newLife = currentLife - lifeLost
        if (newLife < 0) newLife = 0.0

        this.setLifeInTag(lampStack, newLife)
        return newLife
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Technology: $type"))
        list.add(tr("Range: $range blocks"))
        list.add(tr("Power: ${Utils.plotValue(nominalP)}W"))
        list.add(tr("Resistance: ${Utils.plotValue(resistance)}\u2126"))
        list.add(tr("Nominal lifetime: ${serverNominalLife}h"))

        if (itemStack != null) {
            if (!itemStack.hasTagCompound() || !itemStack.tagCompound.hasKey("life")) {
                if (getLifeInTag(itemStack) == nominalLifeHours) list.add(tr("Condition: New"))
                else if (getLifeInTag(itemStack) > (0.5 * nominalLifeHours)) list.add(tr("Condition: Good"))
                else if (getLifeInTag(itemStack) > (0.15 * nominalLifeHours)) list.add(tr("Condition: Used"))
                else if (getLifeInTag(itemStack) > (0.01 * nominalLifeHours)) list.add(tr("Condition: Bad"))
                else list.add(tr("Condition: End of life"))
            }

            if (Eln.debugEnabled) list.add(tr("Life: ${getLifeInTag(itemStack)}h"))
        }
    }

    @Throws(IOException::class)
    override fun serializeConfig(stream: DataOutputStream?) {
        stream!!.writeDouble(nominalLifeHours)
    }

    @Throws(IOException::class)
    override fun deserialize(stream: DataInputStream?) {
        serverNominalLife = stream!!.readDouble()
    }

}