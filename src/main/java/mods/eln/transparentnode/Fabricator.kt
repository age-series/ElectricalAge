package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkin
import mods.eln.misc.BasicContainer
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.INodeContainer
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import kotlin.math.max

class FabricatorDescriptor(
    name: String
): TransparentNodeDescriptor(name, FabricatorElement::class.java, FabricatorRender::class.java) {

    val fabricatorModel = Eln.obj.getObj("fabricator")
    val body = fabricatorModel.getPart("Cube.002_Cube.013")
    val etcherX = fabricatorModel.getPart("Cube.003_Cube.012")
    val etcherZ = fabricatorModel.getPart("Cube.001_Cube.010")

    fun draw() {
        GL11.glTranslated(-0.5, -0.5, 0.5)
        body.draw()
        etcherX.draw()
        etcherZ.draw()
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType?, item: ItemStack?, vararg data: Any?) {
        draw()
    }
}

class FabricatorElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    var operation: FabricatorOperation? = FabricatorOperation.TRANSISTOR

    val electricalLoad = NbtElectricalLoad("load")
    val resistorLoad = Resistor(electricalLoad, null)

    val craftingProcess = FabricatorProcess(this)

    val inventory: TransparentNodeElementInventory = FabricatorInventory(FabricatorSlots.values().size, 64, this)

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad {
        return electricalLoad
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? = null

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        return NodeBase.maskElectricalPower
    }

    override fun multiMeterString(side: Direction?) = ""

    override fun thermoMeterString(side: Direction?) = ""

    override fun initialize() {
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(resistorLoad)
        electricalLoad.rs = Eln.getSmallRs()
        resistorLoad.r = MnaConst.highImpedance
        slowProcessList.add(craftingProcess)
        connect()
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun hasGui() = true

    override fun getInventory(): IInventory {
        return inventory
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return FabricatorContainer(this.node, player, inventory, descriptor as FabricatorDescriptor)
    }
}

enum class FabricatorOperation(val opName: String, val outputItem: ItemStack?, val powerRequired: Double, val yieldPercentage: Double) {
    TRANSISTOR("Transistor", Eln.transistor.newItemStack(1), 2_000.0, 1.0),
    D_FLIP_FLOP("D Flip Flop", null, 4_000.0, 1.0),
    JK_FLIP_FLOP("JK Flip Flop", null, 4_000.0, 1.0),
    SR_FLIP_FLOP("SR Flip Flop", null, 4_000.0, 1.0),
    ALU("8 Bit ALU", Eln.alu.newItemStack(1), 10_000.0, 0.5)
}

class FabricatorProcess(val element: FabricatorElement): IProcess {

    var powerConsumed = 0.0
    val powerSink = 2_000.0

    override fun process(time: Double) {
        val operation = element.operation

        val outputSlot = element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId)
        val siliconWaferSlot = element.inventory.getStackInSlot(FabricatorSlots.SILICON_WAFER.slotId)
        val plateCopperSlot = element.inventory.getStackInSlot(FabricatorSlots.COPPER_PLATE.slotId)

        val silicon_wafer_name = "Silicon_Wafer"
        val copper_plate_name = "Copper_Plate"

        val canOutput = if (outputSlot != null) {
            Utils.canPutStackInInventory(
                listOf(outputSlot).toTypedArray(),
                element.inventory,
                listOf(FabricatorSlots.OUTPUT.slotId).toIntArray()
            )
        } else {
            true
        }

        val hasInputs = (
            siliconWaferSlot != null &&
            plateCopperSlot != null &&
            siliconWaferSlot.unlocalizedName == silicon_wafer_name &&
            plateCopperSlot.unlocalizedName == copper_plate_name
        )

        if (canOutput && hasInputs && operation != null) {
            val power = time * element.resistorLoad.p
            powerConsumed += power
            if (powerConsumed > operation.powerRequired * 2) {
                element.resistorLoad.r = MnaConst.highImpedance
            } else {
                val resistance = max(element.electricalLoad.u * element.electricalLoad.u / powerSink, Eln.getSmallRs())
                element.resistorLoad.r = resistance
            }
        } else {
            element.resistorLoad.r = MnaConst.highImpedance
        }

