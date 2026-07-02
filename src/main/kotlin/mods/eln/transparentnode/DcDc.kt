package mods.eln.transparentnode

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageSlot
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.ISlotSkin.SlotSkin
import mods.eln.i18n.I18N.tr
import mods.eln.item.CaseItemDescriptor
import mods.eln.item.ConfigCopyToolDescriptor
import mods.eln.item.CopperCableDescriptor
import mods.eln.item.FerromagneticCoreDescriptor
import mods.eln.item.IConfigurable
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.NodePeriodicPublishProcess
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.process.TransformerInterSystemProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.nbt.NbtThermalLoad
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

class DcDcDescriptor(
    name: String,
    objM: Obj3D,
    coreM: Obj3D,
    casingM: Obj3D,
    val minimalLoadToHum: Float,
    val guiTexture: String = "dcdc.png"
):
    TransparentNodeDescriptor(name, DcDcElement::class.java, DcDcRender::class.java) {

    companion object {
        const val COIL_SCALE: Float = 4.0f
        const val COIL_SCALE_LIMIT: Int = 16
        const val COIL_BASE_HEIGHT: Float = 0.031f
        const val COIL_OUTER_HALF_WIDTH: Float = 0.0868f
        const val COIL_CORE_HALF_WIDTH: Float = 0.0625f
        const val COIL_CENTER_Y: Float = 0.0155f
        const val COIL_CENTER_Z: Float = -0.3125f
        const val COIL_STACK_MIN_Y: Float = -0.1575f
        const val COIL_STACK_MAX_Y: Float = 0.25f
        const val COIL_STACK_CENTER_Y: Float = (COIL_STACK_MIN_Y + COIL_STACK_MAX_Y) * 0.5f
        const val COIL_STACK_HEIGHT: Float = COIL_STACK_MAX_Y - COIL_STACK_MIN_Y
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
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        list?.add(tr("This DC/DC has unrealistic capacitance effects and can sink/source power that violates Newton's laws"))
        list?.add(tr("It is made this way to improve the performance of the simulator in large power networks"))
        return RealisticEnum.UNREALISTIC
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
            draw(core, 1, 4, 1.0f, 1.0f, false, 0f)
        }
    }

    internal fun draw(
        core: Obj3D.Obj3DPart?,
        priCableNbr: Int,
        secCableNbr: Int,
        primaryThickness: Float,
        secondaryThickness: Float,
        hasCasing: Boolean,
        doorOpen: Float
    ) {
        main?.draw()
        core?.draw()
        if (core != null) {
            drawCoils(priCableNbr, false, primaryThickness)
            drawCoils(secCableNbr, true, secondaryThickness)
        }

        if (hasCasing) {
            casing?.draw()
            casingLeftDoor?.draw(-doorOpen * 90, 0f, 1f, 0f)
            casingRightDoor?.draw(doorOpen * 90, 0f, 1f, 0f)
        }
    }

    private fun drawCoils(count: Int, secondary: Boolean, thickness: Float) {
        if (count == 0) return
        val wireScale = thickness.coerceIn(0.05f, 1.85f)
        val wireHeight = COIL_BASE_HEIGHT * wireScale
        val gap = wireHeight * 0.5f
        val pitch = wireHeight + gap
        val displayedCount = kotlin.math.min(count, ((COIL_STACK_HEIGHT + gap) / pitch).toInt().coerceAtLeast(1))
        val firstCenter = -pitch * (displayedCount - 1) / 2f
        val radialScale = (COIL_CORE_HALF_WIDTH + wireHeight) / COIL_OUTER_HALF_WIDTH
        GL11.glPushMatrix()
        if (secondary) GL11.glRotatef(180f, 0f, 1f, 0f)
        for (idx in 0 until displayedCount) {
            GL11.glPushMatrix()
            GL11.glTranslatef(0f, COIL_STACK_CENTER_Y + firstCenter + pitch * idx, COIL_CENTER_Z)
            GL11.glScalef(radialScale, wireScale, radialScale)
            GL11.glTranslatef(0f, -COIL_CENTER_Y, -COIL_CENTER_Z)
            coil?.draw()
            GL11.glPopMatrix()
        }
        GL11.glPopMatrix()
    }
}

