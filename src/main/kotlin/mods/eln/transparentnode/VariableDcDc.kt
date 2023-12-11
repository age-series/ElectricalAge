package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.ISlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.*
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.process.TransformerInterSystemProcess
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sound.LoopedSound
import mods.eln.wiki.Data
import net.minecraft.client.audio.ISound
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class VariableDcDcDescriptor(name: String, objM: Obj3D, coreM: Obj3D, casingM: Obj3D): TransparentNodeDescriptor(name, VariableDcDcElement::class.java, VariableDcDcRender::class.java) {
    companion object {
        val MIN_LOAD_HUM = 0.5
        val COIL_SCALE: Float = 4.0f
        val COIL_SCALE_LIMIT: Int = 16
    }

    var main: Obj3D.Obj3DPart? = null
    var core: Obj3D.Obj3DPart? = null
    var coil: Obj3D.Obj3DPart? = null
    var casing: Obj3D.Obj3DPart? = null
    var casingLeftDoor: Obj3D.Obj3DPart? = null
    var casingRightDoor: Obj3D.Obj3DPart? = null

    init {
        main = objM.getPart("main")
        coil = objM.getPart("sbire")
        core = coreM.getPart("fero")
        casing = casingM.getPart("Case")
        casingLeftDoor = casingM.getPart("DoorL")
        casingRightDoor = casingM.getPart("DoorR")

        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addWiring(newItemStack())
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *tr("Transforms an input voltage to\nan output voltage.")!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        Collections.addAll(list, *tr("The output voltage is controlled\nfrom a signal input")!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw(core, 1, 4, false, 0f)
        }
    }

    internal fun draw(core: Obj3D.Obj3DPart?, priCableNbr: Int, secCableNbr: Int, hasCasing: Boolean, doorOpen: Float) {
        main?.draw()
        core?.draw()
        if (core != null) {
            if (priCableNbr != 0) {
                var scale = COIL_SCALE
                if (priCableNbr < COIL_SCALE_LIMIT) {
                    scale *= priCableNbr.toFloat() / COIL_SCALE_LIMIT
                }
                GL11.glPushMatrix()
                GL11.glScalef(1f, scale * 2f / (priCableNbr + 1), 1f)
                GL11.glTranslatef(0f, -0.125f * (priCableNbr - 1) / COIL_SCALE, 0f)
                for (idx in 0 until priCableNbr) {
                    coil?.draw()
                    GL11.glTranslatef(0f, 0.25f / COIL_SCALE, 0f)
                }
                GL11.glPopMatrix()
            }
            if (secCableNbr != 0) {
                var scale = COIL_SCALE
                if (secCableNbr < COIL_SCALE_LIMIT) {
                    scale *= secCableNbr.toFloat() / COIL_SCALE_LIMIT
                }
                GL11.glPushMatrix()
                GL11.glRotatef(180f, 0f, 1f, 0f)
                GL11.glScalef(1f, scale * 2f / (secCableNbr + 1), 1f)
                GL11.glTranslatef(0f, -0.125f * (secCableNbr - 1) / COIL_SCALE, 0f)
                for (idx in 0 until secCableNbr) {
                    coil?.draw()
                    GL11.glTranslatef(0f, 0.25f / COIL_SCALE, 0f)
                }
                GL11.glPopMatrix()
            }
        }

        if (hasCasing) {
            casing?.draw()
            casingLeftDoor?.draw(-doorOpen * 90, 0f, 1f, 0f)
            casingRightDoor?.draw(doorOpen * 90, 0f, 1f, 0f)
        }
    }
}

class VariableDcDcElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(transparentNode, descriptor), IConfigurable {
    val primaryLoad = NbtElectricalLoad("primaryLoad")
    val secondaryLoad = NbtElectricalLoad("secondaryLoad")

    val control = NbtElectricalGateInput("control")

    val primaryVoltageSource = VoltageSource("primaryVoltageSource")
    val secondaryVoltageSource = VoltageSource("secondaryVoltageSource")

