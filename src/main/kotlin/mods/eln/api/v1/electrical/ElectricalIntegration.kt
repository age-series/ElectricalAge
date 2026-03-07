@file:Suppress("unused")

package mods.eln.api.v1.electrical

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.Utils
import mods.eln.misc.LRDU
import mods.eln.node.GhostNode
import mods.eln.node.NodeManager
import mods.eln.node.NodeBase
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad as SimElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.Simulator
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource as InternalVoltageSource
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.IDestructible
import mods.eln.sim.process.destruct.ResistorPowerWatchdog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.util.Locale

/**
 * Stable versioned electrical integration API exposed for external mods.
 */
object ElectricalIntegration {
    private const val loadRefKindLocal = 0
    private const val loadRefKindNode = 1

    /**
     * API-owned block face enum for node lookup and connection.
     */
    enum class BlockFace(val id: Int) {
        XN(0),
        XP(1),
        YN(2),
        YP(3),
        ZN(4),
        ZP(5);

        internal fun toInternal(): Direction {
            return when (this) {
                XN -> Direction.XN
                XP -> Direction.XP
                YN -> Direction.YN
                YP -> Direction.YP
                ZN -> Direction.ZN
                ZP -> Direction.ZP
            }
        }

        companion object {
            @JvmStatic
            fun fromId(id: Int): BlockFace {
                return values().firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Unknown BlockFace id: $id")
            }

            internal fun fromInternal(direction: Direction): BlockFace {
                return fromId(direction.int)
            }
        }
    }

    /**
     * Placement-relative block faces for side-aware block node registration.
     */
    enum class RelativeBlockFace {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        UP,
        DOWN;

        fun resolve(front: BlockFace): BlockFace {
            return BlockFace.fromInternal(
                when (this) {
                    FRONT -> front.toInternal()
                    BACK -> front.toInternal().back()
                    LEFT -> front.toInternal().left()
                    RIGHT -> front.toInternal().right()
                    UP -> front.toInternal().up()
                    DOWN -> front.toInternal().down()
                }
            )
        }
    }

    /**
     * API-owned node-port enum for node lookup and connection.
     */
    enum class NodePort(val id: Int) {
        LEFT(0),
        RIGHT(1),
        DOWN(2),
        UP(3);

        internal fun toInternal(): LRDU {
            return when (this) {
                LEFT -> LRDU.Left
                RIGHT -> LRDU.Right
                DOWN -> LRDU.Down
                UP -> LRDU.Up
            }
        }

        companion object {
            @JvmStatic
            fun fromId(id: Int): NodePort {
                return values().firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Unknown NodePort id: $id")
            }

            internal fun fromInternal(lrdu: LRDU): NodePort {
                return fromId(lrdu.toInt())
            }
        }
    }

    /**
     * API-owned electrical connection masks.
     */
    object ElectricalMasks {
        const val POWER = 1 shl 0
        const val GATE = 1 shl 2
        const val SIGNAL_BUS = GATE
        const val ALL = POWER or GATE
    }

    enum class SignalBusChannel(val id: Int) {
        WHITE(0),
        ORANGE(1),
        MAGENTA(2),
        LIGHT_BLUE(3),
        YELLOW(4),
        LIME(5),
        PINK(6),
        GRAY(7),
        LIGHT_GRAY(8),
        CYAN(9),
        PURPLE(10),
        BLUE(11),
        BROWN(12),
        GREEN(13),
        RED(14),
        BLACK(15);

        companion object {
            @JvmStatic
            fun fromId(id: Int): SignalBusChannel {
                return values().firstOrNull { it.id == id }
                    ?: throw IllegalArgumentException("Unknown SignalBusChannel id: $id")
            }

            @JvmStatic
            fun ordered(): List<SignalBusChannel> = values().asList()
        }
    }

    object SignalBusMasks {
        const val BUS: Int = NodeBase.maskElectricalGate
        const val COLOR_SHIFT: Int = NodeBase.maskColorShift
        const val COLOR_CARE_SHIFT: Int = NodeBase.maskColorCareShift

        @JvmStatic
        fun channelMask(channel: SignalBusChannel): Int {
            return BUS or (channel.id shl COLOR_SHIFT)
        }

        @JvmStatic
        fun wildcardMask(): Int {
            return BUS or (1 shl COLOR_CARE_SHIFT)
        }

        @JvmStatic
        fun readChannel(mask: Int): SignalBusChannel {
            return SignalBusChannel.fromId((mask shr COLOR_SHIFT) and 0xF)
        }
    }

    /**
     * Public signal-voltage helpers for integrations that work with normalized signal values.
     */
    object SignalLevels {
        val MAX_VOLTAGE: Double
            get() = Eln.SVU

        fun clampVoltage(voltage: Double): Double {
            if (voltage.isNaN()) return 0.0
            return Utils.limit(voltage, 0.0, MAX_VOLTAGE)
        }

        fun clampNormalized(value: Double): Double {
            if (value.isNaN()) return 0.0
            return Utils.limit(value, 0.0, 1.0)
        }

        fun toVoltage(normalizedValue: Double): Double {
            return clampNormalized(normalizedValue) * MAX_VOLTAGE
        }

        fun toNormalized(voltage: Double): Double {
            return clampVoltage(voltage) / MAX_VOLTAGE
        }
    }

    @JvmStatic
    fun getSignalVoltageLevel(): Double = SignalLevels.MAX_VOLTAGE

    /**
     * True when ELN debug logging/features are enabled.
     */
    @JvmStatic
    fun isDebugEnabled(): Boolean = Eln.debugEnabled

    /**
     * True when ELN easy Waila mode is enabled.
     */
    @JvmStatic
    fun isEasyWailaModeEnabled(): Boolean = Eln.wailaEasyMode

    /**
     * Lightweight telemetry handle for an electrical load.
     */
    interface ElectricalLoad {
        /**
         * Current node voltage in volts.
         */
        val voltage: Double

        /**
         * Electrical current in amperes.
         */
        val current: Double

        /**
         * Electrical power in watts.
         */
        val power: Double

        /**
         * Build a volt-meter style voltage string.
         */
        fun formatVoltage(prefix: String = ""): String {
            return Utils.plotVolt(prefix, voltage)
        }

        /**
         * Build a volt-meter style current string.
         */
        fun formatCurrent(prefix: String = ""): String {
            return Utils.plotAmpere(prefix, current)
        }

        /**
         * Build a volt-meter style power string.
         */
        fun formatPower(prefix: String = ""): String {
            return Utils.plotPower(prefix, power)
        }

        /**
         * Build a combined voltage+current volt-meter string.
         */
        fun formatVoltageCurrent(voltagePrefix: String = "U:", currentPrefix: String = "I:"): String {
            return Utils.plotVolt(voltagePrefix, voltage) + Utils.plotAmpere(currentPrefix, current)
        }

        /**
         * Build a combined voltage+current+power volt-meter string.
         */
        fun formatVoltageCurrentPower(
            voltagePrefix: String = "U:",
            currentPrefix: String = "I:",
            powerPrefix: String = "P:"
        ): String {
            return formatVoltageCurrent(voltagePrefix, currentPrefix) + Utils.plotPower(powerPrefix, power)
        }
    }

    interface SignalElectricalLoad : ElectricalLoad {
        val normalized: Double
            get() = SignalLevels.toNormalized(voltage)

        val signalVoltage: Double
            get() = SignalLevels.clampVoltage(voltage)

        val stateHigh: Boolean
            get() = signalVoltage > SignalLevels.MAX_VOLTAGE * 0.6

        val stateLow: Boolean
            get() = signalVoltage < SignalLevels.MAX_VOLTAGE * 0.2