class DcDcElement(transparentNode: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(transparentNode, descriptor), IConfigurable {
    val primaryLoad = NbtElectricalLoad("primaryLoad")
    val secondaryLoad = NbtElectricalLoad("secondaryLoad")

    val primaryVoltageSource = VoltageSource("primaryVoltageSource")
    val secondaryVoltageSource = VoltageSource("secondaryVoltageSource")

    val interSystemProcess = TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource)
    override val inventory = TransparentNodeElementInventory(4, 64, this)
    private val primaryThermalLoad = NbtThermalLoad("primaryThermalLoad")
    private val secondaryThermalLoad = NbtThermalLoad("secondaryThermalLoad")
    private val primaryThermalProcess = DcDcWindingThermalProcess(
        owner = this,
        inventory = inventory,
        thermalLoad = primaryThermalLoad,
        slot = DcDcContainer.primaryCableSlotId,
        current = { primaryVoltageSource.current },
        label = "Primary",
        onMelted = {
            computeInventory()
            reconnect()
        }
    )
    private val secondaryThermalProcess = DcDcWindingThermalProcess(
        owner = this,
        inventory = inventory,
        thermalLoad = secondaryThermalLoad,
        slot = DcDcContainer.secondaryCableSlotId,
        current = { secondaryVoltageSource.current },
        label = "Secondary",
        onMelted = {
            computeInventory()
            reconnect()
        }
    )

    var primaryMeltCurrent = 0.0
    var secondaryMeltCurrent = 0.0

    val primaryVoltageWatchdog = VoltageStateWatchDog(primaryLoad)
    val secondaryVoltageWatchdog = VoltageStateWatchDog(secondaryLoad)

    var populated = false

    var ratioControl = 1.0

    init {
        electricalLoadList.add(primaryLoad)
        electricalLoadList.add(secondaryLoad)
        electricalComponentList.add(primaryVoltageSource)
        electricalComponentList.add(secondaryVoltageSource)
        thermalLoadList.add(primaryThermalLoad)
        thermalLoadList.add(secondaryThermalLoad)
        slowProcessList.add(primaryThermalProcess)
        slowProcessList.add(secondaryThermalProcess)
        primaryThermalLoad.setAsSlow()
        secondaryThermalLoad.setAsSlow()
        val exp = WorldExplosion(this).machineExplosion()
        slowProcessList.add(primaryVoltageWatchdog.setDestroys(exp))
        slowProcessList.add(secondaryVoltageWatchdog.setDestroys(exp))
        slowProcessList.add(NodePeriodicPublishProcess(node!!, 1.0, .5))
        slowProcessList.add(DcDcProcess(this))
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
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        if (lrdu != LRDU.Down) return null
        return when (side) {
            front.left() -> primaryThermalLoad
            front.right() -> secondaryThermalLoad
            else -> null
        }
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        if (lrdu != LRDU.Down) return 0
        return when (side) {
            front.left() -> NodeBase.maskElectricalPower
            front.right() -> NodeBase.maskElectricalPower
            else -> 0
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
        return when (side) {
            front.left() -> plotAmbientCelsius("T", primaryThermalLoad.temperatureCelsius)
            front.right() -> plotAmbientCelsius("T", secondaryThermalLoad.temperatureCelsius)
            else -> tr(
                "P: %1$ S: %2$",
                plotAmbientCelsius("", primaryThermalLoad.temperatureCelsius).trim(),
                plotAmbientCelsius("", secondaryThermalLoad.temperatureCelsius).trim()
            )
        }
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
        val primaryCable = inventory.getStackInSlot(DcDcContainer.primaryCableSlotId)
        val secondaryCable = inventory.getStackInSlot(DcDcContainer.secondaryCableSlotId)
        val core = inventory.getStackInSlot(DcDcContainer.ferromagneticSlotId)
        val primaryWinding = dcDcWinding(primaryCable)
        val secondaryWinding = dcDcWinding(secondaryCable)

        primaryVoltageWatchdog.setNominalVoltage(120_000.0)
        secondaryVoltageWatchdog.setNominalVoltage(120_000.0)

        primaryMeltCurrent = dcDcWindingMeltCurrent(primaryCable)
        secondaryMeltCurrent = dcDcWindingMeltCurrent(secondaryCable)
        primaryThermalProcess.configure(primaryCable)
        secondaryThermalProcess.configure(secondaryCable)

        val coreDescriptor = GenericItemUsingDamageDescriptor.getDescriptor(
            core, FerromagneticCoreDescriptor::class.java) as? FerromagneticCoreDescriptor
        val coreFactor = coreDescriptor?.cableMultiplicator ?: 1.0
        val hasValidCore = coreDescriptor != null

        if (primaryWinding == null || !hasValidCore) {
            primaryLoad.highImpedance()
            populated = false
        } else {
            primaryLoad.serialResistance = coreFactor * 0.01
        }

        if (secondaryWinding == null || !hasValidCore) {
            secondaryLoad.highImpedance()
            populated = false
        } else {
            secondaryLoad.serialResistance = coreFactor * 0.01
        }

        populated = primaryWinding != null && secondaryWinding != null && hasValidCore
        ratioControl = if (populated && primaryWinding != null && secondaryWinding != null) {
            secondaryWinding.amount / primaryWinding.amount
        } else {
            1.0
        }
    }

    fun meltWindingIfOverCurrent(): Boolean {
        val meltedPrimary = meltDcDcWindingIfOverCurrent(
            inventory,
            DcDcContainer.primaryCableSlotId,
            primaryVoltageSource.current
        )
        val meltedSecondary = meltDcDcWindingIfOverCurrent(
            inventory,
            DcDcContainer.secondaryCableSlotId,
            secondaryVoltageSource.current
        )
        if (meltedPrimary || meltedSecondary) {
            computeInventory()
            needPublish()
            return true
        }
        return false
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

    override fun newContainer(side: Direction, player: EntityPlayer): Container? {
        return DcDcContainer(player, inventory)
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
            stream.writeShort(dcDcRenderedWindingCount(inventory.getStackInSlot(0)))
            stream.writeShort(dcDcRenderedWindingCount(inventory.getStackInSlot(1)))
            stream.writeFloat(dcDcRenderedWindingThickness(inventory.getStackInSlot(DcDcContainer.primaryCableSlotId)))
            stream.writeFloat(dcDcRenderedWindingThickness(inventory.getStackInSlot(DcDcContainer.secondaryCableSlotId)))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(DcDcContainer.ferromagneticSlotId))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(DcDcContainer.primaryCableSlotId))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(DcDcContainer.secondaryCableSlotId))
            node!!.lrduCubeMask.getTranslate(front.down()).serialize(stream)
            var load = 0f
            if (primaryMeltCurrent != 0.0 && secondaryMeltCurrent != 0.0) {
                load = Utils.limit(Math.max(primaryLoad.current / primaryMeltCurrent,
                    secondaryLoad.current / secondaryMeltCurrent).toFloat(), 0f, 1f)
            }
            stream.writeFloat(load)
            stream.writeBoolean(inventory.getStackInSlot(3) != null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getWaila(): Map<String, String> {
        val info = HashMap<String, String>()
        info[tr("Construction")] = dcDcFlexibleConstructionWaila()
        info[tr("Ratio")] = Utils.plotValue(interSystemProcess.ratio)
        info[tr("Primary winding")] = windingStatus(
            inventory.getStackInSlot(DcDcContainer.primaryCableSlotId),
            primaryVoltageSource.current,
            primaryThermalLoad
        )
        info[tr("Secondary winding")] = windingStatus(
            inventory.getStackInSlot(DcDcContainer.secondaryCableSlotId),
            secondaryVoltageSource.current,
            secondaryThermalLoad
        )
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info[tr("Voltages")] = "\u00A7a" + Utils.plotVolt("", primaryLoad.voltage) + " " +
                "\u00A7e" + Utils.plotVolt("", secondaryLoad.voltage)
        }
        info[tr("Subsystem Matrix Size")] = Utils.renderDoubleSubsystemWaila(primaryLoad.subSystem, secondaryLoad.subSystem)
        return info
    }

    private fun dcDcFlexibleConstructionWaila(): String {
        val messages = mutableListOf<String>()
        val core = inventory.getStackInSlot(DcDcContainer.ferromagneticSlotId)
        val primary = inventory.getStackInSlot(DcDcContainer.primaryCableSlotId)
        val secondary = inventory.getStackInSlot(DcDcContainer.secondaryCableSlotId)
        val hasCore = GenericItemUsingDamageDescriptor.getDescriptor(
            core,
            FerromagneticCoreDescriptor::class.java
        ) != null
        if (!hasCore) messages += tr("Needs Core")
        flexibleWindingProblem(primary, tr("Primary"))?.let(messages::add)
        flexibleWindingProblem(secondary, tr("Secondary"))?.let(messages::add)
        if (messages.isEmpty()) return tr("Operational")
        val broken = messages.any { it.contains(tr("Melted")) }
        val title = if (broken) tr("Broken") else tr("Incomplete")
        return title + messages.joinToString(separator = "") { "\n  * $it" }
    }

    private fun flexibleWindingProblem(stack: ItemStack?, label: String): String? {
        if (stack == null) return tr("Needs %1$ Winding", label)
        val descriptor = ElectricalCableDescriptor.getDescriptor(
            stack,
            ElectricalCableDescriptor::class.java
        ) as? ElectricalCableDescriptor ?: return tr("Needs %1$ Winding", label)
        if (descriptor.signalWire) return tr("Needs %1$ Winding", label)
        if (descriptor is mods.eln.sixnode.electricalcable.UtilityCableDescriptor && descriptor.melted) {
            return tr("Melted %1$ requires replacement", label)
        }
        return if (dcDcWinding(stack) == null) tr("Needs %1$ Winding", label) else null
    }

    private fun windingStatus(stack: ItemStack?, current: Double, thermalLoad: NbtThermalLoad): String {
        val descriptor = if (stack == null) {
            null
        } else {
            ElectricalCableDescriptor.getDescriptor(
                stack,
                ElectricalCableDescriptor::class.java
            ) as? ElectricalCableDescriptor
        }
        val name = descriptor?.getName(stack) ?: tr("empty")
        return tr(
            "%1$, %2$, %3$",
            name,
            Utils.plotAmpere("", current).trim(),
            plotAmbientCelsius("", thermalLoad.temperatureCelsius).trim()
        )
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("isolator")) {
            disconnect()
            reconnect()
            needPublish()
        }
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "primary", inventory, DcDcContainer.primaryCableSlotId, invoker))
            inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "secondary", inventory, DcDcContainer.secondaryCableSlotId, invoker))
            inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "core", inventory, DcDcContainer.ferromagneticSlotId, invoker))
            inventoryChange(inventory)
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "primary", inventory.getStackInSlot(DcDcContainer.primaryCableSlotId))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "secondary", inventory.getStackInSlot(DcDcContainer.secondaryCableSlotId))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "core", inventory.getStackInSlot(DcDcContainer.ferromagneticSlotId))
    }
}