    val interSystemProcess = TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource)

    override val inventory = TransparentNodeElementInventory(4, 64, this)

    var primaryMaxCurrent = 0.0
    var secondaryMaxCurrent = 0.0

    val primaryVoltageWatchdog = VoltageStateWatchDog(primaryLoad)
    val secondaryVoltageWatchdog = VoltageStateWatchDog(secondaryLoad)

    var populated = false

    init {
        electricalLoadList.add(primaryLoad)
        electricalLoadList.add(secondaryLoad)
        electricalLoadList.add(control)
        electricalComponentList.add(primaryVoltageSource)
        electricalComponentList.add(secondaryVoltageSource)
        val exp = WorldExplosion(this).machineExplosion()
        slowProcessList.add(primaryVoltageWatchdog.setDestroys(exp))
        slowProcessList.add(secondaryVoltageWatchdog.setDestroys(exp))
        slowProcessList.add(NodePeriodicPublishProcess(node!!, 1.0, .5))
        slowProcessList.add(VariableDcDcProcess(this))
    }

    override fun disconnectJob() {
        super.disconnectJob()
        Eln.simulator.mna.removeProcess(interSystemProcess)

    }

    override fun connectJob() {
        Eln.simulator.mna.addProcess(interSystemProcess)
        super.connectJob()
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front.right() -> secondaryLoad
            front.left() -> primaryLoad
            front -> control
            front.back() -> control
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front -> NodeBase.maskElectricalGate
            front.back() -> NodeBase.maskElectricalGate
            else -> NodeBase.maskElectricalPower
        }
    }

    override fun multiMeterString(side: Direction): String {
        if (side == front.left())
            return Utils.plotVolt("UP+:", primaryLoad.voltage) + Utils.plotAmpere("IP+:", -primaryLoad.current)
        return if (side == front.right())
            Utils.plotVolt("US+:", secondaryLoad.voltage) + Utils.plotAmpere("IS+:", -secondaryLoad.current)
        else
            Utils.plotVolt("UP+:", primaryLoad.voltage) + Utils.plotAmpere("IP+:", primaryVoltageSource.current) + Utils.plotVolt("  US+:", secondaryLoad.voltage) + Utils.plotAmpere("IS+:", secondaryVoltageSource.current)
    }

    override fun thermoMeterString(side: Direction): String {
        return ""
    }

    override fun initialize() {
        primaryVoltageSource.connectTo(primaryLoad, null)
        secondaryVoltageSource.connectTo(secondaryLoad, null)
        electricalComponentList.add(primaryVoltageSource)
        electricalComponentList.add(secondaryVoltageSource)
        interSystemProcess.ratio = 1.0
        computeInventory()
        connect()
    }

    private fun computeInventory() {
        val primaryCable = inventory.getStackInSlot(VariableDcDcContainer.primaryCableSlotId)
        val secondaryCable = inventory.getStackInSlot(VariableDcDcContainer.secondaryCableSlotId)
        val core = inventory.getStackInSlot(VariableDcDcContainer.ferromagneticSlotId)

        primaryVoltageWatchdog.setNominalVoltage(120_000.0)
        secondaryVoltageWatchdog.setNominalVoltage(120_000.0)

        primaryMaxCurrent = 5.0
        secondaryMaxCurrent = 5.0

        var coreFactor = 1.0
        if (core != null) {
            val coreDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(core) as FerromagneticCoreDescriptor
            coreFactor = coreDescriptor.cableMultiplicator
        }

        if (primaryCable == null || core == null || primaryCable.stackSize < 4) {
            primaryLoad.highImpedance()
            populated = false
        } else {
            primaryLoad.serialResistance = coreFactor * 0.01
        }

        if (secondaryCable == null || core == null || secondaryCable.stackSize < 4) {
            secondaryLoad.highImpedance()
            populated = false
        } else {
            secondaryLoad.serialResistance = coreFactor * 0.01
        }

        populated = primaryCable != null && secondaryCable != null && primaryCable.stackSize >= 4 && secondaryCable.stackSize >= 4 && core != null
    }

    override fun inventoryChange(inventory: IInventory?) {
        disconnect()
        computeInventory()
        connect()
        needPublish()
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return VariableDcDcContainer(player, inventory)
    }

    override fun getLightOpacity(): Float {
        return 1.0f
    }

    override fun onGroundedChangedByClient() {
        super.onGroundedChangedByClient()
        computeInventory()
        reconnect()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            if (inventory.getStackInSlot(0) == null)
                stream.writeByte(0)
            else
                stream.writeByte(inventory.getStackInSlot(0)!!.stackSize)
            if (inventory.getStackInSlot(1) == null)
                stream.writeByte(0)
            else
                stream.writeByte(inventory.getStackInSlot(1)!!.stackSize)
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(VariableDcDcContainer.ferromagneticSlotId))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(VariableDcDcContainer.primaryCableSlotId))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(VariableDcDcContainer.secondaryCableSlotId))
            node!!.lrduCubeMask.getTranslate(front.down()).serialize(stream)
            var load = 0f
            if (primaryMaxCurrent != 0.0 && secondaryMaxCurrent != 0.0) {
                load = Utils.limit(Math.max(primaryLoad.current / primaryMaxCurrent,
                    secondaryLoad.current / secondaryMaxCurrent).toFloat(), 0f, 1f)
            }
            stream.writeFloat(load)
            stream.writeBoolean(inventory.getStackInSlot(3) != null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()
        info[tr("Ratio")] = Utils.plotValue(interSystemProcess.ratio)
        // It's just not fair not to show the voltages on the VDC/DC. It's so variable...
        info["Voltages"] = "\u00A7a" + Utils.plotVolt("", primaryLoad.voltage) + " " +
            "\u00A7e" + Utils.plotVolt("", secondaryLoad.voltage)
        info["Control Voltage"] = Utils.plotVolt(control.voltage)
        info[tr("Subsystem Matrix Size: ")] = Utils.renderDoubleSubsystemWaila(primaryLoad.subSystem, secondaryLoad.subSystem)
        return info
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("isolator")) {
            disconnect()
            reconnect()
            needPublish()
        }
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "primary", inventory, VariableDcDcContainer.primaryCableSlotId, invoker))
            inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "secondary", inventory, VariableDcDcContainer.secondaryCableSlotId, invoker))
            inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "core", inventory, VariableDcDcContainer.ferromagneticSlotId, invoker))
            inventoryChange(inventory)
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "primary", inventory.getStackInSlot(VariableDcDcContainer.primaryCableSlotId))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "secondary", inventory.getStackInSlot(VariableDcDcContainer.secondaryCableSlotId))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "core", inventory.getStackInSlot(VariableDcDcContainer.ferromagneticSlotId))
    }
}

