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
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.IDestructible
import mods.eln.sim.process.destruct.ResistorPowerWatchdog
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

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
        const val ALL = POWER or GATE
    }

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
    class Load(val name: String) : ElectricalLoad {
        internal val delegate = NbtElectricalLoad(name).apply {
            Eln.applySmallRs(this)
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
     * A managed virtual ELN node hosted at an external mod block position.
     *
     * Registering this node lets adjacent ELN cables connect to and render against
     * the supplied shared load without requiring the block itself to inherit ELN node types.
     */
    class BlockNode @JvmOverloads constructor(
        val dimension: Int,
        val x: Int,
        val y: Int,
        val z: Int,
        val load: Load,
        val mask: Int = ElectricalMasks.POWER
    ) {
        internal val delegate = ManagedGhostLoadNode(Coordinate(x, y, z, dimension), load.delegate, mask)
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
     * Create and register an API load with the global ELN simulator.
     */
    fun createAndRegisterLoad(name: String): Load {
        return Load(name).also(::registerLoad)
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

    internal class ManagedGhostLoadNode(
        private val hostCoordinate: Coordinate,
        private val electricalLoad: SimElectricalLoad,
        private val connectionMask: Int
    ) : GhostNode() {
        fun initialize() {
            onBlockPlacedBy(hostCoordinate, Direction.XN, null, null)
        }

        override fun initializeFromThat(front: Direction, entityLiving: EntityLivingBase?, itemStack: ItemStack?) {
            connect()
        }

        override fun initializeFromNBT() {}

        override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int = connectionMask

        override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int) = null

        override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): SimElectricalLoad = electricalLoad
    }
}
