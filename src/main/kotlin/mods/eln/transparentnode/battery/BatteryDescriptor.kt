package mods.eln.transparentnode.battery

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.BatteryProcess
import mods.eln.sim.BatterySlowProcess
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.Simulator
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.wiki.Data
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper

class BatteryDescriptor(
    name: String,
    modelName: String,
    var startCharge: Double,
    var isRechargable: Boolean,
    var lifeEnable: Boolean,
    var UfCharge: FunctionTable,
    var electricalU: Double,
    var electricalPMax: Double,
    var electricalDischargeRate: Double,
    var electricalStdP: Double,
    var electricalStdDischargeTime: Double,
    var electricalStdEfficiency: Double,
    var electricalStdHalfLife: Double,
    var thermalHeatTime: Double,
    var thermalWarmLimit: Double,
    var thermalCoolLimit: Double
) : TransparentNodeDescriptor(name, BatteryElement::class.java, BatteryRender::class.java) {
    @JvmField
    var electricalStdEnergy: Double
    var electricalStdI: Double
    var electricalQ: Double
    @JvmField
    var electricalRs: Double
    @JvmField
    var electricalRp: Double
    var thermalC: Double
    var thermalRp: Double
    var thermalPMax: Double
    var lifeNominalCurrent: Double
    var lifeNominalLost: Double
    var modelName: String? = null
    var modelPart: Obj3DPart? = null
    @JvmField
    var IMax: Double
    var obj: Obj3D?
    var main: Obj3DPart? = null
    var plugPlus: Obj3DPart? = null
    var plusMinus: Obj3DPart? = null
    var battery: Obj3DPart? = null
    var renderType = 0
    private var renderSpec: String? = null
    @JvmField
    var currentDropVoltage = 1000000.0
    @JvmField
    var currentDropFactor = 0.0
    fun draw(plus: Boolean, minus: Boolean) {
        when (renderType) {
            0 -> {
                if (modelPart == null) return
                modelPart!!.draw()
            }
            1 -> {
                if (main != null) main!!.draw()
                if (plugPlus != null && plus) plugPlus!!.draw()
                if (plusMinus != null && minus) plusMinus!!.draw()
                if (battery != null) battery!!.draw()
            }
        }
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    fun applyTo(resistor: Resistor) {
        resistor.resistance = electricalRp
    }

    fun applyTo(battery: BatteryProcess) {
        battery.uNominal = electricalU
        battery.QNominal = electricalQ
        battery.voltageFunction = UfCharge
        battery.isRechargeable = isRechargable

        // Convert old battery absolute charge in Coulomb to to fraction of battery capacity if the capacity is
        // very small and the output voltage is more than a quarter of the nominal voltage.
        if (battery.Q > 1.5 && battery.u > battery.uNominal / 4) {
            battery.Q /= electricalQ
        }
    }

    fun applyTo(load: ElectricalLoad, @Suppress("UNUSED_PARAMETER") simulator: Simulator?) {
        load.serialResistance = electricalRs
    }

    fun applyTo(load: ThermalLoad) {
        load.Rp = thermalRp
        load.heatCapacity = thermalC
    }

    fun applyTo(process: BatterySlowProcess) {
        process.lifeNominalCurrent = lifeNominalCurrent
        process.lifeNominalLost = lifeNominalLost
    }

    override fun getDefaultNBT(): NBTTagCompound {
        val nbt = NBTTagCompound()
        nbt.setDouble("charge", startCharge)
        nbt.setDouble("life", 1.0)
        return nbt
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(Utils.plotVolt(tr("Nominal voltage: "), electricalU))
        list.add(Utils.plotPower(tr("Nominal power: "), electricalStdP))
        list.add(Utils.plotEnergy(tr("Energy capacity: "), electricalStdDischargeTime * electricalStdP))
        list.add(Utils.plotOhm(tr("Internal resistance: "), electricalRs * 2))
        list.add("")
        list.add(Utils.plotPercent(tr("Actual charge: "), getChargeInTag(itemStack)))
        if (lifeEnable) list.add(Utils.plotPercent(tr("Life: "), getLifeInTag(itemStack)))
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        list?.add(tr("Battery could be realistic in the future"))
        list?.add(tr("  * Batteries have internal resistance"))
        list?.add(tr("  * Not currently simulating any particular chemistry of battery"))
        list?.add(tr("Batteries are based in realistic battery designs that someone might implement realistically:"))
        list?.add(tr("  * Single use batteries emulate a voltaic pile"))
        list?.add(tr("  * Current oriented uses two smaller batteries in parallel to increase current capability"))
        list?.add(tr("  * Voltage oriented uses two smaller batteries in series to increase voltage"))
        return RealisticEnum.IDEAL
    }

    override fun getName(stack: ItemStack): String {
        return super.getName(stack) + Utils.plotPercent(tr(" charged at "), getChargeInTag(stack))
    }

    fun getChargeInTag(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) stack.tagCompound = defaultNBT
        return stack.tagCompound.getDouble("charge")
    }

    fun getLifeInTag(stack: ItemStack): Double {
        if (!stack.hasTagCompound()) stack.tagCompound = defaultNBT
        return stack.tagCompound.getDouble("life")
    }

    fun getEnergy(charge: Double, life: Double): Double {
        val stepNbr = 50
        val chargeStep = charge / stepNbr
        var chargeIntegrator = 0.0
        var energy = 0.0
        val QperStep = electricalQ * life * charge / stepNbr
        for (step in 0 until stepNbr) {
            val voltage = UfCharge.getValue(chargeIntegrator) * electricalU
            energy += voltage * QperStep
            chargeIntegrator += chargeStep
        }
        return energy
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return true
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw(true, true)
        }
    }

    override fun onEntityItemUpdate(entityItem: EntityItem): Boolean {
        if (entityItem.isBurning) {
            entityItem.worldObj.createExplosion(entityItem, entityItem.posX, entityItem.posY, entityItem.posZ, 2f, true)
            entityItem.extinguish()
            entityItem.setDead()
        }
        return false
    }

    fun setRenderSpec(renderSpec: String) {
        this.renderSpec = renderSpec
        if (obj != null) {
            when (renderType) {
                0, 1 -> {
                    main = obj!!.getPart("main")
                    plugPlus = obj!!.getPart("plugPlus")
                    plusMinus = obj!!.getPart("plugMinus")
                    battery = obj!!.getPart("battery_$renderSpec")
                }
            }
        }
    }

    fun setCurrentDrop(currentDropVoltage: Double, currentDropFactor: Double) {
        this.currentDropFactor = currentDropFactor
        this.currentDropVoltage = currentDropVoltage
    }

    init {
        electricalStdI = electricalStdP / electricalU
        electricalStdEnergy = electricalStdDischargeTime * electricalStdP
        electricalQ = electricalStdP * electricalStdDischargeTime / electricalU
        electricalQ = 1.0
        val energy = getEnergy(1.0, 1.0)
        electricalQ *= electricalStdEnergy / energy
        electricalRs = electricalStdP * (1 - electricalStdEfficiency) / electricalStdI / electricalStdI / 2
        electricalRp = Math.min(electricalU * electricalU / electricalStdP / electricalDischargeRate, 1000000000.0)
        lifeNominalCurrent = electricalStdP / electricalU
        lifeNominalLost = 0.5 / electricalStdHalfLife
        thermalPMax = electricalPMax / electricalU * electricalPMax / electricalU * electricalRs * 2
        thermalC = Math.pow(electricalPMax / electricalU, 2.0) * electricalRs * thermalHeatTime / thermalWarmLimit
        thermalRp = thermalWarmLimit / thermalPMax
        IMax = electricalStdI * 3
        obj = Eln.obj.getObj(modelName)
        if (obj != null) {
            if (obj!!.getString("type") == "A") renderType = 0
            if (obj!!.getString("type") == "B") renderType = 1
            when (renderType) {
                0 -> modelPart = obj!!.getPart("Battery")
                1 -> {
                }
            }
        }
        voltageLevelColor = VoltageLevelColor.fromVoltage(electricalU)
    }
}
