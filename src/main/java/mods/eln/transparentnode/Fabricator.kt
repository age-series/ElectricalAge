package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.IGuiObject
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkin
import mods.eln.misc.BasicContainer
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
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
import mods.eln.transparentnode.turret.TurretElement
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException


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

    override fun handleRenderType(item: ItemStack?, type: IItemRenderer.ItemRenderType?): Boolean {
        return true
    }

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

    override fun networkSerialize(stream: DataOutputStream?) {
        super.networkSerialize(stream)
        stream?.writeInt(operation?.nid ?: -1)
    }

    override fun networkUnserialize(stream: DataInputStream?): Byte {
        val packetType = super.networkUnserialize(stream)
        if (stream == null) return unserializeNulldId
        try {
            when (packetType) {
                FabricatorNetwork.BUTTON_CLICK.id -> {
                    val thing = FabricatorOperation.values().filter { stream.readInt() == it.nid }
                    if (thing.isNotEmpty()) {
                        operation = thing.first()
                        needPublish()
                        Utils.println("Function is now ${operation?.opName}")
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return unserializeNulldId
    }
}

enum class FabricatorNetwork(val id: Byte) {
    BUTTON_CLICK(0)
}

enum class FabricatorOperation(val nid: Int, val opName: String, val outputItem: ItemStack, val powerRequired: Double, val yieldPercentage: Double) {
    TRANSISTOR(0, "Transistor", Eln.transistor.newItemStack(1), 2_000.0, 1.0),
    D_FLIP_FLOP(1, "D Flip Flop", Eln.transistor.newItemStack(1), 4_000.0, 1.0),
    JK_FLIP_FLOP(2, "JK Flip Flop", Eln.transistor.newItemStack(1), 4_000.0, 1.0),
    SR_FLIP_FLOP(3, "SR Flip Flop", Eln.transistor.newItemStack(1), 4_000.0, 1.0),
    ALU(4, "8 Bit ALU", Eln.alu.newItemStack(1), 10_000.0, 0.5)
}

class FabricatorProcess(val element: FabricatorElement): IProcess {

    var powerConsumed = 0.0

    override fun process(time: Double) {
        val operation = element.operation

        val outputSlot = element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId)
        val siliconWaferSlot = element.inventory.getStackInSlot(FabricatorSlots.SILICON_WAFER.slotId)
        val plateCopperSlot = element.inventory.getStackInSlot(FabricatorSlots.COPPER_PLATE.slotId)

        val siliconWaferName = "Silicon_Wafer"
        val copperPlateName = "Copper_Plate"

        val canOutput = if (outputSlot != null) {
            val stack = element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId)
            if (operation != null)
                stack.item == operation.outputItem.item && stack.stackSize < stack.maxStackSize
            else
                true
        } else {
            true
        }

        val hasInputs = (
            siliconWaferSlot != null &&
            plateCopperSlot != null &&
            siliconWaferSlot.unlocalizedName == siliconWaferName &&
            plateCopperSlot.unlocalizedName == copperPlateName
        )

        if (canOutput && hasInputs && operation != null) {
            val power = time * element.resistorLoad.p
            powerConsumed += power
            if (powerConsumed > operation.powerRequired * 2) {
                element.resistorLoad.r = MnaConst.highImpedance
            } else {
                element.resistorLoad.r = 40.0 // This is 200 * 200 / 1000 (volts^2 / watts), prevents current spikes
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
                }
            }
        }
    }
}

class FabricatorRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, descriptor) {

    var operationId: Int = 0

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

    override fun networkUnserialize(stream: DataInputStream?) {
        super.networkUnserialize(stream)
        operationId = stream?.readInt()?: -1
    }
}

const val slotSize = 16;
const val buttonWidth = 20

class FabricatorGui(player: EntityPlayer, inventory: IInventory, val render: FabricatorRender): GuiContainerEln(FabricatorContainer(null, player, inventory, render.transparentNodedescriptor as FabricatorDescriptor)) {

    val buttonsArray = mutableListOf<GuiButtonEln>()

    override fun newHelper() = GuiHelperContainer(this, 176, 164, 8, 80)

    override fun initGui() {
        super.initGui()
        FabricatorOperation.values().forEachIndexed { idx, _ ->
            val column: Int = idx / 3
            val row: Int = idx % 3

            buttonsArray.add(newGuiButton(6 + slotSize * 3 + 4 + (22 * column), 6 + (22 * row), buttonWidth, ""))
        }
        buttonsArray[render.operationId].displayString = "[  ]"
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        FabricatorOperation.values().forEachIndexed { idx, operation ->
            RenderHelper.enableStandardItemLighting()
            RenderHelper.enableGUIStandardItemLighting()

            val column: Int = idx / 3
            val row: Int = idx % 3

            UtilsClient.drawItemStack(operation.outputItem, 6 + slotSize * 3 + 4 + this.guiLeft + 2 + (22 * column), 6 + (22 * idx) + this.guiTop + 2 + (22 * row), null, true)
            RenderHelper.disableStandardItemLighting()
        }

        buttonsArray.forEach { it.displayString = "" }
        buttonsArray[render.operationId].displayString = "[  ]"
    }

    override fun guiObjectEvent(obj: IGuiObject?) {
        super.guiObjectEvent(obj)
        buttonsArray.mapIndexed { idx, it -> Pair(idx, it)}.filter { it.second == obj }.forEach {
            render.clientSendInt(FabricatorNetwork.BUTTON_CLICK.id, it.first)
        }
    }
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
                        SlotWithSkin(inventory, index, 6 + slotSize, 6 + slotSize * 2, SlotSkin.big)
                    }
                    FabricatorSlots.COPPER_PLATE.slotId -> {
                        SlotWithSkin(inventory, index, 6, 6, SlotSkin.medium)
                    }
                    FabricatorSlots.SILICON_WAFER.slotId -> {
                        SlotWithSkin(inventory, index, 6 + slotSize * 2, 6, SlotSkin.medium)
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