        fun formatSignal(prefix: String = ""): String {
            return Utils.plotSignal(signalVoltage).let { if (prefix.isEmpty()) it else prefix + it }
        }
    }

    /**
     * A resistive electrical element that can be coupled to an ElectricalLoad.
     */
    interface ResistiveLoad : ElectricalLoad {
        /**
         * Set the electrical resistance in ohms.
         */
        fun setResistance(resistance: Double)

        /**
         * Put the resistor in high impedance mode.
         */
        fun highImpedance()

        /**
         * Put the resistor in pull-down mode.
         */
        fun pullDown()
    }

    /**
     * Callback invoked when a watchdog trips and destruction is handled.
     */
    interface WatchdogDestroyAction {
        fun onDestroy()

        fun describe(): String? = null
    }

    /**
     * Wrapper around ELN slow-process watchdog classes.
     */
    interface Watchdog : IProcess {
        fun setLimits(minimumVoltageOrPower: Double, maximumVoltageOrPower: Double): Watchdog

        fun setTimeoutReset(timeoutSeconds: Double): Watchdog

        fun setDestroyAction(action: WatchdogDestroyAction?): Watchdog

        fun reset()
    }

    /**
     * A stable handle around an NBT-backed electrical load.
     */
    open class Load @JvmOverloads internal constructor(
        val name: String,
        configure: NbtElectricalLoad.() -> Unit = {
            Eln.applySmallRs(this)
        }
    ) : ElectricalLoad {
        internal val delegate = NbtElectricalLoad(name).apply {
            configure()
            setAsPrivate()
        }

        override val voltage get() = delegate.voltage
        override val current get() = delegate.current
        override val power get() = voltage * current

        /**
         * Persist this load state to NBT.
         */
        @JvmOverloads
        fun writeNbt(tag: NBTTagCompound, prefix: String = name) {
            delegate.writeToNBT(tag, prefix)
        }

        fun writeToNbt(tag: NBTTagCompound, prefix: String = name) {
            writeNbt(tag, prefix)
        }

        /**
         * Restore this load state from NBT.
         */
        @JvmOverloads
        fun readNbt(tag: NBTTagCompound, prefix: String = name) {
            delegate.readFromNBT(tag, prefix)
        }

        fun readFromNbt(tag: NBTTagCompound, prefix: String = name) {
            readNbt(tag, prefix)
        }
    }

    /**
     * Signal-domain load with normalized and threshold helpers.
     */
    open class SignalLoad(name: String) : Load(name, {
        Eln.instance.signalCableDescriptor.applyTo(this)
    }), SignalElectricalLoad

    class SignalBusChannelLoad internal constructor(
        name: String,
        configure: NbtElectricalLoad.() -> Unit = {
            Eln.instance.signalBusCableDescriptor.applyTo(this)
        }
    ) : Load(name, configure), SignalElectricalLoad

    class SignalBusLoad internal constructor(
        name: String,
        private val channelConfigure: NbtElectricalLoad.() -> Unit = {
            Eln.instance.signalBusCableDescriptor.applyTo(this)
        }
    ) {
        val name: String = name
        private val channelLoads = SignalBusChannel.values().associateWith { channel ->
            SignalBusChannelLoad("$name.${channel.name.lowercase(Locale.ROOT)}", channelConfigure)
        }

        fun getChannel(channel: SignalBusChannel): SignalBusChannelLoad = channelLoads.getValue(channel)

        operator fun get(channel: SignalBusChannel): SignalBusChannelLoad = getChannel(channel)

        fun channels(): Map<SignalBusChannel, SignalBusChannelLoad> = channelLoads.toMap()

        fun signalVoltage(channel: SignalBusChannel): Double = getChannel(channel).signalVoltage

        fun normalized(channel: SignalBusChannel): Double = getChannel(channel).normalized

        @JvmOverloads
        fun writeNbt(tag: NBTTagCompound, prefix: String = name) {
            SignalBusChannel.values().forEach { channel ->
                getChannel(channel).writeNbt(tag, "$prefix.${channel.name.lowercase(Locale.ROOT)}")
            }
        }

        fun writeToNbt(tag: NBTTagCompound, prefix: String = name) {
            writeNbt(tag, prefix)
        }

        @JvmOverloads
        fun readNbt(tag: NBTTagCompound, prefix: String = name) {
            SignalBusChannel.values().forEach { channel ->
                getChannel(channel).readNbt(tag, "$prefix.${channel.name.lowercase(Locale.ROOT)}")
            }
        }

        fun readFromNbt(tag: NBTTagCompound, prefix: String = name) {
            readNbt(tag, prefix)
        }
    }

    /**
     * A managed signal output source using ELN's gate-output behavior.
     */
    class SignalSource @JvmOverloads constructor(
        val load: SignalLoad,
        initialVoltage: Double = 0.0
    ) : ElectricalLoad {
        internal val delegate = NbtElectricalGateOutputProcess("signalSource", load.delegate).apply {
            setVoltageSafe(initialVoltage)
        }

        override val voltage: Double
            get() = load.voltage

        override val current: Double
            get() = load.current

        override val power: Double
            get() = load.power

        val outputVoltage: Double
            get() = delegate.voltage

        val outputNormalized: Double
            get() = delegate.outputNormalized

        val isHighImpedance: Boolean
            get() = delegate.isHighImpedance

        fun setVoltage(voltage: Double): SignalSource {
            delegate.setVoltageSafe(voltage)
            return this
        }

        fun setNormalized(normalizedValue: Double): SignalSource {
            delegate.setOutputNormalizedSafe(normalizedValue)
            return this
        }

        fun setState(enabled: Boolean): SignalSource {
            delegate.state(enabled)
            return this
        }

        fun setHighImpedance(enabled: Boolean): SignalSource {
            delegate.setHighImpedance(enabled)
            return this
        }
    }

    class SignalBusSource(val load: SignalBusLoad) {
        private val delegates = SignalBusChannel.values().associateWith { channel ->
            NbtElectricalGateOutputProcess("signalBusSource${channel.id}", load[channel].delegate).apply {
                setHighImpedance(true)
            }
        }

        fun setVoltage(channel: SignalBusChannel, voltage: Double): SignalBusSource {
            delegates.getValue(channel).setVoltageSafe(voltage)
            return this
        }

        fun setNormalized(channel: SignalBusChannel, normalizedValue: Double): SignalBusSource {
            delegates.getValue(channel).setOutputNormalizedSafe(normalizedValue)
            return this
        }

        fun setState(channel: SignalBusChannel, enabled: Boolean): SignalBusSource {
            delegates.getValue(channel).state(enabled)
            return this
        }

        fun setHighImpedance(channel: SignalBusChannel, enabled: Boolean): SignalBusSource {
            delegates.getValue(channel).setHighImpedance(enabled)
            return this
        }

        fun outputVoltage(channel: SignalBusChannel): Double = delegates.getValue(channel).voltage

        fun outputNormalized(channel: SignalBusChannel): Double = delegates.getValue(channel).outputNormalized

        fun isHighImpedance(channel: SignalBusChannel): Boolean = delegates.getValue(channel).isHighImpedance

        internal fun delegates(): Collection<NbtElectricalGateOutputProcess> = delegates.values
    }

    /**
     * A managed virtual ELN node hosted at an external mod block position.
     *
     * Registering this node lets adjacent ELN cables connect to and render against
     * the supplied shared load without requiring the block itself to inherit ELN node types.
     */
    abstract class BlockFaceConnection {
        abstract val representativeLoad: Load
        abstract val combinedMask: Int
        internal abstract fun toInternalConnection(): InternalHostedFaceConnection
    }

