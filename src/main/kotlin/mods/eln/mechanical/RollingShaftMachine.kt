package mods.eln.mechanical

import mods.eln.Eln
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.HelperStdContainer
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.gui.SlotWithSkinAndComment
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.misc.Direction.Companion.fromIntMinecraftSide
import mods.eln.node.transparent.*
import mods.eln.sim.StackMachineProcess
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

class RollingShaftMachineDescriptor (name: String, override val obj: Obj3D) :
    SimpleShaftDescriptor(name, RollingShaftMachineElement::class, RollingShaftMachineRender::class, EntityMetaTag.Basic) {

    override val static = arrayOf(obj.getPart("main"))
    override val rotating = arrayOf(obj.getPart("rot1"))
    private val reverseRotating = arrayOf(obj.getPart("rot2"))

    override val sound = "eln:plate_machine"



    override fun draw(angle: Double) {
        super.draw(angle)
        preserveMatrix {
            val bb = reverseRotating[0].boundingBox()
            val centre = bb.centre()
            val ox = centre.xCoord
            val oy = centre.yCoord
            val oz = centre.zCoord
            GL11.glTranslated(ox, oy, oz)
            GL11.glRotatef(((-angle * 360) / 2.0 / Math.PI).toFloat(), 0f, 0f, 1f)
            GL11.glTranslated(-ox, -oy, -oz)
            for (part in reverseRotating) {
                part.draw()
            }
        }
    }
}

class RollingShaftMachineElement(node: TransparentNode, desc: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc) {
    val desc = desc as RollingShaftMachineDescriptor
    // change size to 4 when adding roller slots
    val inv = RollingShaftMachineInventory(2, 64, this)
    override val inventory = inv

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info[tr("Energy")] = Utils.plotEnergy("", shaft.energy)
        info[tr("Speed")] = Utils.plotRads("", shaft.rads)
        info[tr("Process State")] = Utils.plotPercent("", operationalProcess.getProcessState())
        info[tr("Can Process")] = if (operationalProcess.canSmelt()) "Yes" else "No"
        return info
    }

    override fun coordonate(): Coordinate {
        return node!!.element!!.coordinate()
    }

    override fun hasGui() = true

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return RollingShaftMachineContainer(player, inv)
    }

    private val maximumRate = 4000.0

    private val energyProcess = {
        if (maximumRate > shaft.energy) {
            shaft.energy
        } else {
            maximumRate
        }
    }

    private val energyConsumer = { usedEnergy: Double ->
        shaft.energy -= usedEnergy
    }

    private val operationalProcess = StackMachineProcess(
        inv, 0, 1, 1, Eln.instance.plateMachineRecipes, energyProcess, energyConsumer)

    init {
        slowProcessList.add(operationalProcess)
    }
}

class RollingShaftMachineRender(entity: TransparentNodeEntity, desc: TransparentNodeDescriptor): ShaftRender(entity, desc) {
    val desc = desc as RollingShaftMachineDescriptor
    // change size to 4 when adding roller slots
    val inv = RollingShaftMachineInventory(2, 64, this)
    override val inventory = inv

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return RollingShaftMachineGui(player, inv, this)
    }
}

const val cellOffset = 20

class RollingShaftMachineContainer(player: EntityPlayer, inv: IInventory) : BasicContainer(
    player, inv, arrayOf(
        SlotWithSkinAndComment(inv, 0, 8 + cellOffset, 12, SlotSkin.medium, arrayOf("Input Slot")),
        SlotWithSkinAndComment(inv, 1, 8 + cellOffset, 12 + cellOffset * 2, SlotSkin.big, arrayOf("Output Slot"))//,
        //SlotWithSkinAndComment(inv, 2, 8, 12 + cellOffset, SlotSkin.none, arrayOf("Roller Slot")),
        //SlotWithSkinAndComment(inv, 3, 8 + cellOffset * 2, 12 + cellOffset, SlotSkin.none, arrayOf("Roller Slot"))
    )
)

class RollingShaftMachineGui(player: EntityPlayer, inv: IInventory, val render: RollingShaftMachineRender) : GuiContainerEln(RollingShaftMachineContainer(player, inv)) {
    override fun newHelper() = HelperStdContainer(this)
}

class RollingShaftMachineInventory: TransparentNodeElementInventory {
    private var machineElement: RollingShaftMachineElement? = null

    constructor(size: Int, stackLimit: Int, machineElement: RollingShaftMachineElement?) : super(size, stackLimit, machineElement) {
        this.machineElement = machineElement
    }

    constructor(size: Int, stackLimit: Int, render: TransparentNodeElementRender?) : super(size, stackLimit, render)

    override fun getAccessibleSlotsFromSide(side: Int): IntArray {
        return if (transparentNodeElement == null) IntArray(0) else when (fromIntMinecraftSide(side)) {
            Direction.YP -> intArrayOf(0)
            else -> intArrayOf(1)
        }
    }

    override fun canInsertItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return when (fromIntMinecraftSide(side)) {
            Direction.YP -> true
            else -> false
        }
    }

    override fun canExtractItem(slot: Int, stack: ItemStack?, side: Int): Boolean {
        return when (fromIntMinecraftSide(side)) {
            Direction.YP -> false
            else -> true
        }
    }
}
