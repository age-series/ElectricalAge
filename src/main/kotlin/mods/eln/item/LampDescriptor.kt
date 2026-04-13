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
        LampLists.registeredLampList.add(lampData)
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack())
    }

    fun getLifeInTag(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) stack.tagCompound = getDefaultNBT()

        return if (stack.tagCompound.hasKey("life")) stack.tagCompound.getDouble("life")
        else 24.0 // default 24 hours
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

    fun decreaseLampLife(lampStack: ItemStack, appliedVoltage: Double): Double {
        var currentLife = getLifeInTag(lampStack)

        if (currentLife > lampData.technology.nominalLifeInHours) {
            setLifeInTag(lampStack, lampData.technology.nominalLifeInHours)
            currentLife = getLifeInTag(lampStack)
        }

        // See https://www.desmos.com/calculator/0uuzozsiuu for a plot of the lamp aging function.
        if (!lampData.technology.infiniteLifeEnabled) {
            // Divide by 3600 to convert seconds to hours
            val lifeLost = when {
                // Life lost per second increases exponentially when voltage is above nominal (10x as fast at 1.25x nominal)
                abs(appliedVoltage) > lampData.nominalU -> {
                    10.0.pow(4.0 / lampData.nominalU).pow(abs(appliedVoltage) - lampData.nominalU) / 3600.0
                }
                // Life lost per second increases linearly when voltage is between nominal and minimal
                abs(appliedVoltage) in (lampData.nominalU * lampData.technology.minimalUFactor)..lampData.nominalU -> {
                    val slope = 1.0 / (lampData.nominalU * (1.0 - lampData.technology.minimalUFactor))
                    val intercept = 1.0 - (slope * lampData.nominalU)

                    ((slope * abs(appliedVoltage)) + intercept) / 3600.0
                }
                // Lamp does not lose life when voltage is below minimal (no light produced)
                else -> 0.0
            }

            var newLife = currentLife - lifeLost
            if (newLife < 0.0) newLife = 0.0

            setLifeInTag(lampStack, newLife)
            return newLife
        }

        return currentLife
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(tr("Power: %1\$W", Utils.plotValue(lampData.nominalP)))
        list.add(tr("Resistance: %1\$\u2126", Utils.plotValue(lampData.resistance)))
        list.add(tr("Nominal lifetime: %1\$h", lampData.technology.nominalLifeInHours))

        if (itemStack != null) {
            if (Eln.config.getBooleanOrElse("debug.logging.enabled", false)) list.add(tr("Current lifetime: %1\$h", getLifeInTag(itemStack)))
            list.add(tr("Condition: %1\$", getLampCondition(itemStack)))
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