    class BlockNode internal constructor(
        val dimension: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val load: Load,
        val front: BlockFace,
        val faceConnections: Map<BlockFace, BlockFaceConnection>
    ) {
        val mask: Int
            get() = faceConnections.values.fold(0) { acc, connection -> acc or connection.combinedMask }

        @JvmOverloads
        constructor(
            dimension: Int,
            x: Int,
            y: Int,
            z: Int,
            load: Load,
            mask: Int = ElectricalMasks.POWER
        ) : this(
            dimension,
            x,
            y,
            z,
            load,
            BlockFace.XN,
            defaultFaceConnections(load, mask)
        )

        @JvmOverloads
        constructor(
            dimension: Int,
            x: Int,
            y: Int,
            z: Int,
            front: BlockFace = BlockFace.XN,
            faceConnections: Map<BlockFace, FaceConnection>
        ) : this(
            dimension,
            x,
            y,
            z,
            faceConnections.values.firstOrNull()?.representativeLoad
                ?: throw IllegalArgumentException("BlockNode faceConnections must not be empty"),
            front,
            faceConnections.toMap()
        )

        @JvmOverloads
        constructor(
            dimension: Int,
            x: Int,
            y: Int,
            z: Int,
            load: Load,
            front: BlockFace,
            mask: Int = ElectricalMasks.POWER,
            vararg connectedFaces: RelativeBlockFace
        ) : this(
            dimension,
            x,
            y,
            z,
            load,
            front,
            relativeFaceConnections(load, front, mask, connectedFaces)
        )

        internal val delegate = ManagedGhostLoadNode(
            Coordinate(x, y, z, dimension),
            front.toInternal(),
            faceConnections.mapKeys { (face, _) -> face.toInternal() }
                .mapValues { (_, connection) -> connection.toInternalConnection() }
        )

        companion object {
            private fun defaultFaceConnections(load: Load, mask: Int): Map<BlockFace, BlockFaceConnection> {
                return BlockFace.values().associateWith { FaceConnection(load, mask) }
            }

            private fun relativeFaceConnections(
                load: Load,
                front: BlockFace,
                mask: Int,
                connectedFaces: Array<out RelativeBlockFace>
            ): Map<BlockFace, BlockFaceConnection> {
                val selectedFaces = if (connectedFaces.isEmpty()) RelativeBlockFace.values().asList() else connectedFaces.toList()
                return selectedFaces.associate { it.resolve(front) to FaceConnection(load, mask) }
            }
        }
    }

    /**
     * Per-face connection definition for a block-hosted virtual ELN node.
     */
    class FaceConnection @JvmOverloads constructor(
        val load: Load,
        val mask: Int = ElectricalMasks.POWER
    ) : BlockFaceConnection() {
        init {
            require(mask != 0) { "FaceConnection mask must not be zero" }
        }

        override val representativeLoad: Load
            get() = load

        override val combinedMask: Int
            get() = mask

        override fun toInternalConnection(): InternalHostedFaceConnection {
            return InternalSingleFaceConnection(load.delegate, mask)
        }
    }

    class SignalBusFaceConnection(val bus: SignalBusLoad) : BlockFaceConnection() {
        override val representativeLoad: Load
            get() = bus[SignalBusChannel.WHITE]

        override val combinedMask: Int
            get() = SignalBusMasks.wildcardMask()

        override fun toInternalConnection(): InternalHostedFaceConnection {
            return InternalSignalBusFaceConnection(SignalBusChannel.values().associateWith { bus[it].delegate })
        }
    }

    /**
     * A convenience handle for the common external-block pattern:
     * one shared load, one grounded sink resistor, and one block-hosted ELN node.
     */
    class GroundedResistorSink internal constructor(
        val load: Load,
        val resistor: ResistorLoad,
        val blockNode: BlockNode
    ) : ResistiveLoad {
        override val voltage get() = resistor.voltage
        override val current get() = resistor.current
        override val power get() = resistor.power

        override fun setResistance(resistance: Double) {
            resistor.setResistance(resistance)
        }

        override fun highImpedance() {
            resistor.highImpedance()
        }

        override fun pullDown() {
            resistor.pullDown()
        }

        fun register() {
            registerGroundedResistorSink(this)
        }

        fun unregister() {
            unregisterGroundedResistorSink(this)
        }
    }

    /**
     * A stable handle for an existing electrical port exposed by an ELN node.
     */
    class NodeLoad internal constructor(
        val dimension: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val side: BlockFace,
        val port: NodePort,
        val mask: Int,
        internal val delegate: SimElectricalLoad
    ) : ElectricalLoad {
        override val voltage get() = delegate.voltage
        override val current get() = delegate.current
        override val power get() = voltage * current

        fun toRef(): NodeLoadRef {
            return NodeLoadRef(dimension, x, y, z, side, port, mask)
        }
    }

    /**
     * Serializable reference for a node load.
     */
    class NodeLoadRef(
        val dimension: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val side: BlockFace,
        val port: NodePort,
        val mask: Int
    ) {
        /**
         * Resolve this reference to a live node load from current world state.
         */
        fun resolve(): NodeLoad {
            return resolveNodeLoad(this)
        }

        /**
         * Resolve this reference, returning null when the referenced node cannot be resolved.
         */
        fun resolveOrNull(): NodeLoad? {
            return try {
                resolve()
            } catch (ex: Exception) {
                null
            }
        }

        /**
         * Persist this node endpoint reference to NBT.
         */
        fun writeNbt(tag: NBTTagCompound, key: String = "nodeLoad") {
            tag.setInteger("$key.dimension", dimension)
            tag.setInteger("$key.x", x)
            tag.setInteger("$key.y", y)
            tag.setInteger("$key.z", z)
            tag.setByte("$key.side", side.id.toByte())
            tag.setByte("$key.port", port.id.toByte())
            tag.setInteger("$key.mask", mask)
        }

        /**
         * Persist this node endpoint reference to NBT.
         */
        fun writeToNbt(tag: NBTTagCompound, key: String = "nodeLoad") {
            writeNbt(tag, key)
        }

        companion object {
            /**
             * Read a node endpoint reference from NBT.
             */
            @JvmOverloads
            fun readNbt(tag: NBTTagCompound, key: String = "nodeLoad"): NodeLoadRef {
                val (dimension, x, y, z) = if (tag.hasKey("$key.dimension")) {
                    listOf(
                        tag.getInteger("$key.dimension"),
                        tag.getInteger("$key.x"),
                        tag.getInteger("$key.y"),
                        tag.getInteger("$key.z")
                    )
                } else {
                    val coordinate = Coordinate().apply { readFromNBT(tag, key + "Coord") }
                    listOf(coordinate.dimension, coordinate.x, coordinate.y, coordinate.z)
                }
                val side = if (tag.hasKey("$key.side")) {
                    BlockFace.fromId(tag.getByte("$key.side").toInt())
                } else {
                    throw IllegalStateException("Missing '$key.side' when reading NodeLoadRef")
                }
                val port = if (tag.hasKey("$key.port")) {
                    NodePort.fromId(tag.getByte("$key.port").toInt())
                } else if (tag.hasKey("$key.lrdu")) {
                    NodePort.fromId(tag.getByte("$key.lrdu").toInt())
                } else {
                    throw IllegalStateException("Missing '$key.port' when reading NodeLoadRef")
                }
                val mask = if (tag.hasKey("$key.mask")) tag.getInteger("$key.mask") else ElectricalMasks.ALL
                return NodeLoadRef(dimension, x, y, z, side, port, mask)
            }

            /**
             * Read a node endpoint reference from NBT.
             */
            @JvmOverloads
            fun readFromNbt(tag: NBTTagCompound, key: String = "nodeLoad"): NodeLoadRef {
                return readNbt(tag, key)
            }
        }
    }