class VariableDcDcProcess(val element: VariableDcDcElement): IProcess {
    override fun process(time: Double) {
        val ratio = when {
            element.control.normalized > 0.9 -> 0.9
            element.control.normalized < 0.0 -> 0.0
            else -> element.control.normalized
        }
        if (ratio.isFinite()) {
            element.interSystemProcess.ratio = 1-ratio
        } else {
            element.interSystemProcess.ratio = 1.0
        }
    }
}

class VariableDcDcRender(tileEntity: TransparentNodeEntity, val descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, descriptor) {

    override val inventory = TransparentNodeElementInventory(4, 64, this)

    val load = SlewLimiter(0.5f)

    var primaryStackSize: Byte = 0
    var secondaryStackSize: Byte = 0
    var priRender: CableRenderDescriptor? = null
    var secRender: CableRenderDescriptor? = null
    var controlRender: CableRenderDescriptor? = null

    private var feroPart: Obj3D.Obj3DPart? = null
    private var hasCasing = false

    private val coordinate: Coordinate
    private val doorOpen: PhysicalInterpolator

    private val priConn = LRDUMask()
    private val secConn = LRDUMask()
    private val controlConn = LRDUMask()
    private val eConn = LRDUMask()
    private var cableRenderType: CableRenderType? = null

    init {
        addLoopedSound(object : LoopedSound("eln:Transformer", coordinate(), ISound.AttenuationType.LINEAR) {
            override fun getVolume(): Float {
                return if (load.position > VariableDcDcDescriptor.MIN_LOAD_HUM)
                    0.1f * (load.position - VariableDcDcDescriptor.MIN_LOAD_HUM).toFloat() / (1 - VariableDcDcDescriptor.MIN_LOAD_HUM).toFloat()
                else
                    0f
            }
        })

        coordinate = Coordinate(tileEntity)
        doorOpen = PhysicalInterpolator(0.4f, 4.0f, 0.9f, 0.05f)
    }

