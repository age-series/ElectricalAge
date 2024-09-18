package mods.eln.transparentnode.floodlight

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.ConfigCopyToolDescriptor
import mods.eln.item.LampDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.HybridNodeDirection
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.Utils.getItemObject
import mods.eln.misc.Utils.plotAmpere
import mods.eln.misc.Utils.plotPower
import mods.eln.misc.Utils.plotValue
import mods.eln.misc.Utils.plotVolt
import mods.eln.node.AutoAcceptInventoryProxy
import mods.eln.node.NodeBase
import mods.eln.node.published
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.node.transparent.TransparentNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.MonsterPopFreeProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class FloodlightElement(transparentNode: TransparentNode, transparentNodeDescriptor: TransparentNodeDescriptor) : TransparentNodeElement(transparentNode, transparentNodeDescriptor) {

    companion object {
        const val CONE_WIDTH_SELECT_EVENT: Byte = 0
        const val CONE_RANGE_SELECT_EVENT: Byte = 1
        const val HORIZONTAL_ADJUST_EVENT: Byte = 2
        const val VERTICAL_ADJUST_EVENT: Byte = 3
    }

    override val inventory = TransparentNodeElementInventory(2, 64, this)

    private val acceptingInventory = AutoAcceptInventoryProxy(inventory)
        .acceptIfEmpty(FloodlightContainer.LAMP_SLOT_1_ID, LampDescriptor::class.java)
        .acceptIfEmpty(FloodlightContainer.LAMP_SLOT_2_ID, LampDescriptor::class.java)

    override val descriptor = transparentNodeDescriptor as FloodlightDescriptor

    private var initialPlacement = true

    val motorized = descriptor.motorized

    private var rotationAxis: HybridNodeDirection = (descriptor.placementSide).toHybridNodeDirection()
    private lateinit var blockFacing: HybridNodeDirection

    var powered by published(false)
    var swivelAngle by published(0f)
    var headAngle by published(0f)
    private var coneWidth by published(FloodlightConeWidth.NARROW)
    private var coneRange by published(FloodlightConeRange.NEAR)

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    private val lamp1Resistor: Resistor = Resistor(electricalLoad, null)
    private val lamp2Resistor: Resistor = Resistor(electricalLoad, null)

    val swivelControl = NbtElectricalGateInput("swivelControl")
    val headControl = NbtElectricalGateInput("headControl")

    private val voltageWatchdog = VoltageStateWatchDog(electricalLoad)

    private val watchdogProcess = voltageWatchdog.setDestroys(WorldExplosion(this).machineExplosion())
    private val floodlightProcess = FloodlightProcess(this)
    private val monsterPopProcess = MonsterPopFreeProcess(transparentNode.coordinate, Eln.instance.killMonstersAroundLampsRange)

    init {
        if (motorized) {
            electricalLoadList.add(swivelControl)
            electricalLoadList.add(headControl)
        }

        electricalLoad.serialResistance = ((Eln.MVU * Eln.MVU) / Eln.instance.MVP()) * 0.005 // NOTE: power factor comes from MV cable registration
        voltageWatchdog.setNominalVoltage(Eln.MVU)

        slowProcessList.add(watchdogProcess)
        slowProcessList.add(floodlightProcess)
        slowProcessList.add(monsterPopProcess)
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return FloodlightContainer(player, inventory)
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(player)) {
            blockFacing = blockFacing.left(rotationAxis)
            reconnect()
            return true
        }

        return acceptingInventory.take(player.currentEquippedItem, this, true,false)
    }

    override fun getLightOpacity(): Float {
        return 1f
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        initialPlacement = nbt.getBoolean("initialPlacement")
        rotationAxis = HybridNodeDirection.fromInt(nbt.getInteger("rotationAxis"))!!
        blockFacing = HybridNodeDirection.fromInt(nbt.getInteger("blockFacing"))!!
        powered = nbt.getBoolean("powered")
        swivelAngle = nbt.getFloat("swivelAngle")
        headAngle = nbt.getFloat("headAngle")
        coneWidth = FloodlightConeWidth.fromInt(nbt.getInteger("coneWidth"))!!
        coneRange = FloodlightConeRange.fromInt((nbt.getInteger("coneRange")))!!
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setBoolean("initialPlacement", initialPlacement)
        nbt.setInteger("rotationAxis", rotationAxis.int)
        nbt.setInteger("blockFacing", blockFacing.int)
        nbt.setBoolean("powered", powered)
        nbt.setFloat("swivelAngle", swivelAngle)
        nbt.setFloat("headAngle", headAngle)
        nbt.setInteger("coneWidth", coneWidth.int)
        nbt.setInteger("coneRange", coneRange.int)
    }

    override fun initialize() {
        if (initialPlacement) {
            blockFacing = front.toHybridNodeDirection()
            front = rotationAxis.toStandardDirection()
            initialPlacement = false
        }
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(lamp1Resistor)
        electricalComponentList.add(lamp2Resistor)
        computeInventory()
        connect()
    }

    override fun connectJob() {
        electricalLoadList.add(electricalLoad)
        electricalComponentList.add(lamp1Resistor)
        electricalComponentList.add(lamp2Resistor)
        super.connectJob()
    }

    override fun disconnectJob() {
        super.disconnectJob()
        electricalLoadList.remove(electricalLoad)
        electricalComponentList.remove(lamp1Resistor)
        electricalComponentList.remove(lamp2Resistor)
    }

    override fun inventoryChange(inventory: IInventory?) {
        disconnect()
        computeInventory()
        connect()
        needPublish()
    }

    private fun computeInventory() {
        val lamp1Stack = inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID)
        val lamp2Stack = inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID)

        when (lamp1Stack) {
            null -> lamp1Resistor.highImpedance()
            else -> lamp1Resistor.resistance = (getItemObject(lamp1Stack) as LampDescriptor).r
        }

        when (lamp2Stack) {
            null -> lamp2Resistor.highImpedance()
            else -> lamp2Resistor.resistance = (getItemObject(lamp2Stack) as LampDescriptor).r
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        if (createMaskFromLRDU(lrdu)) return null

        return if (motorized) when (side.toHybridNodeDirection()) {
            blockFacing.back() -> electricalLoad
            blockFacing.right(rotationAxis) -> headControl
            blockFacing.left(rotationAxis) -> swivelControl
            else -> null
        }
        else when (side.toHybridNodeDirection()) {
            blockFacing.back() -> electricalLoad
            else -> null
        }
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        println("rotationAxis: $rotationAxis, facing: $blockFacing, inSide: $side, lrdu: $lrdu")

        if (createMaskFromLRDU(lrdu)) return 0

        return if (motorized) when (side.toHybridNodeDirection()) {
            blockFacing.back() -> NodeBase.maskElectricalPower
            blockFacing.right(rotationAxis) -> NodeBase.maskElectricalGate
            blockFacing.left(rotationAxis) -> NodeBase.maskElectricalGate
            else -> 0
        }
        else when (side.toHybridNodeDirection()) {
            blockFacing.back() -> NodeBase.maskElectricalPower
            else -> 0
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeInt(rotationAxis.int)
            stream.writeInt(blockFacing.int)
            stream.writeBoolean(motorized)
            stream.writeBoolean(powered)
            stream.writeFloat(swivelAngle)
            stream.writeFloat(headAngle)
            stream.writeInt(coneWidth.int)
            stream.writeInt(coneRange.int)
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID))
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID))
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createMaskFromLRDU(lrdu: LRDU): Boolean {
        return when (rotationAxis) {
            HybridNodeDirection.XN -> {
                when (blockFacing) {
                    HybridNodeDirection.XN -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.XP -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.YN -> lrdu != LRDU.Up
                    HybridNodeDirection.YP -> lrdu != LRDU.Up
                    HybridNodeDirection.ZN -> lrdu != LRDU.Right
                    HybridNodeDirection.ZP -> lrdu != LRDU.Left
                }
            }
            HybridNodeDirection.XP -> {
                when (blockFacing) {
                    HybridNodeDirection.XN -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.XP -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.YN -> lrdu != LRDU.Down
                    HybridNodeDirection.YP -> lrdu != LRDU.Down
                    HybridNodeDirection.ZN -> lrdu != LRDU.Left
                    HybridNodeDirection.ZP -> lrdu != LRDU.Right
                }
            }
            HybridNodeDirection.YN -> {
                when (blockFacing) {
                    HybridNodeDirection.XN -> lrdu != LRDU.Up
                    HybridNodeDirection.XP -> lrdu != LRDU.Up
                    HybridNodeDirection.YN -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.YP -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.ZN -> lrdu != LRDU.Up
                    HybridNodeDirection.ZP -> lrdu != LRDU.Up
                }
            }
            HybridNodeDirection.YP -> {
                when (blockFacing) {
                    HybridNodeDirection.XN -> lrdu != LRDU.Down
                    HybridNodeDirection.XP -> lrdu != LRDU.Down
                    HybridNodeDirection.YN -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.YP -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.ZN -> lrdu != LRDU.Down
                    HybridNodeDirection.ZP -> lrdu != LRDU.Down
                }
            }
            HybridNodeDirection.ZN -> {
                when (blockFacing) {
                    HybridNodeDirection.XN -> lrdu != LRDU.Left
                    HybridNodeDirection.XP -> lrdu != LRDU.Right
                    HybridNodeDirection.YN -> lrdu != LRDU.Right
                    HybridNodeDirection.YP -> lrdu != LRDU.Left
                    HybridNodeDirection.ZN -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.ZP -> TODO("unused - impossible facing direction")
                }
            }
            HybridNodeDirection.ZP -> {
                when (blockFacing) {
                    HybridNodeDirection.XN -> lrdu != LRDU.Right
                    HybridNodeDirection.XP -> lrdu != LRDU.Left
                    HybridNodeDirection.YN -> lrdu != LRDU.Left
                    HybridNodeDirection.YP -> lrdu != LRDU.Right
                    HybridNodeDirection.ZN -> TODO("unused - impossible facing direction")
                    HybridNodeDirection.ZP -> TODO("unused - impossible facing direction")
                }
            }
        }
    }

    override fun networkUnserialize(stream: DataInputStream): Byte {
        when (super.networkUnserialize(stream)) {
            CONE_WIDTH_SELECT_EVENT -> coneWidth = coneWidth.cycleConeWidth()
            CONE_RANGE_SELECT_EVENT -> coneRange = coneRange.cycleConeRange()
            HORIZONTAL_ADJUST_EVENT -> swivelAngle = stream.readFloat()
            VERTICAL_ADJUST_EVENT -> headAngle = stream.readFloat()
        }
        return unserializeNulldId
    }

    override fun multiMeterString(side: Direction): String {
        return plotVolt("U:", electricalLoad.voltage) + plotAmpere("I:", electricalLoad.current)
    }

    override fun thermoMeterString(side: Direction): String {
        return ""
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = LinkedHashMap()

        info[I18N.tr("Power consumption")] = plotPower("", electricalLoad.voltage * electricalLoad.current)

        val lamp1Stack = inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID)
        val lamp2Stack = inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID)

        if (lamp1Stack != null) info[I18N.tr("Bulb 1")] = lamp1Stack.displayName
        else info[I18N.tr("Bulb 1")] = I18N.tr("None")

        if (lamp2Stack != null) info[I18N.tr("Bulb 2")] = lamp2Stack.displayName
        else info[I18N.tr("Bulb 2")] = I18N.tr("None")

        if (Eln.wailaEasyMode) {
            info[I18N.tr("Voltage")] = plotVolt("", electricalLoad.voltage)

            if (lamp1Stack != null) {
                val lamp1Descriptor = getItemObject(lamp1Stack) as LampDescriptor
                info[I18N.tr("Bulb 1 Life Left")] = plotValue(lamp1Descriptor.getLifeInTag(lamp1Stack)) + I18N.tr(" Hours")
            }
            if (lamp2Stack != null) {
                val lamp2Descriptor = getItemObject(lamp2Stack) as LampDescriptor
                info[I18N.tr("Bulb 2 Life Left")] = plotValue(lamp2Descriptor.getLifeInTag(lamp2Stack)) + I18N.tr(" Hours")
            }
        }

        return info
    }

    fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "lamp", inventory, FloodlightContainer.LAMP_SLOT_1_ID, invoker)) inventoryChange(inventory)
        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "lamp", inventory, FloodlightContainer.LAMP_SLOT_2_ID, invoker)) inventoryChange(inventory)
    }

    fun writeConfigTool(compound: NBTTagCompound) {
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "lamp", inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID))
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "lamp", inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID))
    }

}