    /**
     * Serializable reference for an API electrical endpoint.
     */
    class ElectricalLoadRef(
        val kind: Int,
        val loadName: String?,
        val nodeLoadRef: NodeLoadRef?
    ) {
        /**
         * Serialize this endpoint reference to NBT.
         */
        fun writeNbt(tag: NBTTagCompound, key: String = "endpoint") {
            tag.setInteger("$key.kind", kind)
            when (kind) {
                loadRefKindLocal -> tag.setString("$key.loadName", loadName)
                loadRefKindNode -> nodeLoadRef!!.writeNbt(tag, "$key.node")
            }
        }

        /**
         * Serialize this endpoint reference to NBT.
         */
        fun writeToNbt(tag: NBTTagCompound, key: String = "endpoint") {
            writeNbt(tag, key)
        }

        companion object {
            /**
             * Read a serialized endpoint reference from NBT.
             */
            @JvmOverloads
            fun readNbt(tag: NBTTagCompound, key: String = "endpoint"): ElectricalLoadRef {
                val kind = tag.getInteger("$key.kind")
                return when (kind) {
                    loadRefKindLocal -> ElectricalLoadRef(kind, tag.getString("$key.loadName"), null)
                    loadRefKindNode -> ElectricalLoadRef(kind, null, NodeLoadRef.readNbt(tag, "$key.node"))
                    else -> throw IllegalArgumentException("Unknown ElectricalLoadRef kind: $kind")
                }
            }

            /**
             * Read a serialized endpoint reference from NBT.
             */
            @JvmOverloads
            fun readFromNbt(tag: NBTTagCompound, key: String = "endpoint"): ElectricalLoadRef {
                return readNbt(tag, key)
            }
        }
    }

    /**
     * Serializable snapshot for a direct electrical connection.
     */
    class NodeConnectionState(
        val endpointA: ElectricalLoadRef,
        val endpointB: ElectricalLoadRef
    ) {
        /**
         * Serialize this persisted connection snapshot to NBT.
         */
        fun writeNbt(tag: NBTTagCompound, key: String = "connection") {
            endpointA.writeNbt(tag, "$key.a")
            endpointB.writeNbt(tag, "$key.b")
        }

        /**
         * Serialize this persisted connection snapshot to NBT.
         */
        fun writeToNbt(tag: NBTTagCompound, key: String = "connection") {
            writeNbt(tag, key)
        }

        companion object {
            /**
             * Read a persisted connection snapshot from NBT.
             */
            @JvmOverloads
            fun readNbt(tag: NBTTagCompound, key: String = "connection"): NodeConnectionState {
                return NodeConnectionState(
                    ElectricalLoadRef.readNbt(tag, "$key.a"),
                    ElectricalLoadRef.readNbt(tag, "$key.b")
                )
            }

            /**
             * Read a persisted connection snapshot from NBT.
             */
            @JvmOverloads
            fun readFromNbt(tag: NBTTagCompound, key: String = "connection"): NodeConnectionState {
                return readNbt(tag, key)
            }
        }
    }

    /**
     * Helper used by persistence restoration to map stored load IDs back to API loads.
     */
    interface LoadLookup {
        /**
         * Resolve a local load by stable API identifier.
         */
        fun getLoad(name: String): Load?
    }

    /**
     * Restore a persisted connection using a lookup for local load names.
     */
    fun resolveConnection(
        state: NodeConnectionState,
        loadLookup: LoadLookup
    ): NodeConnectionStateResolution {
        return NodeConnectionStateResolution(
            resolveLoadRef(state.endpointA, loadLookup),
            resolveLoadRef(state.endpointB, loadLookup)
        )
    }

    /**
     * Holder for connection endpoints while restoring persisted state.
     */
    class NodeConnectionStateResolution(
        val endpointA: ElectricalLoad?,
        val endpointB: ElectricalLoad?
    ) {
        /**
         * Returns true when both endpoints are resolved.
         */
        fun isComplete(): Boolean {
            return endpointA != null && endpointB != null
        }

        /**
         * Build and return a live connection when both ends are present.
         */
        fun createConnectionOrNull(): NodeConnection? {
            return if (isComplete()) {
                NodeConnection(endpointA!!, endpointB!!, ElectricalConnection(unwrap(endpointA!!), unwrap(endpointB!!)))
            } else {
                null
            }
        }
    }

    /**
     * A stable handle around an ELN resistor between two ports.
     */
    class ResistorLoad(
        val portA: Load,
        val portB: Load?,
        internal val delegate: Resistor
    ) : ResistiveLoad {
        constructor(portA: Load, portB: Load?, resistance: Double) : this(
            portA,
            portB,
            Resistor(portA.delegate, portB?.delegate).apply { setResistance(resistance) }
        )

        constructor(portA: Load, portB: Load?) : this(portA, portB, Resistor(portA.delegate, portB?.delegate))

        override val voltage get() = delegate.voltage
        override val current get() = delegate.current
        override val power get() = delegate.power

        override fun setResistance(resistance: Double) {
            delegate.setResistance(resistance)
        }

        override fun highImpedance() {
            delegate.highImpedance()
        }

        override fun pullDown() {
            delegate.pullDown()
        }

        fun getResistance(): Double {
            return delegate.resistance
        }

        @JvmOverloads
        fun writeNbt(tag: NBTTagCompound, key: String = "resistor") {
            tag.setDouble("$key.resistance", delegate.resistance)
        }

        /**
         * Serialize this resistor state to NBT.
         */
        fun writeToNbt(tag: NBTTagCompound, key: String = "resistor") {
            writeNbt(tag, key)
        }

        /**
         * Restore resistor state from NBT.
         */
        @JvmOverloads
        fun readNbt(tag: NBTTagCompound, key: String = "resistor") {
            delegate.setResistance(tag.getDouble("$key.resistance"))
        }

        /**
         * Restore resistor state from NBT.
         */
        fun readFromNbt(tag: NBTTagCompound, key: String = "resistor") {
            readNbt(tag, key)
        }
    }

    /**
     * A stable handle around a resistor connected between a mod load and an existing ELN node port.
     */
    class NodeConnectedResistorLoad(
        val portA: Load,
        val portB: NodeLoad,
        internal val delegate: Resistor
    ) : ResistiveLoad {
        constructor(portA: Load, portB: NodeLoad, resistance: Double) : this(portA, portB, Resistor(portA.delegate, portB.delegate).apply { setResistance(resistance) })

        constructor(portA: Load, portB: NodeLoad) : this(portA, portB, Resistor(portA.delegate, portB.delegate))

        override val voltage get() = delegate.voltage
        override val current get() = delegate.current
        override val power get() = delegate.power

        override fun setResistance(resistance: Double) {
            delegate.setResistance(resistance)
        }

        override fun highImpedance() {
            delegate.highImpedance()
        }

        override fun pullDown() {
            delegate.pullDown()
        }

        fun getResistance(): Double {
            return delegate.resistance
        }

        @JvmOverloads
        fun writeNbt(tag: NBTTagCompound, key: String = "nodeResistor") {
            tag.setDouble("$key.resistance", delegate.resistance)
        }

        /**
         * Serialize this node-side resistor state to NBT.
         */
        fun writeToNbt(tag: NBTTagCompound, key: String = "nodeResistor") {
            writeNbt(tag, key)
        }

        /**
         * Restore node-side resistor state from NBT.
         */
        @JvmOverloads
        fun readNbt(tag: NBTTagCompound, key: String = "nodeResistor") {
            delegate.setResistance(tag.getDouble("$key.resistance"))
        }

        /**
         * Restore node-side resistor state from NBT.
         */
        fun readFromNbt(tag: NBTTagCompound, key: String = "nodeResistor") {
            readNbt(tag, key)
        }
    }

