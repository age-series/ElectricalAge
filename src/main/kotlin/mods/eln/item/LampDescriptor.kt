package mods.eln.item

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.sim.mna.component.Resistor
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import kotlin.math.abs
import kotlin.math.pow

class LampDescriptor(name: String, iconName: String, val lampData: SpecificLampData) : GenericItemUsingDamageDescriptorUpgrade(name) {

    init {
        setDefaultIcon(iconName)
        voltageLevelColor = VoltageLevelColor.fromVoltage(lampData.nominalU)

        if (name.contains("Small")) lampData.nominalP /= 2.0
        Eln.lampLists.registeredLampList.add(lampData)
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
        tag.setDouble("life", lampData.technology.nominalLifeInHours)
        return tag
    }

    fun applyTo(resistor: Resistor) {
        resistor.resistance = lampData.resistance
    }

    // There's still a small bug where a lamp item in the player inventory will not update its life if aging is off and
    // the nominal life changes. There probably isn't anything that can really be done about that.
    fun ageLamp(lampStack: ItemStack, voltage: Double, time: Double): Double {
        val currentLife = this.getLifeInTag(lampStack)

        // Force the life of non-aging bulbs to track the config file.
        if (lampData.technology.infiniteLifeEnabled) {
            return if (currentLife != lampData.technology.nominalLifeInHours) {
                this.setLifeInTag(lampStack, lampData.technology.nominalLifeInHours)
                lampData.technology.nominalLifeInHours
            }
            else currentLife
        }
        else {
            val ageFactor = (0.000008 * abs(voltage).pow(3.0)) - (0.003225 * abs(voltage).pow(2.0)) + (0.33 * abs(voltage))
            val lifeLost = (ageFactor * time) / 3600.0 // Life lost in hours, per tick

            // Force the current life of aging bulbs to decrease to nominal if the nominal life is adjusted (via the config file) to be less than the current life.
            var newLife = if (currentLife > lampData.technology.nominalLifeInHours) lampData.technology.nominalLifeInHours else currentLife
            newLife -= lifeLost
            if (newLife < 0) newLife = 0.0

            this.setLifeInTag(lampStack, newLife)
            return newLife
        }
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Power: ${Utils.plotValue(lampData.nominalP)}W"))
        list.add(tr("Resistance: ${Utils.plotValue(lampData.resistance)}\u2126"))
        list.add(tr("Nominal lifetime: ${lampData.technology.nominalLifeInHours}h"))

        if (itemStack != null) {
            if (Eln.debugEnabled) list.add(tr("Current lifetime: ${getLifeInTag(itemStack)}h"))
            list.add(tr("Condition: ${getLampCondition(itemStack)}"))
        }
    }

    private fun getLampCondition(itemStack: ItemStack): String {
        return if (!itemStack.hasTagCompound() || !itemStack.tagCompound.hasKey("life")) {
            "New"
        } else {
            val lampLife = getLifeInTag(itemStack)

            if (lampLife == lampData.technology.nominalLifeInHours) "New"
            else if (lampLife > (0.5 * lampData.technology.nominalLifeInHours)) "Good"
            else if (lampLife > (0.15 * lampData.technology.nominalLifeInHours)) "Used"
            else if (lampLife > (0.01 * lampData.technology.nominalLifeInHours)) "Bad"
            else "End of life"
        }
    }

}