package mods.eln.sixnode.lampsocket

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.BrushDescriptor
import mods.eln.item.ConfigCopyToolDescriptor
import mods.eln.item.IConfigurable
import mods.eln.item.LampDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
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

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
class LampSocketElement(sixNode: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, sixNodeDescriptor), IConfigurable {

    companion object {
        const val SET_GROUNDED_ID: Byte = 0
        const val TOGGLE_POWER_SUPPLY_TYPE_ID: Byte = 1
        const val SET_LAMP_SUPPLY_CHANNEL_ID: Byte = 2
        const val SET_ALPHA_Z_ID: Byte = 3
    }

    override val inventory = SixNodeElementInventory(2, 64, this)

    private val acceptingInventory = AutoAcceptInventoryProxy(inventory)
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

    private var grounded = true
    var lampSupplyChannel = "Default channel"
    var poweredByLampSupply = true
    var activeLampSupplyConnection = false
    private var paintColor = 15

    var itemsInInventory = false
    var processElapsedTime = 0.0

    override val lightValue: Int
        get() = lampSocketProcess.blockLight

    init {
        // TODO: revisit this
        // NOTE: Power factor (0.005) comes from MV cable registration
        electricalLoad.serialResistance = ((Eln.MVU * Eln.MVU) / Eln.instance.MVP()) * 0.005

        // We currently have both 50V and 200V bulbs, so lamp sockets should be able to handle voltages up to 200V nominal
        voltageWatchdog.setNominalVoltage(Eln.MVU)

        slowProcessList.add(watchdogProcess)
        slowProcessList.add(monsterPopProcess)
        slowProcessList.add(lampSocketProcess)
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return LampSocketContainer(player, inventory)
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            front = front.nextClockwise
            if (descriptor.rotateOnlyBy180Deg) front = front.nextClockwise
            reconnect()
            needPublish()
            return true
        }

        val playerEquippedItem = getItemObject(entityPlayer.currentEquippedItem)

        if (playerEquippedItem is BrushDescriptor) {
            // Ignore brush use on non-paintable sockets (e.g. Streetlight)
            if (!descriptor.paintable) return false

            val brushColor = playerEquippedItem.getColor(entityPlayer.currentEquippedItem)

            if (brushColor != paintColor && playerEquippedItem.use(entityPlayer.currentEquippedItem, entityPlayer)) {
                paintColor = brushColor
                needPublish()
            }

            return true
        }

        if (playerEquippedItem is LampDescriptor) {
            if (playerEquippedItem.lampData.technology in LampSocketContainer.ACCEPTED_LAMP_TYPES) {
                return acceptingInventory.take(
                    entityPlayer.currentEquippedItem, this, publish = true, notifyInventoryChange = true
                )
            }
        }

        if (playerEquippedItem is ElectricalCableDescriptor || playerEquippedItem is CurrentCableDescriptor) {
            return acceptingInventory.take(
                entityPlayer.currentEquippedItem, this, publish = true, notifyInventoryChange = true
            )
        }

        return false
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

        poweredByLampSupply = nbt.getBoolean("poweredByLampSupply")

        if (nbt.hasKey("channel")) {
            lampSupplyChannel = nbt.getString("channel")
            nbt.removeTag("channel")
        } else {
            lampSupplyChannel = nbt.getString("lampSupplyChannel")
        }

        if (nbt.hasKey("color")) {
            paintColor = if (descriptor.paintable) nbt.getByte("color").toInt() and 0xF else 0x0F
            nbt.removeTag("color")
        } else {
            paintColor = if (descriptor.paintable) nbt.getInteger("paintColor") else 0
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setInteger("frontNew", front.toInt())
        nbt.setBoolean("grounded", grounded)
        nbt.setBoolean("poweredByLampSupply", poweredByLampSupply)
        nbt.setString("lampSupplyChannel", lampSupplyChannel)
        nbt.setInteger("paintColor", paintColor)
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
            else -> {
                val lampDescriptor = getItemObject(lampStack) as LampDescriptor
                lampDescriptor.applyTo(lampResistor)
            }
        }

        when (cableStack) {
            null -> electricalLoad.highImpedance()
            else -> {
                val cableDescriptor = Eln.sixNodeItem.getDescriptor(cableStack)

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
            serialiseItemStack(stream, inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID))
            serialiseItemStack(stream, inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID))
            stream.writeBoolean(grounded)
            stream.writeUTF(lampSupplyChannel)
            stream.writeBoolean(poweredByLampSupply)
            stream.writeBoolean(activeLampSupplyConnection)
            stream.writeInt(paintColor)
            stream.writeDouble(lampSocketProcess.alphaZ)
            stream.writeInt(lampSocketProcess.light)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        try {
            when (stream.readByte()) {
                SET_GROUNDED_ID -> {
                    grounded = stream.readByte().toInt() != 0
                    inventoryChange(inventory)
                }

                TOGGLE_POWER_SUPPLY_TYPE_ID -> {
                    poweredByLampSupply = !poweredByLampSupply
                    reconnect()
                }

                SET_LAMP_SUPPLY_CHANNEL_ID -> {
                    lampSupplyChannel = stream.readUTF()
                    needPublish()
                }

                SET_ALPHA_Z_ID -> {
                    lampSocketProcess.alphaZ = stream.readFloat().toDouble()
                    needPublish()
                }
            }
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

        return info
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        if (compound.hasKey("poweredByLampSupply")) {
            poweredByLampSupply = compound.getBoolean("poweredByLampSupply")
            needPublish()
        }

        if (compound.hasKey("lampSupplyChannel")) {
            lampSupplyChannel = compound.getString("lampSupplyChannel")
            needPublish()
        }

        if (ConfigCopyToolDescriptor.readGenDescriptor(compound, "lamp", inventory, LampSocketContainer.LAMP_SLOT_ID, invoker)) {
            inventoryChange(inventory)
        }

        if (ConfigCopyToolDescriptor.readCableType(compound, inventory, LampSocketContainer.CABLE_SLOT_ID, invoker)) {
            inventoryChange(inventory)
        }
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        compound.setBoolean("poweredByLampSupply", poweredByLampSupply)
        compound.setString("lampSupplyChannel", lampSupplyChannel)
        ConfigCopyToolDescriptor.writeGenDescriptor(compound, "lamp", inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID))
        ConfigCopyToolDescriptor.writeCableType(compound, inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID))
    }

}