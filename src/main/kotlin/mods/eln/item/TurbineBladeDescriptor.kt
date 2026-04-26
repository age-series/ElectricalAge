package mods.eln.item

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamage
import mods.eln.i18n.I18N.tr
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

// Config data for one blade tier, mirrors BoilerplateLampData in LampTechnology.kt.
data class BladeConfigData(
    val tierName: String,
    var nominalLifeInHours: Double,
    var infiniteLifeEnabled: Boolean = false
) {
    private val nominalLifePath: String
        get() = "items.turbineBlades.${tierName}.nominalLifeHours"

    private val infiniteLifePath: String
        get() = "items.turbineBlades.${tierName}.infiniteLifeEnabled"

    fun loadConfig() {
        val configLife = Eln.config.getDoubleOrElse(nominalLifePath, nominalLifeInHours)

        try {
            require(configLife > 0)
        } catch (_: IllegalArgumentException) {
            println("ELN config: Nominal blade life of type $tierName must be greater than 0! Changes not applied!")
        }

        if (configLife > 0) {
            nominalLifeInHours = configLife
        }

        infiniteLifeEnabled = Eln.config.getBooleanOrElse(infiniteLifePath, infiniteLifeEnabled)
    }

    fun updateNominalLifeConfig(newNominalLife: Double) {
        nominalLifeInHours = newNominalLife
        Eln.config.setDouble(nominalLifePath, newNominalLife)
        Eln.config.save()
    }

    fun updateInfiniteLifeConfig(enabled: Boolean) {
        infiniteLifeEnabled = enabled
        Eln.config.setBoolean(infiniteLifePath, enabled)
        Eln.config.save()
    }
}

// Pre-populated at object init time so blade config defaults exist before item registration runs.
// Mirrors LampLists in LampTechnology.kt.
object TurbineBladeLists {
    val bladeConfigList = mutableListOf<BladeConfigData>()
    val registeredBlades = mutableListOf<TurbineBladeDescriptor>()
    val bladeTypesList = mutableListOf<String>()
    var bladeTypesString: String = ""

    init {
        // Nominal life at zero fuel-stress conditions. Real service intervals informed ratios:
        // cast iron ~1k–4k h, steel ~8× better, nickel superalloy ~6× over steel, W-Re ~5× over superalloy.
        // Scaled down ~300× from real-world so iron feels like an early-game consumable.
        bladeConfigList.add(BladeConfigData("iron", 10.0))       // cast iron. early-game throwaway, ~7h on steam
        bladeConfigList.add(BladeConfigData("steel", 64.0))      // low-alloy steel. ~6.4× iron, ~46h on steam
        bladeConfigList.add(BladeConfigData("alloy", 400.0))     // nickel superalloy. ~6.25× steel, ~348h on steam
        bladeConfigList.add(BladeConfigData("tungsten", 2000.0)) // W-Re alloy. ~5× alloy, ~1840h on steam

        for (config in bladeConfigList) bladeTypesList.add(config.tierName)
        bladeTypesString = bladeTypesList.joinToString("/") + "/all"
    }

    fun getConfigData(tierName: String): BladeConfigData? = bladeConfigList.find { it.tierName == tierName }
}

// Consumable blade item. Turbines need one installed to run, condition degrades based on fuel temperature and corrosiveness.
class TurbineBladeDescriptor(
    name: String,
    val tierName: String,
    val temperatureResistance: Double,
    val corrosionResistance: Double,
    val tierDescription: String
) : GenericItemUsingDamageDescriptorUpgrade(name) {

    val configData: BladeConfigData = TurbineBladeLists.getConfigData(tierName)
        ?: error("No BladeConfigData for tier '$tierName' — add it to TurbineBladeLists.init{}")

    val nominalLifeInHours: Double get() = configData.nominalLifeInHours
    val infiniteLifeEnabled: Boolean get() = configData.infiniteLifeEnabled

    init {
        TurbineBladeLists.registeredBlades.add(this)
    }

    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addUpgrade(newItemStack())
    }

    // Condition is 0.0 (broken) to 1.0 (new). Stored in NBT so it survives inventory moves.
    override fun getDefaultNBT(): NBTTagCompound {
        val nbt = NBTTagCompound()
        nbt.setDouble("condition", 1.0)
        return nbt
    }

    fun getCondition(stack: ItemStack): Double {
        val tag = stack.tagCompound ?: return 1.0
        return if (tag.hasKey("condition")) tag.getDouble("condition").coerceIn(0.0, 1.0) else 1.0
    }

    fun setCondition(stack: ItemStack, condition: Double) {
        if (stack.tagCompound == null) stack.tagCompound = NBTTagCompound()
        stack.tagCompound.setDouble("condition", condition.coerceIn(0.0, 1.0))
    }

    private fun getConditionLabel(stack: ItemStack): String {
        if (!stack.hasTagCompound() || !stack.tagCompound.hasKey("condition")) return tr("New")
        val c = getCondition(stack)
        return when {
            c >= 1.0  -> tr("New")
            c > 0.5   -> tr("Good")
            c > 0.15  -> tr("Used")
            c > 0.01  -> tr("Bad")
            else      -> tr("End of life")
        }
    }

    override fun addInformation(
        stack: ItemStack?,
        player: EntityPlayer?,
        list: MutableList<String>,
        par4: Boolean
    ) {
        super.addInformation(stack, player, list, par4)
        list.add(tr($$"Nominal Lifetime: %1$h",nominalLifeInHours.toInt()))
        if (stack != null) list.add(tr("Condition: %1$", getConditionLabel(stack)))
        if (tierDescription.isNotEmpty()) list.add(tr(tierDescription))
    }

    companion object {
        fun getDescriptor(stack: ItemStack?): TurbineBladeDescriptor? {
            if (stack == null) return null
            val item = stack.item as? GenericItemUsingDamage<*> ?: return null
            return item.getDescriptor(stack) as? TurbineBladeDescriptor
        }
    }
}