    /**
     * A stable handle for a direct electrical connection between two existing ports.
     */
    class NodeConnection(
        val portA: ElectricalLoad,
        val portB: ElectricalLoad,
        internal val delegate: ElectricalConnection
    )

    /**
     * A stable handle around an ideal power-domain voltage source between two API-owned loads.
     */
    class VoltageSource @JvmOverloads constructor(
        val positiveLoad: Load,
        val negativeLoad: Load? = null,
        initialVoltage: Double = 0.0
    ) : ElectricalLoad {
        internal val delegate = InternalVoltageSource("apiVoltageSource", positiveLoad.delegate, negativeLoad?.delegate).apply {
            setVoltage(initialVoltage)
        }

        override val voltage: Double
            get() = delegate.voltage

        override val current: Double
            get() = delegate.current

        override val power: Double
            get() = delegate.power

        fun setVoltage(voltage: Double): VoltageSource {
            delegate.setVoltage(voltage)
            return this
        }

        @JvmOverloads
        fun writeNbt(tag: NBTTagCompound, key: String = "voltageSource") {
            tag.setDouble("$key.voltage", delegate.voltage)
        }

        fun writeToNbt(tag: NBTTagCompound, key: String = "voltageSource") {
            writeNbt(tag, key)
        }

        @JvmOverloads
        fun readNbt(tag: NBTTagCompound, key: String = "voltageSource") {
            delegate.setVoltage(tag.getDouble("$key.voltage"))
        }

        fun readFromNbt(tag: NBTTagCompound, key: String = "voltageSource") {
            readNbt(tag, key)
        }
    }

    /**
     * Voltage watchdog wrapper for any load-like port (typically NBT electrical loads).
     */
    class VoltageWatchdog internal constructor(internal val delegate: VoltageStateWatchDog) : Watchdog {
        override fun setLimits(minimumVoltageOrPower: Double, maximumVoltageOrPower: Double): Watchdog {
            delegate.min = minimumVoltageOrPower
            delegate.max = maximumVoltageOrPower
            return this
        }

        override fun setTimeoutReset(timeoutSeconds: Double): Watchdog {
            delegate.timeoutReset = timeoutSeconds
            return this
        }

        fun setNominalVoltage(nominalVoltage: Double): VoltageWatchdog {
            delegate.setNominalVoltage(nominalVoltage)
            return this
        }

        override fun setDestroyAction(action: WatchdogDestroyAction?): Watchdog {
            delegate.setDestroys(toDestructible(action))
            return this
        }

        override fun reset() {
            delegate.reset()
        }

        override fun process(time: Double) {
            delegate.process(time)
        }
    }

    /**
     * Power watchdog wrapper for resistive loads.
     */
    class PowerWatchdog internal constructor(internal val delegate: ResistorPowerWatchdog) : Watchdog {
        override fun setLimits(minimumVoltageOrPower: Double, maximumVoltageOrPower: Double): Watchdog {
            delegate.min = minimumVoltageOrPower
            delegate.max = maximumVoltageOrPower
            return this
        }

        override fun setTimeoutReset(timeoutSeconds: Double): Watchdog {
            delegate.timeoutReset = timeoutSeconds
            return this
        }

        fun setMaximumPower(maximumPower: Double): PowerWatchdog {
            delegate.setMaximumPower(maximumPower)
            return this
        }

        override fun setDestroyAction(action: WatchdogDestroyAction?): Watchdog {
            delegate.setDestroys(toDestructible(action))
            return this
        }

        override fun reset() {
            delegate.reset()
        }

        override fun process(time: Double) {
            delegate.process(time)
        }
    }

    /**
     * Register an electrical load with the global ELN simulator.
     */
    fun registerLoad(load: Load) {
        apiDebug(
            "registerLoad name={} voltage={} current={} power={}",
            load.name,
            load.voltage,
            load.current,
            load.power
        )
        simulator().addElectricalLoad(load.delegate)
    }

    /**
     * Remove an electrical load from the global ELN simulator.
     */
    fun unregisterLoad(load: Load) {
        apiDebug("unregisterLoad name={}", load.name)
        simulator().removeElectricalLoad(load.delegate)
    }

