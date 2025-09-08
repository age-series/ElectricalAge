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

class LampDescriptor(
    name: String, iconName: String,
    type: Type, socket: LampSocketType,
    nominalU: Double, nominalP: Double, nominalLight: Double, nominalLife: Double,
    vegetableGrowRate: Double) : GenericItemUsingDamageDescriptorUpgrade(name), IConfigSharing {
    enum class Type {
        INCANDESCENT, ECO, LED
    }

    var nominalP: Double
    var nominalLight: Double
    var nominalLifeHours: Double
    @JvmField
    var type: Type
    @JvmField
    var socket: LampSocketType
    var nominalU: Double
    var minimalU = 0.0
    var stableU = 0.0
    var stableUNormalised = 0.0
    var stableTime = 0.0
    var vegetableGrowRate: Double
    var serverNominalLife = 0.0
    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack())
    }

    val r: Double
        get() = nominalU * nominalU / nominalP

    fun getLifeInTag(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) stack.tagCompound = getDefaultNBT()
        return if (stack.tagCompound.hasKey("life")) stack.tagCompound.getDouble("life") else {
            32.0 * 60.0 * 60.0 * 20.0
        } // 32 hours * 60 * 60 seconds/hour * 20 ticks/second
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

    override fun newItemStack(size: Int): ItemStack {
        return super.newItemStack(size)
    }

    fun applyTo(resistor: Resistor) {
        resistor.resistance = r
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Technology: %1$", type))
        list.add(tr("Range: %1$ blocks", (nominalLight * 15).toInt()))
        list.add(tr("Power: %1\$W", Utils.plotValue(nominalP)))
        list.add(tr("Resistance: %1$\u2126", Utils.plotValue(r)))
        list.add(tr("Nominal lifetime: %1\$h", serverNominalLife))
        if (itemStack != null) {
            if (!itemStack.hasTagCompound() || !itemStack.tagCompound.hasKey("life")) {
                list.add(tr("Condition:") + " " + tr("New"))
            } else if (getLifeInTag(itemStack) > 0.5) {
                list.add(tr("Condition:") + " " + tr("Good"))
            } else if (getLifeInTag(itemStack) > 0.2) {
                list.add(tr("Condition:") + " " + tr("Used"))
            } else if (getLifeInTag(itemStack) > 0.1) {
                list.add(tr("Condition:") + " " + tr("End of life"))
            } else {
                list.add(tr("Condition:") + " " + tr("Bad"))
            }
            if (Eln.debugEnabled)
                list.add("Life: ${getLifeInTag(itemStack)}")
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

    init {
        setDefaultIcon(iconName)
        this.type = type
        this.socket = socket
        this.nominalU = nominalU
        this.nominalP = nominalP
        this.nominalLight = nominalLight
        this.nominalLifeHours = nominalLife
        this.vegetableGrowRate = vegetableGrowRate
        when (type) {
            Type.INCANDESCENT -> minimalU = nominalU * 0.5
            Type.ECO -> {
                stableUNormalised = 0.75
                minimalU = nominalU * 0.5
                stableU = nominalU * stableUNormalised
                stableTime = 4.0
            }
            Type.LED -> minimalU = nominalU * 0.75
        }
        Eln.instance.configShared.add(this)
        voltageLevelColor = VoltageLevelColor.fromVoltage(nominalU)
    }
}
