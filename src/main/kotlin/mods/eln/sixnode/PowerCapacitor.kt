package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.IGuiObject
import mods.eln.gui.ISlotSkin
import mods.eln.gui.ItemStackFilter
import mods.eln.gui.SlotFilter
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.tr
import mods.eln.item.DielectricItem
import mods.eln.item.IConfigurable
import mods.eln.item.ItemMovingHelper
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Capacitor
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.wiki.Data
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.util.HashMap
import kotlin.math.abs
import kotlin.math.pow

class PowerCapacitorSixDescriptor(name: String,
                                  obj: Obj3D,
                                  var serie: IFunction,
                                  var dischargeTao: Double) : SixNodeDescriptor(name, PowerCapacitorSixElement::class.java, PowerCapacitorSixRender::class.java) {
    private var CapacitorCore: Obj3D.Obj3DPart? = null
    private var CapacitorCables: Obj3D.Obj3DPart? = null
    private var Base: Obj3D.Obj3DPart? = null
    fun getCValue(cableCount: Int, nominalDielVoltage: Double): Double {
        if (cableCount == 0) return 1e-6
        val uTemp = nominalDielVoltage / Eln.LVU
        return serie.getValue((cableCount - 1) / uTemp / uTemp)
    }

    fun getCValue(inventory: IInventory): Double {
        val core = inventory.getStackInSlot(PowerCapacitorSixContainer.redId)
        val diel = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId)
        return if (core == null || diel == null) getCValue(0, 0.0) else {
            getCValue(core.stackSize, getUNominalValue(inventory))
        }
    }

    fun getUNominalValue(inventory: IInventory): Double {
        val diel = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId)
        return if (diel == null) 10000.0 else {
            val desc = GenericItemUsingDamageDescriptor.getDescriptor(diel) as DielectricItem
            desc.uNominal * diel.stackSize
        }
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    fun draw() {
        if (null != Base) Base!!.draw()
        if (null != CapacitorCables) CapacitorCables!!.draw()
        if (null != CapacitorCore) CapacitorCore!!.draw()
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type != IItemRenderer.ItemRenderType.INVENTORY) {
            GL11.glTranslatef(0.0f, 0.0f, -0.2f)
            GL11.glScalef(1.25f, 1.25f, 1.25f)
            GL11.glRotatef(-90f, 0f, 1f, 0f)
            draw()
        } else {
            super.renderItem(type, item, *data)
        }
    }

    override fun addInformation(
        itemStack: ItemStack?,
        entityPlayer: EntityPlayer?,
        list: MutableList<String>?,
        par4: Boolean
    ) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list?.add(tr("Provides capacitance. Use with dielectrics and redstone"))
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)
        list?.add(tr("It doesn't really behave well for DC"))
        return RealisticEnum.UNREALISTIC
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player)!!.left()
    }

    init {
        CapacitorCables = obj.getPart("CapacitorCables")
        CapacitorCore = obj.getPart("CapacitorCore")
        Base = obj.getPart("Base")
        voltageLevelColor = VoltageLevelColor.Neutral
    }
}

