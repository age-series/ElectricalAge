package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.gui.*
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.INodeContainer
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class FabricatorDescriptor(
    name: String
): TransparentNodeDescriptor(name, FabricatorElement::class.java, FabricatorRender::class.java) {

    private val fabricatorModel: Obj3D = Eln.obj.getObj("fabricator")
    private val body: Obj3D.Obj3DPart = fabricatorModel.getPart("Cube.002_Cube.013")
    private val etcherX: Obj3D.Obj3DPart = fabricatorModel.getPart("Cube.003_Cube.012")
    private val etcherZ: Obj3D.Obj3DPart = fabricatorModel.getPart("Cube.001_Cube.010")

    fun draw(isRunning: Boolean = false) {
        GL11.glTranslated(-0.5, -0.5, 0.5)
        body.draw()
        if (isRunning)
            GL11.glTranslated(0.3 * Math.random() - 0.15, -0.1 * Math.random(), 0.0)
        etcherX.draw()
        etcherZ.draw()
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper) = true

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        draw()
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: EntityPlayer?, list: MutableList<String>?, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list?.addAll(tr("The Fabricator creates chips\nfrom silicon and copper plates").split("\n"))
        list?.add(tr("Nominal Ohms: %1$",Utils.plotOhm(40.0)))
    }
}

class FabricatorElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    var operation: FabricatorOperation? = FabricatorOperation.TRANSISTOR

    val electricalLoad = NbtElectricalLoad("load")
    val resistorLoad = Resistor(electricalLoad, null)

    private val craftingProcess = FabricatorProcess(this)

    override val inventory: TransparentNodeElementInventory = FabricatorInventory(FabricatorSlots.values().size, 64, this)

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return if (side == Direction.ZN && lrdu == LRDU.Down) electricalLoad else null
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? = null

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return if (side == Direction.ZN && lrdu == LRDU.Down) NodeBase.maskElectricalPower else 0
    }

    override fun multiMeterString(side: Direction) = Utils.plotUIP(resistorLoad.voltage, resistorLoad.current, resistorLoad.resistance)

    override fun thermoMeterString(side: Direction): String = ""

    override fun getWaila(): Map<String, String> {
        return mapOf(Pair(tr("Operation"), operation?.outputItem?.displayName ?: "None"))
    }

    override fun initialize() {
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(resistorLoad)
        electricalLoad.serialResistance = Eln.getSmallRs()
        resistorLoad.resistance = MnaConst.highImpedance
        slowProcessList.add(craftingProcess)
        connect()
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun hasGui() = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return FabricatorContainer(this.node, player, inventory, descriptor as FabricatorDescriptor)
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeInt(operation?.nid ?: -1)
        stream.writeBoolean(resistorLoad.power > 1.0)
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        val packetType = super.networkUnserialize(stream)
        try {
            when (packetType) {
                FabricatorNetwork.BUTTON_CLICK.id -> {
                    val op: Int = stream.readInt()
                    val thing = FabricatorOperation.values().filter { op == it.nid }
                    if (thing.isNotEmpty()) {
                        operation = thing.first()
                        // Restart the crafting process entirely
                        craftingProcess.powerConsumed = 0.0
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

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val id = nbt.getInteger("operation")
        operation = FabricatorOperation.values().firstOrNull { it.nid == id }
        craftingProcess.powerConsumed = nbt.getDouble("powerConsumed")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        if (operation != null)
            nbt.setInteger("operation", operation?.nid?: 0)
        nbt.setDouble("powerConsumed", craftingProcess.powerConsumed)
    }
}

enum class FabricatorNetwork(val id: Byte) {
    BUTTON_CLICK(0)
}

enum class FabricatorOperation(val nid: Int, val opName: String, val outputItem: ItemStack, val perSheet: Int, val yieldPercentage: Double) {
    // Digital Chips
    TRANSISTOR(0, "Transistor", Eln.transistor.newItemStack(1), 16, 1.0),
    D_FLIP_FLOP(1, "D Flip Flop", Eln.findItemStack("D Flip Flop Chip", 1), 4, 1.0),
    JK_FLIP_FLOP(2, "JK Flip Flop", Eln.findItemStack("JK Flip Flop Chip", 1), 4, 1.0),
    ALU(3, "8 Bit ALU", Eln.alu.newItemStack(1), 2, 0.5),
    PAL_CHIP(4, "PAL Chip", Eln.findItemStack("PAL Chip", 1), 4, 1.0),
    OSCILLATOR_CHIP(5, "Oscillator Chip", Eln.findItemStack("Oscillator Chip", 1), 4, 1.0),

    // Analog Chips
    OP_AMP(6, "OpAmp", Eln.findItemStack("OpAmp", 1), 4, 1.0),
    PID_REGULATOR(7, "PID Regulator", Eln.findItemStack("PID Regulator", 1), 4, 1.0),
    VCO_SAW(8, "Voltage controlled sawtooth oscillator", Eln.findItemStack("Voltage controlled sawtooth oscillator", 1), 4, 1.0),
    VCO_SIM(9, "Voltage controlled sine oscillator", Eln.findItemStack("Voltage controlled sine oscillator", 1), 4, 1.0),
    AMPLIFIER(10, "Amplifier", Eln.findItemStack("Amplifier", 1), 4, 1.0),
    VCA(11, "Voltage controlled amplifier", Eln.findItemStack("Voltage controlled amplifier", 1), 4, 1.0),
    SUM(12, "Configurable summing unit", Eln.findItemStack("Configurable summing unit", 1), 4, 1.0),
    SAH(13, "Sample and hold", Eln.findItemStack("Sample and hold", 1), 4, 1.0),
    LPF(14, "Lowpass filter", Eln.findItemStack("Lowpass filter", 1), 4, 1.0),
}

class FabricatorProcess(val element: FabricatorElement): IProcess {

    var powerConsumed = 0.0
    private val powerRequired = 4_000.0

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
                stack!!.item == operation.outputItem.item && stack.stackSize + operation.perSheet < stack.maxStackSize
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
            val power = time * element.resistorLoad.power
            powerConsumed += power
            if (powerConsumed > powerRequired * 2) {
                element.resistorLoad.resistance = MnaConst.highImpedance
            } else {
                element.resistorLoad.resistance = 40.0 // This is 200 * 200 / 1000 (volts^2 / watts), prevents current spikes
            }
        } else {
            element.resistorLoad.resistance = MnaConst.highImpedance
        }

        if (operation?.outputItem != null && powerRequired <= powerConsumed) {
            // Operation completed. Results!

            element.inventory.decrStackSize(FabricatorSlots.COPPER_PLATE.slotId, 1)
            element.inventory.decrStackSize(FabricatorSlots.SILICON_WAFER.slotId, 1)
            if (Math.random() <= operation.yieldPercentage) {
                if (element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId) == null) {
                    val newStack = operation.outputItem.copy()
                    newStack.stackSize = operation.perSheet
                    element.inventory.setInventorySlotContents(FabricatorSlots.OUTPUT.slotId, newStack)
                    powerConsumed -= powerRequired
                    element.needPublish()
                } else {
                    val stackSize = element.inventory.getStackInSlot(FabricatorSlots.OUTPUT.slotId)!!.stackSize
                    if (stackSize in 0..63) {
                        val newStack = operation.outputItem.copy()
                        newStack.stackSize = stackSize + operation.perSheet
                        element.inventory.setInventorySlotContents(FabricatorSlots.OUTPUT.slotId, newStack)
                        powerConsumed -= powerRequired
                        element.needPublish()
                    }
                }
            }
        }
    }
}

class FabricatorRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(entity, descriptor) {

    var operationId: Int = 0
    private var isRunning = false

    override val inventory = FabricatorInventory(3, 64, this)

    init {
        this.transparentNodedescriptor = descriptor as FabricatorDescriptor
    }

    override fun draw() {
        (this.transparentNodedescriptor as FabricatorDescriptor).draw(isRunning)
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return FabricatorGui(player, inventory, this)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        operationId = stream.readInt()
        isRunning = stream.readBoolean()
    }
}

const val slotSize = 16
const val buttonWidth = 20

class FabricatorGui(player: EntityPlayer, inventory: IInventory, val render: FabricatorRender): GuiContainerEln(FabricatorContainer(null, player, inventory, render.transparentNodedescriptor as FabricatorDescriptor)) {

    private val buttonsArray = mutableListOf<GuiButtonEln>()

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

            UtilsClient.drawItemStack(operation.outputItem, 6 + slotSize * 3 + 4 + this.guiLeft + 2 + (22 * column), 6 + this.guiTop + 2 + (22 * row), null, true)
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
    override val node: NodeBase?,
    player: EntityPlayer,
    inventory: IInventory,
    descriptor: FabricatorDescriptor
): BasicContainer(player, inventory, getSlot(inventory, descriptor)), INodeContainer {

    override val refreshRateDivider = 1

    companion object {
        private fun getSlot(inventory: IInventory, @Suppress("UNUSED_PARAMETER") descriptor: FabricatorDescriptor): Array<Slot> {
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

    override fun canInsertItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        if (stack == null) return false
        val itemDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(stack) ?: return false
        if (itemDescriptor === Eln.siliconWafer && slot == FabricatorSlots.SILICON_WAFER.slotId) return true
        if (itemDescriptor === Eln.plateCopper && slot == FabricatorSlots.COPPER_PLATE.slotId) return true
        return false
    }

    override fun canExtractItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return slot == FabricatorSlots.OUTPUT.slotId
    }
}