    override fun draw() {
        GL11.glPushMatrix()
        front!!.glRotateXnRef()
        (descriptor as VariableDcDcDescriptor).draw(feroPart, primaryStackSize.toInt(), secondaryStackSize.toInt(), hasCasing, doorOpen.get())
        GL11.glPopMatrix()
        cableRenderType = drawCable(front!!.down(), priRender, priConn, cableRenderType)
        cableRenderType = drawCable(front!!.down(), secRender, secConn, cableRenderType)
        cableRenderType = drawCable(front!!.down(), Eln.instance.stdCableRenderSignal, controlConn, cableRenderType)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            primaryStackSize = stream.readByte()
            secondaryStackSize = stream.readByte()
            val feroStack = Utils.unserialiseItemStack(stream)
            if (feroStack != null) {
                val feroDesc: GenericItemUsingDamageDescriptor? = GenericItemUsingDamageDescriptor.getDescriptor(feroStack, FerromagneticCoreDescriptor::class.java)
                if (feroDesc != null)
                    feroPart = (feroDesc as FerromagneticCoreDescriptor).feroPart
            }
            val priStack = Utils.unserialiseItemStack(stream)
            if (priStack != null) {
                val priDesc: GenericItemBlockUsingDamageDescriptor? = ElectricalCableDescriptor.getDescriptor(priStack, ElectricalCableDescriptor::class.java)
                if (priDesc != null)
                    priRender = (priDesc as ElectricalCableDescriptor).render
            }

            val secStack = Utils.unserialiseItemStack(stream)
            if (secStack != null) {
                val secDesc: GenericItemBlockUsingDamageDescriptor? = ElectricalCableDescriptor.getDescriptor(secStack, ElectricalCableDescriptor::class.java)
                if (secDesc != null)
                    secRender = (secDesc as ElectricalCableDescriptor).render
            }

            eConn.deserialize(stream)

            priConn.mask = 0
            secConn.mask = 0
            controlConn.mask = 0
            for (lrdu in LRDU.values()) {
                if(!eConn.get(lrdu)) continue
                if(front!!.down().applyLRDU(lrdu) == front!!.left()) {
                    priConn.set(lrdu, true)
                    continue
                }
                if(front!!.down().applyLRDU(lrdu) == front!!.right()) {
                    secConn.set(lrdu, true)
                    continue
                }
                controlConn.set(lrdu, true)
            }
            cableRenderType = null

            load.target = stream.readFloat()
            hasCasing = stream.readBoolean()

        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun getCableRenderSide(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        if (lrdu == LRDU.Down) {
            if (side == front!!.left()) return priRender
            if (side == front!!.right()) return secRender
            if (side == front && !grounded) return priRender
            if (side == front!!.back() && !grounded) return secRender
        }
        return null
    }

    override fun notifyNeighborSpawn() {
        super.notifyNeighborSpawn()
        cableRenderType = null
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        load.step(deltaT)

        if (hasCasing) {
            if (!Utils.isPlayerAround(tileEntity.worldObj, coordinate.moved(front!!).getAxisAlignedBB(0)))
                doorOpen.target = 0f
            else
                doorOpen.target = 1f
            doorOpen.step(deltaT)
        }
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return VariableDcDcGui(player, inventory, this)
    }
}

class VariableDcDcGui(player: EntityPlayer, inventory: IInventory, val render: VariableDcDcRender): GuiContainerEln(VariableDcDcContainer(player, inventory)) {
    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 194 - 33 + 20, 8, 84 + 194 - 166 - 33 + 20, "vdcdc.png")
    }
}

class VariableDcDcContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory,
    arrayOf(
        GenericItemUsingDamageSlot(inventory, primaryCableSlotId, 58, 30, 4,
            arrayOf<Class<*>>(CopperCableDescriptor::class.java),
            ISlotSkin.SlotSkin.medium, arrayOf(tr("Copper cable slot"), tr("4 Copper Cables Required"))),
        GenericItemUsingDamageSlot(inventory, secondaryCableSlotId, 100, 30, 4,
            arrayOf<Class<*>>(CopperCableDescriptor::class.java),
            ISlotSkin.SlotSkin.medium, arrayOf(tr("Copper cable slot"), tr("4 Copper Cables Required"))),
        GenericItemUsingDamageSlot(inventory, ferromagneticSlotId, 58 + (100 - 58) / 2, 30, 1,
            arrayOf<Class<*>>(FerromagneticCoreDescriptor::class.java),
            ISlotSkin.SlotSkin.medium, arrayOf(tr("Ferromagnetic core slot"))),
        GenericItemUsingDamageSlot(inventory, CasingSlotId, 130, 74, 1,
            arrayOf<Class<*>>(CaseItemDescriptor::class.java),
            ISlotSkin.SlotSkin.medium, arrayOf(tr("Casing slot")))))
    {
    companion object {
        const val primaryCableSlotId = 0
        const val secondaryCableSlotId = 1
        const val ferromagneticSlotId = 2
        const val CasingSlotId = 3
    }
}
