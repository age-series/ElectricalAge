package mods.eln.sixnode

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.IGuiObject
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.CopperCableDescriptor
import mods.eln.item.FerromagneticCoreDescriptor
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
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Inductor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.wiki.Data
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.util.HashMap
import kotlin.math.abs

class PowerInductorSixDescriptor(name: String,
                                 obj: Obj3D,
                                 var serie: IFunction) : SixNodeDescriptor(name, PowerInductorSixElement::class.java, PowerInductorSixRender::class.java) {
    var InductorBaseExtention: Obj3D.Obj3DPart? = null
    var InductorCables: Obj3D.Obj3DPart? = null
    var InductorCore: Obj3D.Obj3DPart? = null
    var Base: Obj3D.Obj3DPart? = null
    fun getlValue(cableCount: Int): Double {
        return if (cableCount == 0) 0.0 else serie.getValue((cableCount - 1).toDouble())
    }

    fun getlValue(inventory: IInventory): Double {
        val core = inventory.getStackInSlot(PowerInductorSixContainer.cableId)
        return if (core == null) getlValue(0) else getlValue(core.stackSize)
    }

    fun getRsValue(inventory: IInventory): Double {
        val core = inventory.getStackInSlot(PowerInductorSixContainer.coreId) ?: return MnaConst.highImpedance
        val coreDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(core) as FerromagneticCoreDescriptor
        val coreFactor = coreDescriptor.cableMultiplicator
        return Eln.instance.lowVoltageCableDescriptor.electricalRs * coreFactor
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addEnergy(newItemStack())
    }

    fun draw() {
        if (null != Base) Base!!.draw()
        if (null != InductorBaseExtention) InductorBaseExtention!!.draw()
        if (null != InductorCables) InductorCables!!.draw()
        if (null != InductorCore) InductorCore!!.draw()
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
        list?.add(tr("Provides inductance. Use with iron cores and bare copper cables"))
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        super.addRealismContext(list)
        list?.add(tr("It doesn't really behave well for DC"))
        list?.add(tr("* Missing an inductive voltage spike on field collapse"))
        return RealisticEnum.UNREALISTIC
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player)!!.left()
    }

    init {
        InductorBaseExtention = obj.getPart("InductorBaseExtention")
        InductorCables = obj.getPart("InductorCables")
        InductorCore = obj.getPart("InductorCore")
        Base = obj.getPart("Base")
        voltageLevelColor = VoltageLevelColor.Neutral
    }
}


