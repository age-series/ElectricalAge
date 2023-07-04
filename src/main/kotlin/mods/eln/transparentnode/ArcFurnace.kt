package mods.eln.transparentnode

import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.ghost.GhostGroup
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.item.GraphiteDescriptor
import mods.eln.misc.*
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11

class ArcFurnaceDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, ArcFurnaceElement::class.java, ArcFurnaceRender::class.java) {
    private var main: Obj3DPart? = null

    init {
        main = obj.getPart("ArcFurnace")
        val gg = GhostGroup()
        gg.addRectangle(0, 2, 0, 4, -1, 1)
        gg.removeElement(0, 0, 0)
        ghostGroup = gg
    }

    fun draw(front: Direction) {
        if (main != null) {
            front.glRotateZnRef()
            //GL11.glRotatef(-90f, 0f, 1f, 0f);
            GL11.glTranslatef(-1.5f, -0.5f, 2.5f);
            GL11.glScalef(0.5f, 0.5f, 0.5f);
            main?.draw()
            //UtilsClient.drawEntityItem(inEntity, -0.35, 0.04, 0.3, 1, 1f)
        }
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return false
    }

    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?): Boolean {
        return false
    }
}

class ArcFurnaceElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    var adesc: ArcFurnaceDescriptor? = null;

    init {
        adesc = descriptor as ArcFurnaceDescriptor
    }

    override val inventory = TransparentNodeElementInventory(5, 1, this)
    //private val connectionType: CableRenderType? = null
    //private val eConn = LRDUMask()

    //private val inEntity: EntityItem? = null
    //private val outEntity: EntityItem? = null
    //var powerFactor = 0f
    //var processState = 0f
    //private val processStatePerSecond = 0f

    //var UFactor = 0f

    private val electricalLoad = NbtElectricalLoad("electricalLoad")
    val electricalResistor = Resistor(electricalLoad, null)

    private val voltageWatchdog = VoltageStateWatchDog(electricalLoad)

    init {
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(electricalResistor)
        val exp = WorldExplosion(this).machineExplosion()
        slowProcessList.add(voltageWatchdog.setNominalVoltage(800.0).setDestroys(exp))
    }

    override fun multiMeterString(side: Direction): String {
        return Utils.plotUIP(electricalLoad.voltage, electricalLoad.current)
    }


    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return electricalLoad
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        return NodeBase.maskElectricalPower
    }

    override fun initialize() {
        connect()
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return ArcFurnaceContainer(node, player, inventory)
    }
}

class ArcFurnaceRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, descriptor) {

    override val inventory = TransparentNodeElementInventory(5, 64, this)

    var adesc: ArcFurnaceDescriptor? = null;

    init {
        adesc = descriptor as ArcFurnaceDescriptor;
    }

    override fun draw() {
        adesc?.draw(front!!)
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return ArcFurnaceGui(player, inventory, this)
    }
}

class ArcFurnaceContainer(val node: NodeBase?, player: EntityPlayer?, inventory: IInventory): BasicContainer(
    player, inventory, arrayOf<Slot>(
        GenericItemUsingDamageSlot(
            inventory, 0, 0, 0, 1,
            GraphiteDescriptor::class.java,
            SlotSkin.medium, arrayOf("Graphite Slot")
        ),
        GenericItemUsingDamageSlot(
            inventory, 1, 30, 0, 1,
            GraphiteDescriptor::class.java,
            SlotSkin.medium, arrayOf("Graphite Slot")
        ),
        GenericItemUsingDamageSlot(
            inventory, 2, 15, 15, 1,
            GraphiteDescriptor::class.java,
            SlotSkin.medium, arrayOf("Graphite Slot")
        ),
        GenericItemUsingDamageSlot(
            inventory, 3, 15, 30, 64,
            GraphiteDescriptor::class.java,
            SlotSkin.medium, arrayOf("Input Slot")
        ),
        GenericItemUsingDamageSlot(
            inventory, 4, 15, 45, 64,
            GraphiteDescriptor::class.java,
            SlotSkin.medium, arrayOf("Output Slot")
        )
    ))

class ArcFurnaceGui(player: EntityPlayer?, inventory: IInventory, @Suppress("UNUSED_PARAMETER") render: ArcFurnaceRender): GuiContainerEln(ArcFurnaceContainer(null, player, inventory)) {
    override fun newHelper(): GuiHelperContainer {
            return GuiHelperContainer(this, 176, 166, 50, 84)
    }
}