    /**
     * Resolve an electrical port from an existing ELN node and expose it as API load,
     * returning null when resolution fails.
     */
    @JvmOverloads
    fun resolveNodeLoadOrNull(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        side: BlockFace,
        port: NodePort,
        mask: Int = ElectricalMasks.ALL
    ): NodeLoad? {
        return try {
            resolveNodeLoad(dimension, x, y, z, side, port, mask)
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Resolve an electrical port from an API-owned node reference, returning null on failure.
     */
    fun resolveNodeLoadOrNull(nodeRef: NodeLoadRef): NodeLoad? {
        return try {
            resolveNodeLoad(nodeRef)
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Register a virtual ELN node hosted at an external block position.
     */
    fun registerBlockNode(node: BlockNode) {
        apiDebug(
            "registerBlockNode dimension={} x={} y={} z={} load={} mask={}",
            node.dimension,
            node.x,
            node.y,
            node.z,
            node.load.name,
            node.mask
        )
        node.delegate.initialize()
    }

    /**
     * Remove a previously registered virtual ELN node hosted at an external block position.
     */
    fun unregisterBlockNode(node: BlockNode) {
        apiDebug(
            "unregisterBlockNode dimension={} x={} y={} z={} load={} mask={}",
            node.dimension,
            node.x,
            node.y,
            node.z,
            node.load.name,
            node.mask
        )
        node.delegate.onBreakBlock()
    }

    /**
     * Create the common external-block topology of a shared load, grounded resistor sink,
     * and block-hosted ELN node.
     */
    @JvmOverloads
    fun createGroundedResistorSink(
        name: String,
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        resistance: Double,
        mask: Int = ElectricalMasks.POWER
    ): GroundedResistorSink {
        val load = Load(name)
        val resistor = ResistorLoad(load, null, resistance)
        val blockNode = BlockNode(dimension, x, y, z, load, mask)
        return GroundedResistorSink(load, resistor, blockNode)
    }

    /**
     * Create the common external-block topology of a shared load, grounded resistor sink,
     * and side-aware block-hosted ELN node.
     */
    @JvmOverloads
    fun createGroundedResistorSink(
        name: String,
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        resistance: Double,
        front: BlockFace,
        mask: Int = ElectricalMasks.POWER,
        vararg connectedFaces: RelativeBlockFace
    ): GroundedResistorSink {
        val load = Load(name)
        val resistor = ResistorLoad(load, null, resistance)
        val blockNode = BlockNode(dimension, x, y, z, load, front, mask, *connectedFaces)
        return GroundedResistorSink(load, resistor, blockNode)
    }

    /**
     * Register the common external-block topology of a shared load, grounded resistor sink,
     * and block-hosted ELN node.
     */
    fun registerGroundedResistorSink(sink: GroundedResistorSink) {
        registerLoad(sink.load)
        registerComponent(sink.resistor)
        registerBlockNode(sink.blockNode)
    }

    /**
     * Unregister the common external-block topology of a shared load, grounded resistor sink,
     * and block-hosted ELN node.
     */
    fun unregisterGroundedResistorSink(sink: GroundedResistorSink) {
        unregisterBlockNode(sink.blockNode)
        unregisterComponent(sink.resistor)
        unregisterLoad(sink.load)
    }

    /**
     * Create and register the common external-block topology of a shared load,
     * grounded resistor sink, and block-hosted ELN node.
     */
    @JvmOverloads
    fun createAndRegisterGroundedResistorSink(
        name: String,
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        resistance: Double,
        mask: Int = ElectricalMasks.POWER
    ): GroundedResistorSink {
        return createGroundedResistorSink(name, dimension, x, y, z, resistance, mask).also {
            registerGroundedResistorSink(it)
        }
    }

    /**
     * Create and register the common external-block topology of a shared load,
     * grounded resistor sink, and side-aware block-hosted ELN node.
     */
    @JvmOverloads
    fun createAndRegisterGroundedResistorSink(
        name: String,
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        resistance: Double,
        front: BlockFace,
        mask: Int = ElectricalMasks.POWER,
        vararg connectedFaces: RelativeBlockFace
    ): GroundedResistorSink {
        return createGroundedResistorSink(name, dimension, x, y, z, resistance, front, mask, *connectedFaces).also {
            registerGroundedResistorSink(it)
        }
    }

    /**
     * Create and register an API load with the global ELN simulator.
     */
    fun createAndRegisterLoad(name: String): Load {
        return Load(name).also(::registerLoad)
    }

    /**
     * Create and register a signal-domain API load with the global ELN simulator.
     */
    fun createAndRegisterSignalLoad(name: String): SignalLoad {
        return SignalLoad(name).also(::registerLoad)
    }

    fun registerLoad(load: SignalBusLoad) {
        load.channels().values.forEach(::registerLoad)
    }

    fun unregisterLoad(load: SignalBusLoad) {
        load.channels().values.forEach(::unregisterLoad)
    }

    fun createAndRegisterSignalBusLoad(name: String): SignalBusLoad {
        return SignalBusLoad(name).also(::registerLoad)
    }

    /**
     * Create a managed signal source for a signal-domain API load.
     */
    @JvmOverloads
    fun createSignalSource(load: SignalLoad, initialVoltage: Double = 0.0): SignalSource {
        return SignalSource(load, initialVoltage)
    }

    /**
     * Create and register a managed signal source plus its backing signal load.
     */
    @JvmOverloads
    fun createAndRegisterSignalSource(name: String, initialVoltage: Double = 0.0): SignalSource {
        val load = createAndRegisterSignalLoad(name)
        return createSignalSource(load, initialVoltage).also(::registerComponent)
    }

    fun createSignalBusSource(load: SignalBusLoad): SignalBusSource {
        return SignalBusSource(load)
    }

    fun createAndRegisterSignalBusSource(name: String): SignalBusSource {
        val load = createAndRegisterSignalBusLoad(name)
        return createSignalBusSource(load).also(::registerComponent)
    }

    /**
     * Create a power-domain voltage source between two API-owned loads.
     */
    @JvmOverloads
    fun createVoltageSource(
        positiveLoad: Load,
        negativeLoad: Load? = null,
        initialVoltage: Double = 0.0
    ): VoltageSource {
        return VoltageSource(positiveLoad, negativeLoad, initialVoltage)
    }

    /**
     * Create and register a power-domain voltage source between two API-owned loads.
     */
    @JvmOverloads
    fun createAndRegisterVoltageSource(
        positiveLoad: Load,
        negativeLoad: Load? = null,
        initialVoltage: Double = 0.0
    ): VoltageSource {
        return createVoltageSource(positiveLoad, negativeLoad, initialVoltage).also(::registerComponent)
    }

    /**
     * Create and register a block-hosted ELN node for an API load.
     */
    @JvmOverloads
    fun createAndRegisterBlockNode(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        load: Load,
        mask: Int = ElectricalMasks.POWER
    ): BlockNode {
        return BlockNode(dimension, x, y, z, load, mask).also(::registerBlockNode)
    }

    /**
     * Create and register a side-aware block-hosted ELN node for an API load.
     */
    @JvmOverloads
    fun createAndRegisterBlockNode(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        load: Load,
        front: BlockFace,
        mask: Int = ElectricalMasks.POWER,
        vararg connectedFaces: RelativeBlockFace
    ): BlockNode {
        return BlockNode(dimension, x, y, z, load, front, mask, *connectedFaces).also(::registerBlockNode)
    }

    /**
     * Create a block-hosted ELN node with explicit per-face configuration.
     */
    @JvmOverloads
    fun createHostedBlockNode(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        front: BlockFace = BlockFace.XN,
        faceConnections: Map<BlockFace, BlockFaceConnection>
    ): BlockNode {
        require(faceConnections.isNotEmpty()) { "BlockNode faceConnections must not be empty" }
        return BlockNode(
            dimension,
            x,
            y,
            z,
            faceConnections.values.first().representativeLoad,
            front,
            faceConnections
        )
    }

    /**
     * Create and register a block-hosted ELN node with explicit per-face configuration.
     */
    @JvmOverloads
    fun createAndRegisterBlockNode(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        front: BlockFace = BlockFace.XN,
        faceConnections: Map<BlockFace, BlockFaceConnection>
    ): BlockNode {
        return createHostedBlockNode(dimension, x, y, z, front, faceConnections).also(::registerBlockNode)
    }

    /**
     * Register an electrical component (currently resistive load only) with the global ELN simulator.
     */
    fun registerComponent(resistiveLoad: ResistorLoad) {
        apiDebug(
            "registerComponent resistor portA={} portB={} resistance={} voltage={} current={} power={}",
            resistiveLoad.portA.name,
            resistiveLoad.portB?.name ?: "<ground>",
            resistiveLoad.getResistance(),
            resistiveLoad.voltage,
            resistiveLoad.current,
            resistiveLoad.power
        )
        simulator().addElectricalComponent(resistiveLoad.delegate)
    }

    /**
     * Register a resistor linked to a node port with the global ELN simulator.
     */
    fun registerComponent(nodeConnectedResistorLoad: NodeConnectedResistorLoad) {
        apiDebug(
            "registerComponent nodeResistor portA={} node={} resistance={} voltage={} current={} power={}",
            nodeConnectedResistorLoad.portA.name,
            describe(nodeConnectedResistorLoad.portB),
            nodeConnectedResistorLoad.getResistance(),
            nodeConnectedResistorLoad.voltage,
            nodeConnectedResistorLoad.current,
            nodeConnectedResistorLoad.power
        )
        simulator().addElectricalComponent(nodeConnectedResistorLoad.delegate)
    }

    /**
     * Register a signal output source with the global ELN simulator.
     */
    fun registerComponent(signalSource: SignalSource) {
        apiDebug(
            "registerComponent signalSource load={} voltage={} normalized={} highImpedance={}",
            signalSource.load.name,
            signalSource.outputVoltage,
            signalSource.outputNormalized,
            signalSource.isHighImpedance
        )
        simulator().addElectricalComponent(signalSource.delegate)
    }

    fun registerComponent(signalBusSource: SignalBusSource) {
        signalBusSource.delegates().forEach { simulator().addElectricalComponent(it) }
    }

    /**
     * Register a power-domain voltage source with the global ELN simulator.
     */
    fun registerComponent(voltageSource: VoltageSource) {
        apiDebug(
            "registerComponent voltageSource positiveLoad={} negativeLoad={} voltage={} current={} power={}",
            voltageSource.positiveLoad.name,
            voltageSource.negativeLoad?.name ?: "<ground>",
            voltageSource.voltage,
            voltageSource.current,
            voltageSource.power
        )
        simulator().addElectricalComponent(voltageSource.delegate)
    }

    /**
     * Register a generic electrical component with the global ELN simulator.
     */
    fun registerComponent(component: Component) {
        simulator().addElectricalComponent(component)
    }

    /**
     * Remove an electrical component (currently resistive load only) from the global ELN simulator.
     */
    fun unregisterComponent(resistiveLoad: ResistorLoad) {
        apiDebug("unregisterComponent resistor portA={} portB={}", resistiveLoad.portA.name, resistiveLoad.portB?.name ?: "<ground>")
        simulator().removeElectricalComponent(resistiveLoad.delegate)
    }

    /**
     * Remove a resistor linked to a node port from the global ELN simulator.
     */
    fun unregisterComponent(nodeConnectedResistorLoad: NodeConnectedResistorLoad) {
        apiDebug("unregisterComponent nodeResistor portA={} node={}", nodeConnectedResistorLoad.portA.name, describe(nodeConnectedResistorLoad.portB))
        simulator().removeElectricalComponent(nodeConnectedResistorLoad.delegate)
    }

    /**
     * Remove a signal output source from the global ELN simulator.
     */
    fun unregisterComponent(signalSource: SignalSource) {
        apiDebug("unregisterComponent signalSource load={}", signalSource.load.name)
        simulator().removeElectricalComponent(signalSource.delegate)
    }

    fun unregisterComponent(signalBusSource: SignalBusSource) {
        signalBusSource.delegates().forEach { simulator().removeElectricalComponent(it) }
    }

    /**
     * Remove a power-domain voltage source from the global ELN simulator.
     */
    fun unregisterComponent(voltageSource: VoltageSource) {
        apiDebug("unregisterComponent voltageSource positiveLoad={} negativeLoad={}", voltageSource.positiveLoad.name, voltageSource.negativeLoad?.name ?: "<ground>")
        simulator().removeElectricalComponent(voltageSource.delegate)
    }

    /**
     * Remove a generic electrical component from the global ELN simulator.
     */
    fun unregisterComponent(component: Component) {
        simulator().removeElectricalComponent(component)
    }

    /**
     * Register a direct electrical connection created through this API.
     */
    fun registerConnection(connection: NodeConnection) {
        apiDebug(
            "registerConnection portA={} portB={} voltageA={} voltageB={} currentA={} currentB={}",
            describe(connection.portA),
            describe(connection.portB),
            connection.portA.voltage,
            connection.portB.voltage,
            connection.portA.current,
            connection.portB.current
        )
        simulator().addElectricalComponent(connection.delegate)
    }

    /**
     * Create and register a direct electrical connection between two existing ports.
     */
    fun createAndRegisterNodeConnection(loadA: ElectricalLoad, loadB: ElectricalLoad): NodeConnection {
        return createNodeConnection(loadA, loadB).also(::registerConnection)
    }

    /**
     * Remove a direct electrical connection created through this API.
     */
    fun unregisterConnection(connection: NodeConnection) {
        apiDebug("unregisterConnection portA={} portB={}", describe(connection.portA), describe(connection.portB))
        simulator().removeElectricalComponent(connection.delegate)
    }

    /**
     * Register a watchdog as a slow process in the global ELN simulator.
     */
    fun registerWatchdog(watchdog: Watchdog) {
        apiDebug("registerWatchdog type={}", watchdog.javaClass.simpleName)
        simulator().addSlowProcess(watchdog)
    }

    /**
     * Remove a watchdog from the global ELN simulator.
     */
    fun unregisterWatchdog(watchdog: Watchdog) {
        apiDebug("unregisterWatchdog type={}", watchdog.javaClass.simpleName)
        simulator().removeSlowProcess(watchdog)
    }

    /**
     * Build a resistor watch-dog suitable for an electrical load.
     */
    fun createVoltageWatchdog(load: Load, nominalVoltage: Double): VoltageWatchdog {
        apiDebug("createVoltageWatchdog load={} nominalVoltage={}", load.name, nominalVoltage)
        return VoltageWatchdog(VoltageStateWatchDog(load.delegate).setNominalVoltage(nominalVoltage))
    }

    /**
     * Build a resistor power watch-dog suitable for resistive loads.
     */
    fun createPowerWatchdog(resistiveLoad: ResistorLoad, maximumPower: Double): PowerWatchdog {
        apiDebug(
            "createPowerWatchdog portA={} portB={} maximumPower={}",
            resistiveLoad.portA.name,
            resistiveLoad.portB?.name ?: "<ground>",
            maximumPower
        )
        return PowerWatchdog(ResistorPowerWatchdog(resistiveLoad.delegate).setMaximumPower(maximumPower))
    }

    /**
     * Resolve an electrical port from an existing ELN node and expose it as API load.
     */
    @JvmOverloads
    fun resolveNodeLoad(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        side: BlockFace,
        port: NodePort,
        mask: Int = ElectricalMasks.ALL
    ): NodeLoad {
        return resolveNodeLoad(NodeLoadRef(dimension, x, y, z, side, port, mask))
    }

    /**
     * Resolve an electrical port from an API-owned node reference.
     */
    fun resolveNodeLoad(nodeRef: NodeLoadRef): NodeLoad {
        val nodeCoordinate = Coordinate(nodeRef.x, nodeRef.y, nodeRef.z, nodeRef.dimension)
        val side = nodeRef.side.toInternal()
        val lrdu = nodeRef.port.toInternal()
        val mask = nodeRef.mask
        apiDebug(
            "resolveNodeLoad dimension={} x={} y={} z={} side={} port={} mask={}",
            nodeRef.dimension,
            nodeRef.x,
            nodeRef.y,
            nodeRef.z,
            nodeRef.side,
            nodeRef.port,
            mask
        )
        val node = NodeManager.instance?.getNodeFromCoordonate(nodeCoordinate)
            ?: throw IllegalArgumentException("No node found at coordinate $nodeCoordinate")
        val resolvedLoad = node.getElectricalLoad(side, lrdu, mask)
            ?: throw IllegalStateException("No electrical load exposed by node at $nodeCoordinate on side=$side, lrdu=$lrdu, mask=$mask")
        apiDebug(
            "resolvedNodeLoad node={} voltage={} current={} serialResistance={}",
            describe(nodeRef),
            resolvedLoad.voltage,
            resolvedLoad.current,
            resolvedLoad.serialResistance
        )
        return NodeLoad(nodeRef.dimension, nodeRef.x, nodeRef.y, nodeRef.z, nodeRef.side, nodeRef.port, mask, resolvedLoad)
    }

    /**
     * Create a direct connection between two API electrical loads.
     */
    fun createNodeConnection(loadA: ElectricalLoad, loadB: ElectricalLoad): NodeConnection {
        val a = unwrap(loadA)
        val b = unwrap(loadB)
        apiDebug(
            "createNodeConnection portA={} portB={} voltageA={} voltageB={} serialResistanceA={} serialResistanceB={}",
            describe(loadA),
            describe(loadB),
            a.voltage,
            b.voltage,
            a.serialResistance,
            b.serialResistance
        )
        return NodeConnection(loadA, loadB, ElectricalConnection(a, b))
    }

    /**
     * Create and return a direct connection from an API load to a resolved node port.
     */
    fun connectLoadToNode(load: Load, nodeLoad: NodeLoad): NodeConnection {
        return createNodeConnection(load, nodeLoad)
    }

    /**
     * Resolve a node port and create a direct connection from an API load.
     */
    @JvmOverloads
    fun connectLoadToNode(
        load: Load,
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        side: BlockFace,
        port: NodePort,
        mask: Int = ElectricalMasks.ALL
    ): NodeConnection {
        val nodeLoad = resolveNodeLoad(dimension, x, y, z, side, port, mask)
        return connectLoadToNode(load, nodeLoad)
    }

    /**
     * Resolve a node port from an API-owned node reference and create a direct connection from an API load.
     */
    fun connectLoadToNode(load: Load, nodeRef: NodeLoadRef): NodeConnection {
        val nodeLoad = resolveNodeLoad(nodeRef)
        return connectLoadToNode(load, nodeLoad)
    }

    /**
     * Resolve a node port, create a direct connection from an API load, and register it.
     */
    fun connectAndRegisterLoadToNode(load: Load, nodeRef: NodeLoadRef): NodeConnection {
        return connectLoadToNode(load, nodeRef).also(::registerConnection)
    }

    /**
     * Resolve a node port, create a direct connection from an API load, and register it.
     */
    @JvmOverloads
    fun connectAndRegisterLoadToNode(
        load: Load,
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        side: BlockFace,
        port: NodePort,
        mask: Int = ElectricalMasks.ALL
    ): NodeConnection {
        return connectLoadToNode(load, dimension, x, y, z, side, port, mask).also(::registerConnection)
    }

    /**
     * Build a resistor load connected directly to a node port.
     */
    fun createNodeResistor(load: Load, nodeLoad: NodeLoad, resistance: Double): NodeConnectedResistorLoad {
        apiDebug(
            "createNodeResistor load={} node={} resistance={} loadVoltage={} nodeVoltage={}",
            load.name,
            describe(nodeLoad),
            resistance,
            load.voltage,
            nodeLoad.voltage
        )
        return NodeConnectedResistorLoad(load, nodeLoad, resistance)
    }

    /**
     * Resolve a node port from an API-owned node reference and create a resistor from an API load.
     */
    fun createNodeResistor(load: Load, nodeRef: NodeLoadRef, resistance: Double): NodeConnectedResistorLoad {
        return createNodeResistor(load, resolveNodeLoad(nodeRef), resistance)
    }

    /**
     * Convert a load-like object to a serialized endpoint reference.
     */
    fun toRef(load: ElectricalLoad): ElectricalLoadRef {
        return when (load) {
            is Load -> ElectricalLoadRef(loadRefKindLocal, load.name, null)
            is NodeLoad -> ElectricalLoadRef(loadRefKindNode, null, load.toRef())
            else -> throw IllegalArgumentException("Unsupported ElectricalLoad implementation. Use ElectricalIntegration.Load or ElectricalIntegration.NodeLoad.")
        }
    }

    /**
     * Serialize a live connection to a persisted state.
     */
    fun createConnectionState(connection: NodeConnection): NodeConnectionState {
        return NodeConnectionState(toRef(connection.portA), toRef(connection.portB))
    }

    /**
     * Build a live connection from persisted state, or return null when unresolved.
     */
    fun resolveConnectionOrNull(state: NodeConnectionState, loadLookup: LoadLookup): NodeConnection? {
        return resolveConnection(state, loadLookup).createConnectionOrNull()
    }

    /**
     * Build a node load reference for persistence.
     */
    fun createNodeLoadRef(nodeLoad: NodeLoad): NodeLoadRef {
        return nodeLoad.toRef()
    }

    private fun resolveLoadRef(ref: ElectricalLoadRef, loadLookup: LoadLookup): ElectricalLoad? {
        return when (ref.kind) {
            loadRefKindLocal -> loadLookup.getLoad(ref.loadName ?: "")
            loadRefKindNode -> ref.nodeLoadRef?.resolveOrNull()
            else -> null
        }
    }

    private fun toDestructible(action: WatchdogDestroyAction?): IDestructible {
        if (action == null) {
            return object : IDestructible {
                override fun destructImpl() {
                    // no-op
                }

                override fun describe(): String? = null
            }
        }

        return object : IDestructible {
            override fun destructImpl() {
                action.onDestroy()
            }

            override fun describe(): String? {
                return action.describe()
            }
        }
    }

    private fun simulator(): Simulator {
        return Eln.simulator ?: throw IllegalStateException("Electrical Age simulator has not been initialized")
    }

    private fun apiDebug(message: String, vararg args: Any?) {
        if (!Eln.debugEnabled) return
        Eln.LOGGER.info("[ElectricalApiV1] $message", *args)
    }

    private fun describe(load: ElectricalLoad): String {
        return when (load) {
            is Load -> "Load(name=${load.name})"
            is NodeLoad -> "NodeLoad(${describe(load.toRef())})"
            else -> load.javaClass.simpleName
        }
    }

    private fun describe(nodeRef: NodeLoadRef): String {
        return "dim=${nodeRef.dimension},x=${nodeRef.x},y=${nodeRef.y},z=${nodeRef.z},side=${nodeRef.side},port=${nodeRef.port},mask=${nodeRef.mask}"
    }

    private fun unwrap(load: ElectricalLoad): SimElectricalLoad {
        return when (load) {
            is Load -> load.delegate
            is NodeLoad -> load.delegate
            else -> throw IllegalArgumentException("Unsupported ElectricalLoad implementation. Use ElectricalIntegration.Load or ElectricalIntegration.NodeLoad.")
        }
    }

    internal interface InternalHostedFaceConnection {
        val advertisedMask: Int
        fun resolveLoad(mask: Int): SimElectricalLoad?
        fun newConnectionAt(owner: ManagedGhostLoadNode, side: Direction, connection: mods.eln.node.NodeConnection, isA: Boolean) {}
    }

    internal data class InternalSingleFaceConnection(
        val load: SimElectricalLoad,
        val mask: Int
    ) : InternalHostedFaceConnection {
        override val advertisedMask: Int
            get() = mask

        override fun resolveLoad(mask: Int): SimElectricalLoad = load
    }

    internal data class InternalSignalBusFaceConnection(
        val loads: Map<SignalBusChannel, SimElectricalLoad>
    ) : InternalHostedFaceConnection {
        override val advertisedMask: Int
            get() = SignalBusMasks.wildcardMask()

        override fun resolveLoad(mask: Int): SimElectricalLoad {
            return loads.getValue(SignalBusMasks.readChannel(mask))
        }

        override fun newConnectionAt(owner: ManagedGhostLoadNode, side: Direction, connection: mods.eln.node.NodeConnection, isA: Boolean) {
            val otherNode = if (isA) connection.N2 else connection.N1
            when (otherNode) {
                is ManagedGhostLoadNode -> {
                    val otherSide = if (isA) connection.dir2 else connection.dir1
                    val otherFace = otherNode.faceConnections[otherSide] as? InternalSignalBusFaceConnection ?: return
                    for (channel in SignalBusChannel.values().drop(1)) {
                        val econ = ElectricalConnection(loads.getValue(channel), otherFace.loads.getValue(channel))
                        Eln.simulator.addElectricalComponent(econ)
                        connection.addConnection(econ)
                    }
                }
                is mods.eln.node.six.SixNode -> {
                    val otherDirection = if (isA) connection.dir2 else connection.dir1
                    val otherLrdu = if (isA) connection.lrdu2 else connection.lrdu1
                    val el = otherNode.getElement(otherDirection.applyLRDU(otherLrdu))
                    if (el is mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement) {
                        for (channel in SignalBusChannel.values().drop(1)) {
                            val econ = ElectricalConnection(loads.getValue(channel), el.coloredElectricalLoads[channel.id])
                            Eln.simulator.addElectricalComponent(econ)
                            connection.addConnection(econ)
                        }
                    }
                }
            }
        }
    }

    internal class ManagedGhostLoadNode(
        private val hostCoordinate: Coordinate,
        private val front: Direction,
        internal val faceConnections: Map<Direction, InternalHostedFaceConnection>
    ) : GhostNode() {
        fun initialize() {
            onBlockPlacedBy(hostCoordinate, front, null, null)
        }

        override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
            connect()
        }

        override fun initializeFromNBT() {}

        override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
            return faceConnections[side]?.advertisedMask ?: 0
        }

        override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int) = null

        override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): SimElectricalLoad? {
            return faceConnections[side]?.resolveLoad(mask)
        }

        override fun newConnectionAt(connection: mods.eln.node.NodeConnection?, isA: Boolean) {
            if (connection == null) return
            val side = if (isA) connection.dir1 else connection.dir2
            faceConnections[side]?.newConnectionAt(this, side, connection, isA)
        }
    }
}