class PowerCapacitorSixElement(SixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(SixNode, side, descriptor), IConfigurable {
    var descriptor: PowerCapacitorSixDescriptor = descriptor as PowerCapacitorSixDescriptor
    override var inventory = SixNodeElementInventory(2, 64, this)
    var positiveLoad = NbtElectricalLoad("positiveLoad")
    var negativeLoad = NbtElectricalLoad("negativeLoad")
    var capacitor = Capacitor(positiveLoad, negativeLoad)
    var dischargeResistor = Resistor(positiveLoad, negativeLoad)
    var punkProcess: PunkProcess = PunkProcess()
    var voltageWatchdog = BipoleVoltageWatchdog(capacitor).setNominalVoltage(this.descriptor.getUNominalValue(this.inventory)).setDestroys(WorldExplosion(this).cableExplosion())
    var stdDischargeResistor = 0.0
    var fromNbt = false

    inner class PunkProcess : IProcess {
        var eLeft = 0.0
        var eLegaliseResistor = 0.0
        override fun process(time: Double) {
            if (eLeft <= 0) {
                eLeft = 0.0
                dischargeResistor.resistance = stdDischargeResistor
            } else {
                eLeft -= dischargeResistor.power * time
                dischargeResistor.resistance = eLegaliseResistor
            }
        }
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        if (lrdu == front.right()) return positiveLoad
        return if (lrdu == front.left()) negativeLoad else null
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        if (lrdu == front.right()) return NodeBase.maskElectricalPower
        return if (lrdu == front.left()) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(): String {
        return Utils.plotVolt("U", abs(capacitor.voltage)) + Utils.plotAmpere("I", capacitor.current)
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Capacity")] = Utils.plotValue(capacitor.coulombs, "F")
        info[tr("Charge")] = Utils.plotEnergy("", capacitor.energy)
        if (Eln.wailaEasyMode) {
            info[tr("Voltage drop")] = Utils.plotVolt("", Math.abs(capacitor.voltage))
            info[tr("Current")] = Utils.plotAmpere("", Math.abs(capacitor.current))
        }
        return info
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun initialize() {
        Eln.applySmallRs(positiveLoad)
        Eln.applySmallRs(negativeLoad)
        setupPhysical()
    }

    public override fun inventoryChanged() {
        super.inventoryChanged()
        setupPhysical()
    }

    fun setupPhysical() {
        val eOld = capacitor.energy
        capacitor.coulombs = descriptor.getCValue(inventory)
        stdDischargeResistor = descriptor.dischargeTao / capacitor.coulombs
        punkProcess.eLegaliseResistor = descriptor.getUNominalValue(inventory).pow(2.0) / 400
        if (fromNbt) {
            dischargeResistor.resistance = stdDischargeResistor
            fromNbt = false
        } else {
            val deltaE = capacitor.energy - eOld
            punkProcess.eLeft += deltaE
            if (deltaE < 0) {
                dischargeResistor.resistance = stdDischargeResistor
            } else {
                dischargeResistor.resistance = punkProcess.eLegaliseResistor
            }
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setDouble("punkELeft", punkProcess.eLeft)
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        punkProcess.eLeft = nbt.getDouble("punkELeft")
        if (java.lang.Double.isNaN(punkProcess.eLeft)) punkProcess.eLeft = 0.0
        fromNbt = true
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return PowerCapacitorSixContainer(player, inventory)
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("capRedstoneAmt")) {
            val desired = compound.getInteger("capRedstoneAmt")
            object : ItemMovingHelper() {
                override fun acceptsStack(stack: ItemStack): Boolean {
                    return stack.item === Items.redstone
                }

                override fun newStackOfSize(size: Int): ItemStack {
                    return ItemStack(Items.redstone, size)
                }
            }.move(invoker.inventory, inventory, PowerCapacitorSixContainer.redId, desired)
            reconnect()
        }
        if (compound.hasKey("capDielectricAmt")) {
            val desired = compound.getInteger("capDielectricAmt")
            val dielectric = GenericItemUsingDamageDescriptor.getByName("Dielectric")
            object : ItemMovingHelper() {
                override fun acceptsStack(stack: ItemStack): Boolean {
                    return dielectric!!.checkSameItemStack(stack)
                }

                override fun newStackOfSize(items: Int): ItemStack {
                    return dielectric!!.newItemStack(items)
                }
            }.move(invoker.inventory, inventory, PowerCapacitorSixContainer.dielectricId, desired)
            reconnect()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        var stack = inventory.getStackInSlot(PowerCapacitorSixContainer.redId)
        if (stack == null) {
            compound.setInteger("capRedstoneAmt", 0)
        } else {
            compound.setInteger("capRedstoneAmt", stack.stackSize)
        }
        stack = inventory.getStackInSlot(PowerCapacitorSixContainer.dielectricId)
        if (stack == null) {
            compound.setInteger("capDielectricAmt", 0)
        } else {
            compound.setInteger("capDielectricAmt", stack.stackSize)
        }
    }

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalComponentList.add(capacitor)
        electricalComponentList.add(dischargeResistor)
        electricalProcessList.add(punkProcess)
        slowProcessList.add(voltageWatchdog)
        positiveLoad.setAsMustBeFarFromInterSystem()
    }
}

class PowerCapacitorSixRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {
    var descriptor: PowerCapacitorSixDescriptor = descriptor as PowerCapacitorSixDescriptor
    override var inventory = SixNodeElementInventory(2, 64, this)

    override fun draw() {
        GL11.glRotatef(90f, 1f, 0f, 0f)
        front!!.glRotateOnX()
        descriptor.draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return PowerCapacitorSixGui(player, inventory, this)
    }
}

class PowerCapacitorSixGui(player: EntityPlayer, inventory: IInventory, var render: PowerCapacitorSixRender) : GuiContainerEln(PowerCapacitorSixContainer(player, inventory)) {

    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        helper.drawString(8, 8, -0x1000000, tr("Capacity: %1\$F", Utils.plotValue(render.descriptor.getCValue(render.inventory))))
        helper.drawString(8, 8 + 8 + 1, -0x1000000, tr("Nominal voltage: %1\$V", Utils.plotValue(render.descriptor.getUNominalValue(render.inventory))))
        super.postDraw(f, x, y)
    }

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54)
    }
}

class PowerCapacitorSixContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf(
    SlotFilter(inventory, redId, 132, 8, 13, arrayOf(ItemStackFilter(Items.redstone)),
        ISlotSkin.SlotSkin.medium, arrayOf(tr("Redstone slot"), tr("(Increases capacity)"))),
    GenericItemUsingDamageSlot(inventory, dielectricId, 132 + 20, 8, 20, DielectricItem::class.java,
        ISlotSkin.SlotSkin.medium, arrayOf(tr("Dielectric slot"), tr("(Increases maximum voltage)")))
)) {
    companion object {
        const val redId = 0
        const val dielectricId = 1
    }
}