class DcDcProcess(val element: DcDcElement): IProcess {

    companion object {
        const val MAX_RATIO = 256.0
        const val MIN_RATIO = 1.0 / 256.0
    }

    override fun process(time: Double) {
        if (!element.populated) {
            element.interSystemProcess.ratio = 1.0
            return
        }
        val ratio = when {
            element.ratioControl > MAX_RATIO -> MAX_RATIO
            element.ratioControl < MIN_RATIO -> MIN_RATIO
            else -> element.ratioControl
        }
        if (ratio.isFinite()) {
            element.interSystemProcess.ratio = ratio
        } else {
            element.interSystemProcess.ratio = 1.0
        }
    }
}

class DcDcRender(tileEntity: TransparentNodeEntity, val descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, descriptor) {

    override val inventory = TransparentNodeElementInventory(4, 64, this)

    val load = SlewLimiter(0.5f)

    var primaryStackSize = 0
    var secondaryStackSize = 0
    var primaryThickness = 1.0f
    var secondaryThickness = 1.0f
    var priRender: CableRenderDescriptor? = null
    var secRender: CableRenderDescriptor? = null

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
                return if (load.position > (descriptor as DcDcDescriptor).minimalLoadToHum)
                    0.1f * (load.position - descriptor.minimalLoadToHum) / (1 - descriptor.minimalLoadToHum)
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
        (descriptor as DcDcDescriptor).draw(
            feroPart,
            primaryStackSize.toInt(),
            secondaryStackSize.toInt(),
            primaryThickness,
            secondaryThickness,
            hasCasing,
            doorOpen.get()
        )
        GL11.glPopMatrix()
        cableRenderType = drawCable(front!!.down(), priRender, priConn, cableRenderType)
        cableRenderType = drawCable(front!!.down(), secRender, secConn, cableRenderType)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            primaryStackSize = stream.readShort().toInt()
            secondaryStackSize = stream.readShort().toInt()
            primaryThickness = stream.readFloat()
            secondaryThickness = stream.readFloat()
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
        return DcDcGui(player, inventory, this)
    }
}

