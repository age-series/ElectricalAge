package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.BrushDescriptor
import mods.eln.item.ConfigCopyToolDescriptor
import mods.eln.item.IConfigurable
import mods.eln.item.lampitem.LampDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.NominalVoltage
import mods.eln.misc.Utils
import mods.eln.misc.Utils.getItemObject
import mods.eln.misc.Utils.plotAmpere
import mods.eln.misc.Utils.plotPower
import mods.eln.misc.Utils.plotValue
import mods.eln.misc.Utils.plotVolt
import mods.eln.misc.Utils.serialiseItemStack
import mods.eln.node.AutoAcceptInventoryProxy
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.MonsterPopFreeProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.IInventory
import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.pow

class LampSocketElement(sixNode: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, sixNodeDescriptor), IConfigurable {

    override val inventory = SixNodeElementInventory(2, 64, this, LampSocketContainer.REQUIRED_CABLE_LENGTH)

    // ElectricalCableDescriptor here covers utility cables
    private val inventoryAcceptor = AutoAcceptInventoryProxy(inventory)
        .acceptIfEmpty(LampSocketContainer.LAMP_SLOT_ID, LampDescriptor::class.java)
        .acceptIfEmpty(LampSocketContainer.CABLE_SLOT_ID, ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java)

    val descriptor = sixNodeDescriptor as LampSocketDescriptor

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val lampResistor = Resistor(electricalLoad, null)

    private val voltageWatchdog = VoltageStateWatchDog(electricalLoad)

    private val watchdogProcess = voltageWatchdog.setDestroys(WorldExplosion(this).cableExplosion())
    private val monsterPopProcess = MonsterPopFreeProcess(sixNode.coordinate,
        Eln.config.getIntOrElse("entities.mobSpawning.preventNearLampsRange", 9))
    private val lampSocketProcess = LampSocketProcess(this)

    var poweredByLampSupply = true
    var lampSupplyChannel = "Default channel"
    var activeLampSupplyConnection = false
    var projectionRotationAngle = 0.0
    private var paintColor = LampSocketRender.DEFAULT_PAINT_COLOR
    private var grounded = true

    init {
        // We currently have 12V, 120V, and 240V bulbs, so lamp sockets should be able to handle voltages up to 240V nominal
        voltageWatchdog.setNominalVoltage(NominalVoltage.V240)

        slowProcessList.add(watchdogProcess)
        slowProcessList.add(monsterPopProcess)
        slowProcessList.add(lampSocketProcess)
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return LampSocketContainer(player, inventory, descriptor)
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            reconnect()
            needPublish()
            return true
        }

        var takeItem = false

        when (val equippedItemDescriptor = getItemObject(entityPlayer.currentEquippedItem)) {
            is BrushDescriptor -> {
                // Ignore brush use on non-paintable sockets (e.g. Streetlight)
                if (!descriptor.paintable) return false

                val brushColor = equippedItemDescriptor.getColor(entityPlayer.currentEquippedItem)

                if (brushColor != paintColor && equippedItemDescriptor.use(entityPlayer.currentEquippedItem, entityPlayer)) {
                    paintColor = brushColor
                    needPublish()
                }

                return true
            }

            is LampDescriptor -> {
                takeItem = equippedItemDescriptor.lampData.technology in descriptor.acceptedLampTypes
            }

            // ElectricalCableDescriptor here covers utility cables (utility cables are not signal cables)
            // Spool length check and trimming are handled in AutoAcceptInventoryProxy
            is ElectricalCableDescriptor -> {
                takeItem = !equippedItemDescriptor.signalWire
            }

            is CurrentCableDescriptor -> {
                takeItem = true
            }
        }

