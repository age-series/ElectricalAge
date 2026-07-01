package mods.eln.simplenode.computerprobe

import cpw.mods.fml.common.Optional
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Context
import mods.eln.Eln
import mods.eln.Other
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.misc.Version
import mods.eln.node.NodeBase
import mods.eln.node.simple.SimpleNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.nbt.NbtElectricalGateInputOutput
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx
import mods.eln.sixnode.wirelesssignal.WirelessUtils
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator
import mods.eln.sixnode.wirelesssignal.aggregator.IWirelessSignalAggregator
import mods.eln.sixnode.wirelesssignal.aggregator.SmallerAggregator
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement
import net.minecraft.nbt.NBTTagCompound

@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = Other.modIdCc)
class ComputerProbeNode : SimpleNode(), IPeripheral {
    @JvmField
    val ioGate = arrayOfNulls<NbtElectricalGateInputOutput>(6)

    @JvmField
    val ioGateProcess = arrayOfNulls<NbtElectricalGateOutputProcess>(6)

    private var spotTimeout = 0.0
    private var spot: IWirelessSignalSpot? = null
    private val txSet = HashMap<String, HashSet<IWirelessSignalTx>>()
    private val txStrength = HashMap<IWirelessSignalTx, Double>()
    private val wirelessTxMap = HashMap<String, WirelessTx>()

    override fun initialize() {
        slowProcessList.add(SlowProcess())

        for (idx in 0 until 6) {
            val gate = NbtElectricalGateInputOutput("ioGate$idx")
            val process = NbtElectricalGateOutputProcess("ioGateProcess$idx", gate)
            ioGate[idx] = gate
            ioGateProcess[idx] = process

            electricalLoadList.add(gate)
            electricalComponentList.add(process)

            process.isHighImpedance = true
        }
        connect()
    }

    private inner class SlowProcess : IProcess {
        override fun process(time: Double) {
            if (spot != null) {
                spotTimeout -= time
                if (spotTimeout < 0) {
                    spot = null
                    txSet.clear()
                    txStrength.clear()
                }
            }
        }
    }

    private fun wirelessRead(channel: String, aggregator: IWirelessSignalAggregator): Double? {
        if (spot == null) {
            spot = WirelessUtils.buildSpot(coordinate, null, 0)
            txSet.clear()
            txStrength.clear()
            WirelessUtils.getTx(spot, txSet, txStrength)
            spotTimeout = Utils.rand(1.0, 2.0)
        }

        val txs = txSet[channel] ?: return null
        return aggregator.aggregate(txs)
    }

    private fun aggregatorFor(name: String): IWirelessSignalAggregator? {
        return when (name.lowercase()) {
            "bigger" -> BiggerAggregator()
            "smaller" -> SmallerAggregator()
            else -> null
        }
    }