        if (operation?.outputItem != null && operation.powerRequired <= powerConsumed) {
            // Operation completed. Results!

            element.inventory.decrStackSize(FabricatorSlots.COPPER_PLATE.slotId, 1)
            element.inventory.decrStackSize(FabricatorSlots.SILICON_WAFER.slotId, 1)
            if (Math.random() <= operation.yieldPercentage) {
                if (element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId) == null) {
                    val newStack = operation.outputItem.copy()
                    newStack.stackSize = 1
                    element.inventory.setInventorySlotContents(FabricatorSlots.OUTPUT.slotId, newStack)
                    powerConsumed -= operation.powerRequired
                    element.needPublish()
                } else {
                    val stackSize = element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId).stackSize
                    if (stackSize in 0..63) {
                        val newStack = operation.outputItem.copy()
                        newStack.stackSize = stackSize + 1
                        element.inventory.setInventorySlotContents(FabricatorSlots.OUTPUT.slotId, newStack)
                        powerConsumed -= operation.powerRequired
                        element.needPublish()
                    }
                    // Abort if not in 0 to 63, don't consume power, just sit and wait
                }
            }
        }
    }
}

class FabricatorRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, descriptor) {

    val inventory = FabricatorInventory(3, 64, this)

    init {
        this.transparentNodedescriptor = descriptor as FabricatorDescriptor
    }

    override fun draw() {
        (this.transparentNodedescriptor as FabricatorDescriptor).draw()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return FabricatorGui(player, inventory, this)
    }
}

class FabricatorGui(player: EntityPlayer, inventory: IInventory, render: FabricatorRender): GuiContainerEln(FabricatorContainer(null, player, inventory, render.transparentNodedescriptor as FabricatorDescriptor)) {
    override fun newHelper() = GuiHelperContainer(this, 176, 164, 8, 80)
}

enum class FabricatorSlots(val slotId: Int) {
    OUTPUT(0),
    SILICON_WAFER(1),
    COPPER_PLATE(2)
}

class FabricatorContainer(
    node: NodeBase?,
    player: EntityPlayer,
    inventory: IInventory,
    descriptor: FabricatorDescriptor
): BasicContainer(player, inventory, getSlot(inventory, descriptor)), INodeContainer {

    val nb = node

    override fun getNode(): NodeBase? {
        return nb
    }

    override fun getRefreshRateDivider(): Int {
        return 1
    }

    companion object {
        private fun getSlot(inventory: IInventory, descriptor: FabricatorDescriptor): Array<Slot> {
            return FabricatorSlots.values().mapIndexed { index, _ ->
                when (index) {
                    FabricatorSlots.OUTPUT.slotId -> {
                        SlotWithSkin(inventory, index, 8 + 16, 12 + 32, SlotSkin.medium)
                    }
                    FabricatorSlots.COPPER_PLATE.slotId -> {
                        SlotWithSkin(inventory, index, 8, 12, SlotSkin.medium)
                    }
                    FabricatorSlots.SILICON_WAFER.slotId -> {
                        SlotWithSkin(inventory, index, 8 + 32, 12, SlotSkin.medium)
                    }
                    else -> null
                }
            }.filterNotNull().toTypedArray()
        }
    }
}

class FabricatorInventory: TransparentNodeElementInventory {

    var element: FabricatorElement? = null

    constructor(size: Int, stackLimit: Int, element: FabricatorElement) : super(size, stackLimit, element) {
        this.element = element
    }

    constructor(size: Int, stackLimit: Int, render: TransparentNodeElementRender): super(size, stackLimit, render)

    override fun getAccessibleSlotsFromSide(side: Int): IntArray {
        return FabricatorSlots.values().map{it.slotId}.toIntArray()
    }

    override fun canInsertItem(slot: Int, item: ItemStack?, side: Int): Boolean {
        if (item == null) return false
        val itemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(item) ?: return false
        if (itemDescriptor === Eln.siliconWafer && slot == FabricatorSlots.SILICON_WAFER.slotId) return true
        if (itemDescriptor === Eln.plateCopper && slot == FabricatorSlots.COPPER_PLATE.slotId) return true
        return false
    }

    override fun canExtractItem(slot: Int, item: ItemStack?, side: Int): Boolean {
        return slot == FabricatorSlots.OUTPUT.slotId
    }
}