class PowerInductorSixElement(SixNode: SixNode, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElement(SixNode, side, descriptor), IConfigurable {
    var descriptor: PowerInductorSixDescriptor = descriptor as PowerInductorSixDescriptor
    var positiveLoad = NbtElectricalLoad("positiveLoad")
    var negativeLoad = NbtElectricalLoad("negativeLoad")
    var inductor = Inductor("inductor", positiveLoad, negativeLoad)
    var fromNbt = false
    override var inventory = SixNodeElementInventory(2, 64, this)

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
        return Utils.plotVolt("U", abs(inductor.voltage)) + Utils.plotAmpere("I", inductor.current)
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Inductance")] = Utils.plotValue(inductor.inductance, "H")
        info[tr("Charge")] = Utils.plotEnergy("", inductor.energy)
        if (Eln.wailaEasyMode) {
            info[tr("Voltage drop")] = Utils.plotVolt("", abs(inductor.voltage))
            info[tr("Current")] = Utils.plotAmpere("", abs(inductor.current))
        }
        return info
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun initialize() {
        setupPhysical()
    }

    public override fun inventoryChanged() {
        super.inventoryChanged()
        setupPhysical()
    }

    fun setupPhysical() {
        val rs = descriptor.getRsValue(inventory)
        inductor.inductance = descriptor.getlValue(inventory)
        positiveLoad.serialResistance = rs
        negativeLoad.serialResistance = rs
        if (fromNbt) {
            fromNbt = false
        } else {
            inductor.resetStates()
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        fromNbt = true
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return PowerInductorSixContainer(player, inventory)
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("indCableAmt")) {
            val desired = compound.getInteger("indCableAmt")
            object : ItemMovingHelper() {
                override fun acceptsStack(stack: ItemStack): Boolean {
                    return Eln.instance.copperCableDescriptor.checkSameItemStack(stack)
                }

                override fun newStackOfSize(items: Int): ItemStack {
                    return Eln.instance.copperCableDescriptor.newItemStack(items)
                }
            }.move(invoker.inventory, inventory, PowerInductorSixContainer.cableId, desired)
            reconnect()
        }
        if (compound.hasKey("indCore")) {
            val descName = compound.getString("indCore")
            if (descName === GenericItemUsingDamageDescriptor.INVALID_NAME) {
                val stack = inventory.getStackInSlot(PowerInductorSixContainer.coreId)
                val desc = GenericItemUsingDamageDescriptor.getDescriptor(stack)
                if (desc != null) {
                    object : ItemMovingHelper() {
                        override fun acceptsStack(stack: ItemStack): Boolean {
                            return desc === GenericItemUsingDamageDescriptor.getDescriptor(stack)
                        }

                        override fun newStackOfSize(items: Int): ItemStack {
                            return desc.newItemStack(items)
                        }
                    }.move(invoker.inventory, inventory, PowerInductorSixContainer.coreId, 0)
                }
            } else {
                val desc = GenericItemUsingDamageDescriptor.getByName(compound.getString("indCore"))
                object : ItemMovingHelper() {
                    override fun acceptsStack(stack: ItemStack): Boolean {
                        return GenericItemUsingDamageDescriptor.getDescriptor(stack) === desc
                    }

                    override fun newStackOfSize(items: Int): ItemStack {
                        return desc!!.newItemStack(items)
                    }
                }.move(invoker.inventory, inventory, PowerInductorSixContainer.coreId, 1)
            }
            reconnect()
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        var stack = inventory.getStackInSlot(PowerInductorSixContainer.cableId)
        if (stack == null) {
            compound.setInteger("indCableAmt", 0)
        } else {
            compound.setInteger("indCableAmt", stack.stackSize)
        }
        stack = inventory.getStackInSlot(PowerInductorSixContainer.coreId)
        val desc = GenericItemUsingDamageDescriptor.getDescriptor(stack)
        if (desc == null) {
            compound.setString("indCore", GenericItemUsingDamageDescriptor.INVALID_NAME)
        } else {
            compound.setString("indCore", desc.name)
        }
    }

    init {
        electricalLoadList.add(positiveLoad)
        electricalLoadList.add(negativeLoad)
        electricalComponentList.add(inductor)
        positiveLoad.setAsMustBeFarFromInterSystem()
    }
}


class PowerInductorSixRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) : SixNodeElementRender(tileEntity, side, descriptor) {
    @JvmField
    var descriptor: PowerInductorSixDescriptor = descriptor as PowerInductorSixDescriptor

    override var inventory = SixNodeElementInventory(2, 64, this)

    override fun draw() {
        front!!.left().glRotateOnX()
        descriptor.draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return PowerInductorSixGui(player, inventory, this)
    }

}


class PowerInductorSixGui(player: EntityPlayer, inventory: IInventory, var render: PowerInductorSixRender) : GuiContainerEln(PowerInductorSixContainer(player, inventory)) {
    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        helper.drawString(8, 12, -0x1000000, tr("Inductance: %1\$H", Utils.plotValue(render.descriptor.getlValue(render.inventory))))
        super.postDraw(f, x, y)
    }

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 166 - 54, 8, 84 - 54)
    }
}


class PowerInductorSixContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory, arrayOf<Slot>(
    GenericItemUsingDamageSlot(inventory, cableId, 132, 8, 19, CopperCableDescriptor::class.java,
        ISlotSkin.SlotSkin.medium, arrayOf(tr("Copper cable slot"), tr("(Increases inductance)"))),
    GenericItemUsingDamageSlot(inventory, coreId, 132 + 20, 8, 1, FerromagneticCoreDescriptor::class.java,
        ISlotSkin.SlotSkin.medium, arrayOf(tr("Ferromagnetic core slot")))
)) {
    companion object {
        const val cableId = 0
        const val coreId = 1
    }
}