    private fun directionFor(name: String): Direction? {
        return Direction.values().firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    private fun softError(reason: String): Array<Any?> {
        return arrayOf(null, reason)
    }

    override fun onBreakBlock() {
        super.onBreakBlock()
        unregister()
    }

    override fun unload() {
        super.unload()
        unregister()
    }

    private fun unregister() {
        for (tx in wirelessTxMap.values) {
            WirelessSignalTxElement.channelRemove(tx)
        }
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return NodeBase.maskElectricalAll
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad {
        return ioGate[side.int]!!
    }

    override val nodeUuid: String
        get() = getNodeUuidStatic()

    fun signalSetDir(side: Direction, highImpedance: Boolean): Array<Any?>? {
        ioGateProcess[side.int]!!.isHighImpedance = highImpedance
        Utils.println(ioGateProcess[side.int]!!.isHighImpedance)
        return null
    }

    fun signalGetDir(side: Direction): Array<Any?> {
        return arrayOf(if (ioGateProcess[side.int]!!.isHighImpedance) "in" else "out")
    }

    fun signalSetOut(side: Direction, value: Double): Array<Any?>? {
        ioGateProcess[side.int]!!.outputNormalized = value
        return null
    }

    fun signalGetOut(side: Direction): Array<Any?> {
        return arrayOf(ioGateProcess[side.int]!!.outputNormalized)
    }

    fun signalGetIn(side: Direction): Array<Any?> {
        return arrayOf(ioGate[side.int]!!.inputNormalized)
    }

    fun wirelessSet(channel: String, value: Double): Array<Any?>? {
        var tx = wirelessTxMap[channel]
        if (tx == null) {
            tx = WirelessTx()
            tx.channelName = channel
            WirelessSignalTxElement.channelRegister(tx)
            wirelessTxMap[channel] = tx
        }

        tx.signalValue = value
        return null
    }

    fun wirelessRemove(channel: String): Array<Any?>? {
        val tx = wirelessTxMap[channel]
        if (tx != null) {
            WirelessSignalTxElement.channelRemove(tx)
            wirelessTxMap.remove(channel)
        }
        return null
    }

    fun wirelessRemoveAll(): Array<Any?>? {
        for (tx in wirelessTxMap.values) {
            WirelessSignalTxElement.channelRemove(tx)
        }
        wirelessTxMap.clear()
        return null
    }

    fun wirelessGet(channel: String, aggregation: String): Array<Any?> {
        val aggregator = aggregatorFor(aggregation)
            ?: return softError("unknown aggregation '$aggregation'; expected bigger or smaller")
        val value = wirelessRead(channel, aggregator)
            ?: return softError("channel not available: $channel")
        return arrayOf(value)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun signalSetDir(context: Context?, args: Arguments?): Array<Any?>? {
        if (args == null || args.count() < 2) return softError("expected side and direction")
        val sideName = args.checkString(0)
        val side = directionFor(sideName) ?: return softError("unknown side: $sideName")
        val direction = args.checkString(1)
        if (direction != "in" && direction != "out") return softError("unknown direction '$direction'; expected in or out")
        val highImpedance = direction == "in"
        return signalSetDir(side, highImpedance)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun signalGetDir(context: Context?, args: Arguments?): Array<Any?> {
        if (args == null || args.count() < 1) return softError("expected side")
        val sideName = args.checkString(0)
        val side = directionFor(sideName) ?: return softError("unknown side: $sideName")
        return signalGetDir(side)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun signalSetOut(context: Context?, args: Arguments?): Array<Any?>? {
        if (args == null || args.count() < 2) return softError("expected side and value")
        val sideName = args.checkString(0)
        val side = directionFor(sideName) ?: return softError("unknown side: $sideName")
        val value = args.checkDouble(1)
        return signalSetOut(side, value)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun signalGetOut(context: Context?, args: Arguments?): Array<Any?> {
        if (args == null || args.count() < 1) return softError("expected side")
        val sideName = args.checkString(0)
        val side = directionFor(sideName) ?: return softError("unknown side: $sideName")
        return signalGetOut(side)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun signalGetIn(context: Context?, args: Arguments?): Array<Any?> {
        if (args == null || args.count() < 1) return softError("expected side")
        val sideName = args.checkString(0)
        val side = directionFor(sideName) ?: return softError("unknown side: $sideName")
        return signalGetIn(side)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun wirelessSet(context: Context?, args: Arguments?): Array<Any?>? {
        if (args == null || args.count() < 2) return softError("expected channel and value")
        val channel = args.checkString(0)
        val value = args.checkDouble(1)
        return wirelessSet(channel, value)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun wirelessRemove(context: Context?, args: Arguments?): Array<Any?>? {
        if (args == null || args.count() < 1) return softError("expected channel")
        val channel = args.checkString(0)
        return wirelessRemove(channel)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun wirelessRemoveAll(context: Context?, args: Arguments?): Array<Any?>? {
        return wirelessRemoveAll()
    }

    @Optional.Method(modid = Other.modIdOc)
    fun wirelessGet(context: Context?, args: Arguments?): Array<Any?> {
        if (args == null || args.count() < 1) return softError("expected channel")
        val channel = args.checkString(0)
        var aggregation = "bigger"
        if (args.count() == 2) aggregation = args.checkString(1)

        return wirelessGet(channel, aggregation)
    }

    @Optional.Method(modid = Other.modIdOc)
    fun version(context: Context?, args: Arguments?): Array<Any?> {
        return arrayOf(Version.simpleVersionName)
    }

    @Optional.Method(modid = Other.modIdCc)
    override fun getType(): String {
        return "ElnProbe"
    }

    private val functionNames = arrayOf(
        "signalSetDir",
        "signalGetDir",
        "signalSetOut",
        "signalGetOut",
        "signalGetIn",
        "wirelessSet",
        "wirelessRemove",
        "wirelessRemoveAll",
        "wirelessGet",
        "version"
    )

    @Optional.Method(modid = Other.modIdCc)
    override fun getMethodNames(): Array<String> {
        return functionNames
    }

    @Optional.Method(modid = Other.modIdCc)
    @Throws(LuaException::class, InterruptedException::class)
    override fun callMethod(
        computer: IComputerAccess,
        context: ILuaContext,
        method: Int,
        args: Array<Any>
    ): Array<Any?>? {
        try {
            if (method < 0 || method >= functionNames.size) return null
            when (method) {
                0 -> return signalSetDir(Direction.valueOf(args[0] as String), args[1] == "in")
                1 -> return signalGetDir(Direction.valueOf(args[0] as String))
                2 -> return signalSetOut(Direction.valueOf(args[0] as String), args[1] as Double)
                3 -> return signalGetOut(Direction.valueOf(args[0] as String))
                4 -> return signalGetIn(Direction.valueOf(args[0] as String))
                5 -> return wirelessSet(args[0] as String, args[1] as Double)
                6 -> return wirelessRemove(args[0] as String)
                7 -> return wirelessRemoveAll()
                8 -> {
                    var aggregation = "bigger"
                    if (args.size == 2) aggregation = args[1] as String
                    return wirelessGet(args[0] as String, aggregation)
                }
                9 -> return arrayOf(Version.simpleVersionName)
            }
        } catch (e: Exception) {
        }
        return null
    }

    @Optional.Method(modid = Other.modIdCc)
    override fun attach(computer: IComputerAccess) {
        Utils.println("CC attache")
    }

    @Optional.Method(modid = Other.modIdCc)
    override fun detach(computer: IComputerAccess) {
        Utils.println("CC detach")
    }

    @Optional.Method(modid = Other.modIdCc)
    override fun equals(other: IPeripheral): Boolean {
        return this === other
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        super.writeToNBT(nbt)
        nbt.setInteger("wirelessTxCount", wirelessTxMap.size)
        var idx = 0
        for (tx in wirelessTxMap.values) {
            nbt.setString("wirelessTx" + idx + "channel", tx.channelName)
            nbt.setDouble("wirelessTx" + idx + "value", tx.signalValue)
            idx++
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        super.readFromNBT(nbt)
        val wirelessTxCount = nbt.getInteger("wirelessTxCount")
        for (idx in 0 until wirelessTxCount) {
            val tx = WirelessTx()
            tx.channelName = nbt.getString("wirelessTx" + idx + "channel")
            tx.signalValue = nbt.getDouble("wirelessTx" + idx + "value")
            WirelessSignalTxElement.channelRegister(tx)
            wirelessTxMap[tx.channelName] = tx
        }
    }

    private inner class WirelessTx : IWirelessSignalTx {
        lateinit var channelName: String
        var signalValue = 0.0

        override fun getCoordinate(): Coordinate {
            return coordinate
        }

        override fun getRange(): Int {
            return Eln.config.getIntOrElse("wireless.transmitter.maxRangeBlocks", 32)
        }

        override fun getChannel(): String {
            return channelName
        }

        override fun getValue(): Double {
            return signalValue
        }
    }

    companion object {
        @JvmStatic
        fun getNodeUuidStatic(): String {
            return "ElnComputerProbe"
        }
    }
}
