package mods.eln.item.lampitem

import mods.eln.i18n.I18N
import mods.eln.item.GenericItemUsingDamageDescriptorUpgrade
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
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

    /**
     * See https://www.desmos.com/calculator/0uuzozsiuu for a plot of the life lost vs. applied voltage curve.
     */
    fun decreaseLampLife(lampStack: ItemStack, appliedVoltage: Double): Double {
        val currentLife = getLifeInTag(lampStack)

        // resetLampLifeFlag should be automatically set to false after ~1 tick (see usage in Simulator.java)
        if (currentLife > lampData.technology.nominalLifeInHours || LampLists.resetLampLifeFlag) {
            setLifeInTag(lampStack, lampData.technology.nominalLifeInHours)
            return getLifeInTag(lampStack)
        }

        // Lamp aging only occurs when the shift key is not held. This prevents the infamous NBT mismatch/desync bug
        // from occurring, where an item is duplicated when shift-clicked from an inventory if its NBT tags differ
        // between the client and the server.
        if (!lampData.technology.infiniteLifeEnabled && !UtilsClient.isShiftHeld()) {
            var lifeLost: Double

            if (abs(appliedVoltage) > lampData.nominalU) {
                // Life lost per tick increases exponentially when voltage is above nominal (10x as fast at 1.25x nominal)
                lifeLost = 10.0.pow(4.0 / lampData.nominalU).pow(abs(appliedVoltage) - lampData.nominalU)
            } else if (abs(appliedVoltage) in (lampData.nominalU * lampData.technology.minimalUFactor)..lampData.nominalU) {
                // Life lost per tick increases linearly when voltage is between nominal and minimal
                val slope = 1.0 / (lampData.nominalU * (1.0 - lampData.technology.minimalUFactor))
                val intercept = 1.0 - (slope * lampData.nominalU)
                lifeLost = (slope * abs(appliedVoltage)) + intercept
            } else {
                // Lamp does not lose life when voltage is below minimal (no light produced)
                lifeLost = 0.0
            }

            // Division by 72,000 = 20 * 3600 converts ticks to hours (20 ticks/second, 3600 seconds/hour)
            // Bulb lives are defined in hours, so this conversion is necessary for losing life at the proper rate
            lifeLost /= (20.0 * 3600.0)

            var newLife = currentLife - lifeLost
            if (newLife < 0.0) newLife = 0.0

            setLifeInTag(lampStack, newLife)
            return newLife
        } else {
            return currentLife
        }
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list.add(I18N.tr($$"Nominal voltage: %1$V", Utils.plotValue(lampData.nominalU)))
        list.add(I18N.tr($$"Nominal power: %1$W", Utils.plotValue(lampData.nominalP)))
        list.add(I18N.tr("Resistance: %1$\u2126", Utils.plotValue(lampData.resistance)))
        list.add(I18N.tr("Nominal brightness: %1$", Utils.plotValue(lampData.nominalLightValue.toDouble())))
        list.add(I18N.tr($$"Nominal lifetime: %1$h", lampData.technology.nominalLifeInHours))

        if (itemStack != null) {
            if (Utils.isDebugEnabled()) list.add(I18N.tr($$"Current lifetime: %1$h", getLifeInTag(itemStack)))
            list.add(I18N.tr("Condition: %1$", getLampCondition(itemStack)))
        }
    }

    private fun getLampCondition(itemStack: ItemStack): String {
        return if (!itemStack.hasTagCompound() || !itemStack.tagCompound.hasKey("life")) {
            I18N.tr("New")
        } else {
            val lampLife = getLifeInTag(itemStack)

            if (lampLife == lampData.technology.nominalLifeInHours) I18N.tr("New")
            else if (lampLife > (0.5 * lampData.technology.nominalLifeInHours)) I18N.tr("Good")
            else if (lampLife > (0.15 * lampData.technology.nominalLifeInHours)) I18N.tr("Used")
            else if (lampLife > (0.01 * lampData.technology.nominalLifeInHours)) I18N.tr("Bad")
            else I18N.tr("End of life")
        }
    }

}