class DcDcGui(player: EntityPlayer, inventory: IInventory, val render: DcDcRender): GuiContainerEln(DcDcContainer(player, inventory)) {
    override fun newHelper(): GuiHelperContainer {
        val descriptor = render.descriptor as DcDcDescriptor
        return GuiHelperContainer(this, 176, 194 - 33 + 20, 8, 84 + 194 - 166 - 33 + 20, descriptor.guiTexture)
    }
}

class DcDcContainer(player: EntityPlayer, inventory: IInventory) : BasicContainer(player, inventory,
    arrayOf(
        DcDcWindingSlot(inventory, primaryCableSlotId, 58, 30, 16,
            arrayOf(tr("Power cable or wire slot"))),
        DcDcWindingSlot(inventory, secondaryCableSlotId, 100, 30, 16,
            arrayOf(tr("Power cable or wire slot"))),
        GenericItemUsingDamageSlot(inventory, ferromagneticSlotId, 58 + (100 - 58) / 2, 30, 1,
            arrayOf<Class<*>>(FerromagneticCoreDescriptor::class.java),
            SlotSkin.medium, arrayOf(tr("Ferromagnetic core slot"))),
        GenericItemUsingDamageSlot(inventory, CasingSlotId, 130, 74, 1,
            arrayOf<Class<*>>(CaseItemDescriptor::class.java),
            SlotSkin.medium, arrayOf(tr("Casing slot")))))
    {
    companion object {
        const val primaryCableSlotId = 0
        const val secondaryCableSlotId = 1
        const val ferromagneticSlotId = 2
        const val CasingSlotId = 3
    }
}
