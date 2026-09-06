package mods.eln.sixnode.lampsupply

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.ConfigCopyToolDescriptor
import mods.eln.item.IConfigurable
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.Utils.getItemObject
import mods.eln.node.AutoAcceptInventoryProxy
import mods.eln.node.NodeBase
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sixnode.currentcable.CurrentCableDescriptor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator
import mods.eln.sixnode.wirelesssignal.aggregator.IWirelessSignalAggregator
import mods.eln.sixnode.wirelesssignal.aggregator.SmallerAggregator
import mods.eln.sixnode.wirelesssignal.aggregator.ToggleAggregator
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.inventory.Container
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

class LampSupplyElement(sixNode: SixNode, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElement(sixNode, side, sixNodeDescriptor), IConfigurable {

    data class PowerSupplyChannelHandle(val element: LampSupplyElement, val id: Int) {
        companion object {
            @JvmStatic
            fun registerChannel(tx: LampSupplyElement, id: Int, channel: String) {
                if (channel.isEmpty()) return
                if (globalChannelMap[channel] == null) globalChannelMap[channel] = mutableListOf()
                globalChannelMap[channel]!!.add(PowerSupplyChannelHandle(tx, id))
            }

            @JvmStatic
            fun removeChannel(tx: LampSupplyElement, id: Int, channel: String) {
                if (channel.isEmpty()) return
                val channelList = globalChannelMap[channel] ?: return
                val iterator = channelList.iterator()
                while (iterator.hasNext()) {
                    val channelHandler = iterator.next()
                    if (channelHandler.element == tx && channelHandler.id == id) iterator.remove()
                }
                if (channelList.isEmpty()) globalChannelMap.remove(channel)
            }
        }
    }

    data class LocalLampSupplyEntry(var powerChannel: String, var wirelessChannel: String, var aggregator: Int)

    companion object {
        @JvmField
        val globalChannelMap = mutableMapOf<String, MutableList<PowerSupplyChannelHandle>>()

        // This is used to force lamp sockets and other wireless power devices to recheck for the closest available
        // lamp supply every time any lamp supply is added or removed.
        var forceCachedLampSupplyUpdate = false
    }

    override val inventory = SixNodeElementInventory(1, 64, this, LampSupplyContainer.REQUIRED_CABLE_LENGTH)

    // ElectricalCableDescriptor here covers utility cables
    private val inventoryProxy = AutoAcceptInventoryProxy(inventory)
        .acceptIfEmpty(LampSupplyContainer.CABLE_SLOT_ID, ElectricalCableDescriptor::class.java, CurrentCableDescriptor::class.java)

    val descriptor = sixNodeDescriptor as LampSupplyDescriptor

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val loadResistor = Resistor(electricalLoad, null)

    private val voltageWatchdog = VoltageStateWatchDog(electricalLoad)
    private val lampSupplyProcess = LampSupplyProcess(this)

    val localEntries = mutableListOf<LocalLampSupplyEntry>()
    val localChannelStates = mutableListOf<Boolean>()
    val localAggregators = mutableListOf<List<IWirelessSignalAggregator>>()

    // Sum of resistances of all connected devices (in parallel, so equivalent to conductance)
    var connectedConductance = 0.0

    val range: Int
        get() {
            val cableStack = inventory.getStackInSlot(LampSupplyContainer.CABLE_SLOT_ID)
            return if (cableStack == null) descriptor.range else descriptor.range + cableStack.stackSize
        }

    init {
        electricalComponentList.add(loadResistor)
        electricalLoadList.add(electricalLoad)

        voltageWatchdog.setNominalVoltage(descriptor.nominalVoltage)
        voltageWatchdog.setDestroys(WorldExplosion(this).cableExplosion())

        slowProcessList.add(voltageWatchdog)
        slowProcessList.add(lampSupplyProcess)

        repeat(LampSupplyDescriptor.CHANNEL_COUNT) {
            localEntries.add(LocalLampSupplyEntry("", "", 2))
            localChannelStates.add(false)
            localAggregators.add(listOf(BiggerAggregator(), SmallerAggregator(), ToggleAggregator()))
        }
    }

    fun getChannelState(channel: Int): Boolean {
        return localChannelStates[channel]
    }

    fun addToConductance(resistance: Double) {
        connectedConductance += (1.0 / resistance.coerceIn(MnaConst.noImpedance, MnaConst.highImpedance))
    }

    private fun registerAllPowerChannels() {
        var idx = 0
        localEntries.forEach { PowerSupplyChannelHandle.registerChannel(this, idx++, it.powerChannel) }
    }

    private fun unregisterAllPowerChannels() {
        var idx = 0
        localEntries.forEach { PowerSupplyChannelHandle.removeChannel(this, idx++, it.powerChannel) }
    }

    override fun initialize() {
        computeInventory()
    }

    override fun unload() {
        super.unload()
        unregisterAllPowerChannels()
    }

    override fun destroy(entityPlayer: EntityPlayerMP?) {
        super.destroy(entityPlayer)
        unregisterAllPowerChannels()
    }

    override fun inventoryChanged() {
        computeInventory()
        reconnect()
        needPublish()
    }

    private fun computeInventory() {
        when (val cableStack = inventory.getStackInSlot(LampSupplyContainer.CABLE_SLOT_ID)) {
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
            inventory.getStackInSlot(LampSupplyContainer.CABLE_SLOT_ID) == null -> null
            front == lrdu -> electricalLoad
            else -> null
        }
    }

    override fun getThermalLoad(lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getConnectionMask(lrdu: LRDU): Int {
        return when {
            inventory.getStackInSlot(LampSupplyContainer.CABLE_SLOT_ID) == null -> 0
            front == lrdu -> NodeBase.maskElectricalPower
            else -> 0
        }
    }

    override fun multiMeterString(): String {
        return Utils.plotUIP(electricalLoad.voltage, electricalLoad.current)
    }

    override fun thermoMeterString(): String {
        return ""
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = LinkedHashMap()

        for (idx in 0..<LampSupplyDescriptor.CHANNEL_COUNT) {
            if (localEntries[idx].powerChannel.isNotEmpty()) {
                val onOffString = if (localChannelStates[idx]) "§a" + I18N.tr("ON") else "§c" + I18N.tr("OFF")
                info[I18N.tr("Channel %1$", idx + 1)] = I18N.tr("%1$ = %2$", localEntries[idx].powerChannel, onOffString)
            }
        }

        info[I18N.tr("Total power")] = Utils.plotPower("", abs(electricalLoad.voltage) * electricalLoad.current)
        if (Utils.isWailaEasyModeEnabled()) info[I18N.tr("Voltage")] = Utils.plotVolt("", electricalLoad.voltage)
        if (Utils.isDebugEnabled()) info[I18N.tr("Range")] = Utils.plotValue(range.toDouble())

        return info
    }

    override fun newContainer(side: Direction, player: EntityPlayer): Container {
        return LampSupplyContainer(player, inventory)
    }

    override fun hasGui(): Boolean {
        return true
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        if (onBlockActivatedRotate(entityPlayer)) return true
        var takeItem = false

        when (val equippedItemDescriptor = getItemObject(entityPlayer.currentEquippedItem)) {
            // ElectricalCableDescriptor here covers utility cables (utility cables are not signal cables)
            // Spool length check and trimming are handled in AutoAcceptInventoryProxy
            is ElectricalCableDescriptor -> takeItem = !equippedItemDescriptor.signalWire
            is CurrentCableDescriptor -> takeItem = true
        }

        return takeItem && inventoryProxy.take(entityPlayer.currentEquippedItem, this, notifyInventoryChange = true)
    }

    /**
     * The if/else block here addresses the possible existence of legacy NBT tags and should remain in place.
     */
    override fun readFromNBT(nbt: NBTTagCompound) {
        unregisterAllPowerChannels()
        super.readFromNBT(nbt)
        var idx = 0
        if (nbt.hasKey("entry_p0")) while (nbt.hasKey("entry_p$idx")) {
            localEntries[idx] = LocalLampSupplyEntry(nbt.getString("entry_p$idx"), nbt.getString("entry_w$idx"), nbt.getInteger("selectedAggregator$idx"))
            localChannelStates[idx] = nbt.getBoolean("channelStates$idx")
            (localAggregators[idx][2] as ToggleAggregator).readFromNBT(nbt, "toogleAggregator$idx")
            idx++
        } else while (nbt.hasKey("powerChannel$idx")) {
            localEntries[idx] = LocalLampSupplyEntry(nbt.getString("powerChannel$idx"), nbt.getString("wirelessChannel$idx"), nbt.getInteger("aggregator$idx"))
            localChannelStates[idx] = nbt.getBoolean("channelState$idx")
            (localAggregators[idx][2] as ToggleAggregator).readFromNBT(nbt, "toggleAggregator$idx")
            idx++
        }
        registerAllPowerChannels()
        forceCachedLampSupplyUpdate = true
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        var idx = 0
        localEntries.forEach {
            nbt.setString("powerChannel$idx", it.powerChannel)
            nbt.setString("wirelessChannel$idx", it.wirelessChannel)
            nbt.setInteger("aggregator$idx", it.aggregator)
            nbt.setBoolean("channelState$idx", localChannelStates[idx])
            (localAggregators[idx][2] as ToggleAggregator).writeToNBT(nbt, "toggleAggregator$idx")
            idx++
        }
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            localEntries.forEach {
                stream.writeUTF(it.powerChannel)
                stream.writeUTF(it.wirelessChannel)
                stream.writeInt(it.aggregator)
            }
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(LampSupplyContainer.CABLE_SLOT_ID))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            when (stream.readByte()) {
                LampSupplyGui.SET_POWER_NAME_EVENT -> {
                    val id = stream.readInt()
                    val newName = stream.readUTF()
                    PowerSupplyChannelHandle.removeChannel(this, id, localEntries[id].powerChannel)
                    localEntries[id].powerChannel = newName
                    PowerSupplyChannelHandle.registerChannel(this, id, localEntries[id].powerChannel)
                    forceCachedLampSupplyUpdate = true
                }
                LampSupplyGui.SET_WIRELESS_NAME_EVENT -> {
                    val id = stream.readInt()
                    val newName = stream.readUTF()
                    localEntries[id].wirelessChannel = newName
                }
                LampSupplyGui.SET_SELECTED_AGGREGATOR_EVENT -> {
                    val id = stream.readInt()
                    val newAggregator = stream.readInt()
                    localEntries[id].aggregator = newAggregator
                }
            }
            needPublish()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun readConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        var publishChanges = false
        var inventoryChanged = false

        if (compound.hasKey("powerChannels")) {
            val powerChannelList = compound.getTagList("powerChannels", 8)
            for (idx in 0..<minOf(LampSupplyDescriptor.CHANNEL_COUNT, powerChannelList.tagCount())) {
                PowerSupplyChannelHandle.removeChannel(this, idx, localEntries[idx].powerChannel)
                localEntries[idx].powerChannel = powerChannelList.getStringTagAt(idx)
                PowerSupplyChannelHandle.registerChannel(this, idx, localEntries[idx].powerChannel)
            }
            forceCachedLampSupplyUpdate = true
            publishChanges = true
        }

        if (compound.hasKey("wirelessChannels")) {
            val wirelessChannelList = compound.getTagList("wirelessChannels", 8)
            for (idx in 0..<minOf(LampSupplyDescriptor.CHANNEL_COUNT, wirelessChannelList.tagCount())) {
                localEntries[idx].wirelessChannel = wirelessChannelList.getStringTagAt(idx)
            }
            publishChanges = true
        }

        if (compound.hasKey("aggregators")) {
            val aggregatorList = compound.getIntArray("aggregators")
            for (idx in 0..<minOf(LampSupplyDescriptor.CHANNEL_COUNT, aggregatorList.size)) {
                localEntries[idx].aggregator = aggregatorList[idx]
            }
            publishChanges = true
        }

        if (ConfigCopyToolDescriptor.readCableType(compound, inventory, LampSupplyContainer.CABLE_SLOT_ID, invoker, false)) {
            inventoryChanged = true
        }

        // Prevent duplicate calls of these functions
        if (inventoryChanged) inventoryChanged() else if (publishChanges) needPublish()
    }

    override fun writeConfigTool(compound: NBTTagCompound, invoker: EntityPlayer) {
        val powerChannelList = NBTTagList()
        val wirelessChannelList = NBTTagList()
        val aggregatorList = mutableListOf<Int>()

        for (idx in 0..<LampSupplyDescriptor.CHANNEL_COUNT) {
            powerChannelList.appendTag(NBTTagString(localEntries[idx].powerChannel))
            wirelessChannelList.appendTag(NBTTagString(localEntries[idx].wirelessChannel))
            aggregatorList.add(localEntries[idx].aggregator)
        }

        compound.setTag("powerChannels", powerChannelList)
        compound.setTag("wirelessChannels", wirelessChannelList)
        compound.setIntArray("aggregators", aggregatorList.toIntArray())

        ConfigCopyToolDescriptor.writeCableType(compound, inventory.getStackInSlot(LampSupplyContainer.CABLE_SLOT_ID))
    }

}