        return if (takeItem) {
            inventoryAcceptor.take(entityPlayer.currentEquippedItem, this, notifyInventoryChange = true)
        } else false
    }

    /**
     * The if/else blocks here address the possible existence of legacy NBT tags and should remain in place.
     */
    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)

        if (nbt.hasKey("front")) {
            val temp = nbt.getByte("front")
            front = LRDU.fromInt((temp.toInt() shr 0) and 0x3)
            grounded = (temp.toInt() and 4) != 0
            nbt.removeTag("front")
        } else {
            front = LRDU.fromInt(nbt.getInteger("frontNew"))
            grounded = nbt.getBoolean("grounded")
        }

        if (nbt.hasKey("color")) {
            paintColor = if (descriptor.paintable) nbt.getByte("color").toInt() and 0xF else LampSocketRender.DEFAULT_PAINT_COLOR
            nbt.removeTag("color")
        } else {
            paintColor = if (descriptor.paintable) nbt.getInteger("paintColor") else LampSocketRender.DEFAULT_PAINT_COLOR
        }

        poweredByLampSupply = nbt.getBoolean("poweredByLampSupply")

        if (nbt.hasKey("channel")) {
            lampSupplyChannel = nbt.getString("channel")
            nbt.removeTag("channel")
        } else {
            lampSupplyChannel = nbt.getString("lampSupplyChannel")
        }

        projectionRotationAngle = nbt.getDouble("projectionRotationAngle")
        lampSocketProcess.stableLightProbability = nbt.getDouble("stableLightProbability")
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setInteger("frontNew", front.toInt())
        nbt.setBoolean("grounded", grounded)
        nbt.setInteger("paintColor", paintColor)
        nbt.setBoolean("poweredByLampSupply", poweredByLampSupply)
        nbt.setString("lampSupplyChannel", lampSupplyChannel)
        nbt.setDouble("projectionRotationAngle", projectionRotationAngle)
        nbt.setDouble("stableLightProbability", lampSocketProcess.stableLightProbability)
    }

    override fun initialize() {
        computeInventory()
    }

    override fun connectJob() {
        if (!poweredByLampSupply) {
            electricalLoadList.add(electricalLoad)
            electricalComponentList.add(lampResistor)
        }
        super.connectJob()
    }

    override fun disconnectJob() {
        super.disconnectJob()
        electricalLoadList.remove(electricalLoad)
        electricalComponentList.remove(lampResistor)
        electricalLoad.state = 0.0
    }

    override fun inventoryChange(inventory: IInventory?) {
        computeInventory()
        reconnect()
        needPublish()
    }

    fun computeInventory() {
        val lampStack = inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
        val cableStack = inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID)

        when (lampStack) {
            null -> lampResistor.highImpedance()
            else -> (getItemObject(lampStack) as LampDescriptor).applyTo(lampResistor)
        }

        when (cableStack) {
            null -> electricalLoad.highImpedance()
            else -> {
                val cableDescriptor = Eln.sixNodeItem.getDescriptor(cableStack)

                // ElectricalCableDescriptor here covers utility cables
                if (cableDescriptor is ElectricalCableDescriptor || cableDescriptor is CurrentCableDescriptor) {
                    cableDescriptor.applyTo(electricalLoad)
                }
            }
        }
    }

    override fun getElectricalLoad(lrdu: LRDU, mask: Int): ElectricalLoad? {
        return when {
            inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID) == null -> null
            poweredByLampSupply -> null
            grounded -> electricalLoad
            else -> null
        }
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return when {
            inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID) == null -> 0
            poweredByLampSupply -> 0
            grounded -> NodeBase.maskElectricalPower
            front == lrdu || front == lrdu.inverse() -> NodeBase.maskElectricalPower
            else -> 0
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)

        try {
            stream.writeBoolean(poweredByLampSupply)
            stream.writeUTF(lampSupplyChannel)
            stream.writeBoolean(activeLampSupplyConnection)
            stream.writeDouble(projectionRotationAngle)
            stream.writeInt(paintColor)
            stream.writeBoolean(grounded)
            stream.writeInt(sixNode!!.lightValue)
            serialiseItemStack(stream, inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID))
            serialiseItemStack(stream, inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        try {
            when (stream.readByte()) {
                LampSocketGui.TOGGLE_GROUNDED_EVENT -> {
                    grounded = !grounded
                    reconnect()
                }
                LampSocketGui.TOGGLE_POWER_SOURCE_EVENT -> {
                    poweredByLampSupply = !poweredByLampSupply
                    reconnect()
                }
                LampSocketGui.UPDATE_LAMP_SUPPLY_CHANNEL_EVENT -> lampSupplyChannel = stream.readUTF()
                LampSocketGui.ADJUST_ROTATION_ANGLE_EVENT -> projectionRotationAngle = stream.readDouble()
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun multiMeterString(): String {
        return plotVolt("U:", electricalLoad.voltage) + plotAmpere("I:", electricalLoad.current)
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = LinkedHashMap()

        info[I18N.tr("Power Consumption")] = plotPower("", electricalLoad.voltage.pow(2) / lampResistor.resistance)

        val lampStack = inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
        if (lampStack != null) info[I18N.tr("Bulb")] = lampStack.displayName
        else info[I18N.tr("Bulb")] = I18N.tr("None")

        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info[I18N.tr("Voltage")] = plotVolt("", electricalLoad.voltage)

            if (lampStack != null) {
                val lampDescriptor = getItemObject(lampStack) as LampDescriptor
                info[I18N.tr("Bulb Life Left")] = plotValue(lampDescriptor.getLifeInTag(lampStack)) + I18N.tr(" Hours")
            }

            if (poweredByLampSupply) info[I18N.tr("Channel")] = lampSupplyChannel
        }

        if (Eln.config.getBooleanOrElse("debug.logging.enabled", false)) {
            info[I18N.tr("Lamp Brightness")] = plotValue(sixNode!!.lightValue.toDouble())
        }

        return info
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        var publishChanges = false
        var inventoryChanged = false

        if (compound.hasKey("poweredByLampSupply")) {
            poweredByLampSupply = compound.getBoolean("poweredByLampSupply")
            publishChanges = true
        }

        if (compound.hasKey("lampSupplyChannel")) {
            lampSupplyChannel = compound.getString("lampSupplyChannel")
            publishChanges = true
        }

        if (descriptor.enableProjectionRotation && compound.hasKey("projectionRotationAngle")) {
            projectionRotationAngle = compound.getDouble("projectionRotationAngle")
            publishChanges = true
        }

        if (compound.hasKey("grounded")) {
            grounded = compound.getBoolean("grounded")
            publishChanges = true
        }

        if (ConfigCopyToolDescriptor.readLampDescriptor(compound, "lamp", inventory, LampSocketContainer.LAMP_SLOT_ID, invoker, descriptor.acceptedLampTypes)) {
            inventoryChanged = true
        }

        if (ConfigCopyToolDescriptor.readCableType(compound, inventory, LampSocketContainer.CABLE_SLOT_ID, invoker, false)) {
            inventoryChanged = true
        }

        // Prevent duplicate calls of these functions
        if (inventoryChanged) inventoryChange(inventory)
        else if (publishChanges) needPublish()
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setBoolean("poweredByLampSupply", poweredByLampSupply)
        compound.setString("lampSupplyChannel", lampSupplyChannel)
        if (descriptor.enableProjectionRotation) compound.setDouble("projectionRotationAngle", projectionRotationAngle)
        compound.setBoolean("grounded", grounded)
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "lamp", inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID))
        ConfigCopyToolDescriptor.writeCableType(compound, inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID))
    }

}