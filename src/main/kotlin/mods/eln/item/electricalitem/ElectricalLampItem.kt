package mods.eln.item.electricalitem

import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper

class ElectricalLampItem(name: String, var lightMin: Int, var rangeMin: Int, dischargeMin: Double, var lightMax: Int,
                         rangeMax: Int, dischargeMax: Double, energyStorage: Double, chargePower: Double) : LampItem(name), IItemEnergyBattery {
    var rangeMax: Int
    var energyStorage: Double
    var dischargeMin: Double
    var dischargeMax: Double
    var chargePower: Double
    var on: ResourceLocation
    var off: ResourceLocation
    var boosted: ResourceLocation
    override fun setParent(item: Item?, damage: Int) {
        super.setParent(item, damage)
        Data.addPortable(newItemStack())
        Data.addLight(newItemStack())
    }

    override fun getRange(stack: ItemStack): Int {
        return if (getLightState(stack) == 1) rangeMin else rangeMax
    }

    override fun getLight(stack: ItemStack): Int {
        val energy = getEnergy(stack)
        val state = getLightState(stack)
        var power = 0.0
        when (state) {
            1 -> power = dischargeMin
            2 -> power = dischargeMax
        }
        return if (energy > power) {
            //setEnergy(stack, energy - power);
            getLightLevel(stack)
        } else {
            //setEnergy(stack,0);
            0
        }
    }

    override fun getDefaultNBT(): NBTTagCompound? {
        val nbt = NBTTagCompound()
        nbt.setDouble("energy", 0.0)
        nbt.setBoolean("powerOn", false)
        nbt.setInteger("rand", (Math.random() * 0xFFFFFFF).toInt())
        return nbt
    }

    public override fun getLightState(stack: ItemStack): Int {
        return getNbt(stack).getInteger("LightState")
    }

    fun setLightState(stack: ItemStack?, value: Int) {
        getNbt(stack!!).setInteger("LightState", value)
    }

    fun getLightLevel(stack: ItemStack): Int {
        return if (getLightState(stack) == 1) lightMin else lightMax
    }

    override fun onItemRightClick(s: ItemStack, w: World, p: EntityPlayer): ItemStack {
        if (!w.isRemote && getEnergy(s) > 0) {
            var lightState = getLightState(s) + 1
            if (lightState > 2) lightState = 0
            when (lightState) {
                0 -> Utils.addChatMessage(p as EntityPlayerMP, "Flashlight OFF")
                1 -> Utils.addChatMessage(p as EntityPlayerMP, "Flashlight ON")
                2 -> Utils.addChatMessage(p as EntityPlayerMP, "Flashlight BOOSTED")
                else -> {
                }
            }
            setLightState(s, lightState)
        }
        return s
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Discharge power: %1\$W", Utils.plotValue(dischargeMin)))
        if (itemStack != null) {
            list.add(tr("Stored Energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
            list.add(tr("State:") + " " + if (getLightState(itemStack) != 0) tr("On") else tr("Off"))
        }
    }

    /*
	@Override
	public double putEnergy(ItemStack stack, double energy, double time) {
		double hit = Math.min(energy,Math.min(energyStorage - getEnergy(stack), chargePower * time));
		setEnergy(stack, getEnergy(stack) + hit);
		return energy - hit;
	}

	@Override
	public boolean isFull(ItemStack stack) {
		return getEnergy(stack) == energyStorage;
	}
*/
    override fun getEnergy(stack: ItemStack): Double {
        return getNbt(stack).getDouble("energy")
    }

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).setDouble("energy", value)
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

    override fun shouldUseRenderHelper(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return if (type == ItemRenderType.INVENTORY) false else true
    }

    override fun handleRenderType(item: ItemStack?, type: ItemRenderType?): Boolean {
        return true
    }

    override fun renderItem(type: ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        var drawlightstate = off
        when (getLightState(item!!)) {
            0 -> drawlightstate = off
            1 -> drawlightstate = on
            2 -> drawlightstate = boosted
        }
        UtilsClient.drawIcon(type!!, drawlightstate)
        //UtilsClient.drawIcon(type, (getLight(item) != 0 && getLightState(item) != 0 ? on : off));
        if (type == ItemRenderType.INVENTORY) {
            UtilsClient.drawEnergyBare(type, (getEnergy(item) / getEnergyMax(item)).toFloat())
        }
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {
        val energy = getEnergy(stack)
        val state = getLightState(stack)
        var power = 0.0
        when (state) {
            1 -> power = dischargeMin * time
            2 -> power = dischargeMax * time
        }
        if (energy > power) {
			setEnergy(stack, energy - power)
		} else {
			setEnergy(stack, 0.0)
			setLightState(stack, 0)
		}
    }

    init {
        this.rangeMax = rangeMax + 1 //adding 1 is a hack. Since the value is locked at 1 anyway, I would rather not change a ton of code to make this work, and just double its range by adding 1 here
        this.chargePower = chargePower
        this.dischargeMin = dischargeMin
        this.dischargeMax = dischargeMax
        this.energyStorage = energyStorage
        setDefaultIcon(name + "off")
        boosted = ResourceLocation("eln", "textures/items/" + name.replace(" ", "").lowercase() + "boosted.png")
        on = ResourceLocation("eln", "textures/items/" + name.replace(" ", "").lowercase() + "on.png")
        off = ResourceLocation("eln", "textures/items/" + name.replace(" ", "").lowercase() + "off.png")
        //	off = new ResourceLocation("eln", "/model/StoneFurnace/all.png");
    }
}
