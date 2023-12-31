package mods.eln.item.electricalitem

import mods.eln.generic.genericArmorItem
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.wiki.Data
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.DamageSource
import net.minecraftforge.common.ISpecialArmor
import net.minecraftforge.common.ISpecialArmor.ArmorProperties

class ElectricalArmor(
    par2EnumArmorMaterial: ArmorMaterial?,
    par3: Int,
    type: ArmourType?,
    t1: String?,
    t2: String?,  //String icon,
    var energyStorage: Double,
    var chargePower: Double,
    var ratioMax: Double,
    var ratioMaxEnergy: Double,
    var energyPerDamage: Double
    ) : genericArmorItem(par2EnumArmorMaterial, par3, type, t1, t2), IItemEnergyBattery, ISpecialArmor {


    override fun getProperties(player: EntityLivingBase, armor: ItemStack, source: DamageSource, damage: Double, slot: Int): ArmorProperties {
        return ArmorProperties(100, Math.min(1.0, getEnergy(armor) / ratioMaxEnergy) * ratioMax, (getEnergy(armor) / energyPerDamage * 25.0).toInt())
    }

    override fun getArmorDisplay(player: EntityPlayer, armor: ItemStack, slot: Int): Int {
        return (Math.min(1.0, getEnergy(armor) / ratioMaxEnergy) * ratioMax * 20).toInt()
    }

    override fun damageArmor(entity: EntityLivingBase, stack: ItemStack, source: DamageSource, damage: Int, slot: Int) {
        var e = getEnergy(stack)
        e = Math.max(0.0, e - damage * energyPerDamage)
        setEnergy(stack, e)
        Utils.println("armor hit  damage=" + damage + " energy=" + e + " energyLost=" + damage * energyPerDamage)
    }

    override fun getIsRepairable(par1ItemStack: ItemStack, par2ItemStack: ItemStack): Boolean {
        return false
    }

    override fun hasColor(par1ItemStack: ItemStack): Boolean {
        return false
    }

    val defaultNBT: NBTTagCompound
        get() {
            val nbt = NBTTagCompound()
            nbt.setDouble("energy", 0.0)
            nbt.setBoolean("powerOn", false)
            nbt.setInteger("rand", (Math.random() * 0xFFFFFFF).toInt())
            return nbt
        }

    protected fun getNbt(stack: ItemStack): NBTTagCompound? {
        var nbt = stack.tagCompound
        if (nbt == null) {
            stack.tagCompound = defaultNBT.also { nbt = it }
        }
        return nbt
    }

    fun getPowerOn(stack: ItemStack): Boolean {
        return getNbt(stack)!!.getBoolean("powerOn")
    }

    fun setPowerOn(stack: ItemStack, value: Boolean) {
        getNbt(stack)!!.setBoolean("powerOn", value)
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer?, list: MutableList<Any?>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Charge power: %1\$W", chargePower.toInt()))
        list.add(tr("Stored energy: %1\$J (%2$%)", getEnergy(itemStack),
            (getEnergy(itemStack) / energyStorage * 100).toInt()))
        //list.add("Power button is " + (getPowerOn(itemStack) ? "ON" : "OFF"));
    }

    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack)!!.getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack)!!.setDouble("energy", value)
    }

    override fun getEnergyMax(stack: ItemStack): Double {
        return energyStorage
    }

    override fun getChargePower(stack: ItemStack): Double {
        return chargePower
    }

    override fun getDischagePower(stack: ItemStack): Double {
        return 0.0
    }

    override fun getPriority(stack: ItemStack): Int {
        return 0
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}

    override fun getItemEnchantability(): Int {
        return 0;
    }

    init {
        //rIcon = new ResourceLocation("eln", icon);
        Data.addPortable(ItemStack(this))
    }
}
