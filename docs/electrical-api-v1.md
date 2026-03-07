# Electrical API v1 (`mods.eln.api.v1.electrical`)

`mods.eln.api.v1.electrical` is the supported integration surface for third-party electrical load registration, connection wiring, and watchdog usage.

## Why v1 exists

- Stable package naming for external integrations.
- Dedicated persistence helpers for save/load compatibility.
- Explicit node connectivity helpers to link into existing ELN node ports.
- Minimal assumptions about your internal mod model.

## Package layout

Use classes in:

- `mods.eln.api.v1.electrical.ElectricalIntegration`

The API entry object is `ElectricalIntegration`.

## Core concepts

- `Load`: owned electrical node managed by your mod.
- `SignalLoad`: owned signal-domain load with normalized voltage helpers.
- `SignalBusLoad`: owned 16-channel signal bus load.
- `VoltageSource`: ideal power-domain source between one or two owned loads.
- `NodeLoad`: a read-only endpoint resolved from an existing ELN node.
- `NodeLoadRef`: API-owned node address for resolving ELN ports from world position plus side/port info.
- `ResistorLoad`: resistor between two owned loads.
- `NodeConnectedResistorLoad`: resistor between one owned load and one node port.
- `NodeConnection`: direct short connection between two electrical endpoints.
- `Watchdog`: timeout/fault protection wrappers for voltage and power.
- `BlockFace` / `RelativeBlockFace` / `NodePort`: API enums for block-face, placement-relative face, and node-port selection.
- `ElectricalMasks`: API constants for electrical connection masks.
- `SignalLevels`: API helpers for converting between normalized `0..1` values and ELN signal voltage.
- `ElectricalLoadRef` / `NodeConnectionState`: serializable descriptors for persistence.

## Creating and using your load

```kotlin
val myLoad = ElectricalIntegration.Load("my.mod.my_load")
ElectricalIntegration.registerLoad(myLoad)

val otherLoad = ElectricalIntegration.Load("my.mod.other_load")
ElectricalIntegration.registerLoad(otherLoad)
```

## Connecting to existing ELN nodes

When wiring to an existing node port, resolve it first and then either:

- create a direct connection with `connectLoadToNode`
- or create a resistor with `createNodeResistor`

```kotlin
val nodeRef = ElectricalIntegration.NodeLoadRef(
    dimension = world.provider.dimensionId,
    x = xCoord,
    y = yCoord,
    z = zCoord,
    side = ElectricalIntegration.BlockFace.XP,
    port = ElectricalIntegration.NodePort.LEFT,
    mask = ElectricalIntegration.ElectricalMasks.ALL
)

val nodeLoad = ElectricalIntegration.resolveNodeLoad(nodeRef)

val toNodeConnection = ElectricalIntegration.connectLoadToNode(myLoad, nodeLoad)
ElectricalIntegration.registerConnection(toNodeConnection)

val nodeResistor = ElectricalIntegration.createNodeResistor(myLoad, nodeRef, resistance = 1000.0)
ElectricalIntegration.registerComponent(nodeResistor)
```

You can also skip the explicit `resolveNodeLoad` call and connect directly from the reference:

```kotlin
val direct = ElectricalIntegration.connectLoadToNode(myLoad, nodeRef)
ElectricalIntegration.registerConnection(direct)
```

## Signal outputs and normalized signal voltage

For signal-domain outputs, use `SignalSource` instead of a power `VoltageSource`.

```kotlin
val source = ElectricalIntegration.createAndRegisterSignalSource("my.mod.signal_out")

source.setNormalized(0.5)
val halfScaleVolts = ElectricalIntegration.getSignalVoltageLevel() * 0.5

source.setVoltage(halfScaleVolts)
source.setHighImpedance(false)

val load = source.load
val measuredVoltage = load.signalVoltage
val measuredNormalized = load.normalized
```

If you only need the conversion helpers:

```kotlin
val maxSignalVoltage = ElectricalIntegration.SignalLevels.MAX_VOLTAGE
val volts = ElectricalIntegration.SignalLevels.toVoltage(0.25)
val normalized = ElectricalIntegration.SignalLevels.toNormalized(3.0)
```

## Signal bus

For a full 16-channel signal bus on a face, use `SignalBusLoad` and `SignalBusSource`.

```kotlin
val bus = ElectricalIntegration.createAndRegisterSignalBusLoad("my.mod.bus")
val busSource = ElectricalIntegration.createSignalBusSource(bus)
ElectricalIntegration.registerComponent(busSource)

busSource.setNormalized(ElectricalIntegration.SignalBusChannel.RED, 1.0)
busSource.setVoltage(ElectricalIntegration.SignalBusChannel.BLUE, 2.5)
busSource.setHighImpedance(ElectricalIntegration.SignalBusChannel.GREEN, true)

val red = bus[ElectricalIntegration.SignalBusChannel.RED].normalized
val blueVolts = bus.signalVoltage(ElectricalIntegration.SignalBusChannel.BLUE)

for (channel in ElectricalIntegration.SignalBusChannel.ordered()) {
    val volts = bus.signalVoltage(channel)
    val normalized = bus.normalized(channel)
}
```

Bus state can be persisted as one logical unit:

```kotlin
val tag = NBTTagCompound()
bus.writeToNbt(tag, "signalBus")

val restoredBus = ElectricalIntegration.SignalBusLoad("my.mod.bus")
restoredBus.readFromNbt(tag, "signalBus")
```

When exposing a block face as a full bus, use the public hosted-node factory:

```kotlin
val blockNode = ElectricalIntegration.createHostedBlockNode(
    dimension = world.provider.dimensionId,
    x = xCoord,
    y = yCoord,
    z = zCoord,
    front = ElectricalIntegration.BlockFace.ZP,
    faceConnections = mapOf(
        ElectricalIntegration.BlockFace.XN to ElectricalIntegration.SignalBusFaceConnection(bus)
    )
)
```

Multiple bus faces on one block work the same way:

```kotlin
val blockNode = ElectricalIntegration.createHostedBlockNode(
    dimension = world.provider.dimensionId,
    x = xCoord,
    y = yCoord,
    z = zCoord,
    front = ElectricalIntegration.BlockFace.ZP,
    faceConnections = mapOf(
        ElectricalIntegration.BlockFace.XN to ElectricalIntegration.SignalBusFaceConnection(bus),
        ElectricalIntegration.BlockFace.XP to ElectricalIntegration.SignalBusFaceConnection(bus),
        ElectricalIntegration.BlockFace.YP to ElectricalIntegration.FaceConnection(statusLoad, ElectricalIntegration.ElectricalMasks.GATE)
    )
)
```

## Power-domain voltage sources

For an ideal electrical source in the power network, use `VoltageSource`.

```kotlin
val positive = ElectricalIntegration.createAndRegisterLoad("my.mod.source_pos")
val negative = ElectricalIntegration.createAndRegisterLoad("my.mod.source_neg")

val source = ElectricalIntegration.createAndRegisterVoltageSource(
    positiveLoad = positive,
    negativeLoad = negative,
    initialVoltage = 120.0
)

source.setVoltage(230.0)
```

This is intentionally separate from `SignalSource`:

- `SignalSource` is clamped to ELN signal voltage and supports normalized `0..1` writes.
- `VoltageSource` is an unconstrained power-domain source component.

## Registering block-hosted ELN nodes with explicit face configuration

`BlockNode` can now be registered with per-face connectivity instead of one shared mask on all six faces. For mixed connection types, prefer the hosted-node factory over direct constructor calls.

```kotlin
val input = ElectricalIntegration.Load("my.mod.input")
val output = ElectricalIntegration.Load("my.mod.output")

ElectricalIntegration.registerLoad(input)
ElectricalIntegration.registerLoad(output)

val blockNode = ElectricalIntegration.createHostedBlockNode(
    dimension = world.provider.dimensionId,
    x = xCoord,
    y = yCoord,
    z = zCoord,
    front = ElectricalIntegration.BlockFace.ZP,
    faceConnections = mapOf(
        ElectricalIntegration.BlockFace.XN to ElectricalIntegration.FaceConnection(input, ElectricalIntegration.ElectricalMasks.POWER),
        ElectricalIntegration.BlockFace.XP to ElectricalIntegration.FaceConnection(output, ElectricalIntegration.ElectricalMasks.POWER),
        ElectricalIntegration.BlockFace.YN to ElectricalIntegration.FaceConnection(output, ElectricalIntegration.ElectricalMasks.GATE)
    )
)

ElectricalIntegration.registerBlockNode(blockNode)
```

If you want one shared load but only specific placement-relative faces to connect, use the convenience overload:

```kotlin
val sideOnly = ElectricalIntegration.BlockNode(
    world.provider.dimensionId,
    xCoord,
    yCoord,
    zCoord,
    myLoad,
    ElectricalIntegration.BlockFace.ZP,
    ElectricalIntegration.ElectricalMasks.POWER,
    ElectricalIntegration.RelativeBlockFace.LEFT,
    ElectricalIntegration.RelativeBlockFace.RIGHT
)

ElectricalIntegration.registerBlockNode(sideOnly)
```

## Connecting two mod loads

```kotlin
val resistor = ElectricalIntegration.ResistorLoad(myLoad, otherLoad, resistance = 250.0)
ElectricalIntegration.registerComponent(resistor)

val direct = ElectricalIntegration.createNodeConnection(myLoad, otherLoad)
ElectricalIntegration.registerConnection(direct)
```

## Voltage and power watchdogs

```kotlin
val voltageWatchdog = ElectricalIntegration.createVoltageWatchdog(myLoad, nominalVoltage = 220.0)
    .setLimits(120.0, 280.0)
    .setDestroyAction(object : ElectricalIntegration.WatchdogDestroyAction {
        override fun onDestroy() {
            // your mod-specific destroy behavior
        }

        override fun describe(): String? = "v1 watchdog"
    })
ElectricalIntegration.registerWatchdog(voltageWatchdog)
```

## Voltmeter string hooks

`ElectricalLoad` exposes stable helpers for in-game meter style formatting:

- `formatVoltage(prefix: String = "")` -> `Utils.plotVolt(...)`
- `formatCurrent(prefix: String = "")` -> `Utils.plotAmpere(...)`
- `formatPower(prefix: String = "")` -> `Utils.plotPower(...)`
- `formatVoltageCurrent(voltagePrefix: String = "U:", currentPrefix: String = "I:")`
- `formatVoltageCurrentPower(voltagePrefix: String = "U:", currentPrefix: String = "I:", powerPrefix: String = "P:")`

```kotlin
val line = myLoad.formatVoltageCurrentPower()
// Typical output: "U:  230V   I:  0.5A   P:  115W"

player?.addChatMessage(line)
```

The same pattern can be used for `Load` and `NodeLoad` values because both implement `ElectricalLoad`.

## Persistence with NBT

The API ships with serializable descriptors so integrations can persist and restore electrical topology.

### Save

```kotlin
val nbt = NBTTagCompound()

// Persist loads and resistive components.
myLoad.writeToNbt(nbt, "load")
resistor.writeToNbt(nbt, "mainRes")

// Persist a live connection as a stable endpoint pair.
val connState = ElectricalIntegration.createConnectionState(direct)
connState.writeToNbt(nbt, "connection")
```

### Restore

```kotlin
val restoredLoad = ElectricalIntegration.Load("my.mod.my_load")
restoredLoad.readFromNbt(nbt, "load")

val restoredRes = ElectricalIntegration.ResistorLoad(restoredLoad, otherLoad)
restoredRes.readFromNbt(nbt, "mainRes")

val restoredState = ElectricalIntegration.NodeConnectionState.readFromNbt(nbt, "connection")
val lookup = object : ElectricalIntegration.LoadLookup {
    override fun getLoad(name: String): ElectricalIntegration.Load? = when (name) {
        restoredLoad.name -> restoredLoad
        otherLoad.name -> otherLoad
        else -> null
    }
}

val restoredConnection = ElectricalIntegration.resolveConnectionOrNull(restoredState, lookup)
restoredConnection?.let { ElectricalIntegration.registerConnection(it) }
```

### Node endpoint persistence and delayed resolution

- `NodeLoadRef` and `ElectricalLoadRef` can represent either local API load IDs or node ports.
- Use `NodeLoadRef.resolveOrNull()` for restoration paths where world nodes may not be available yet.
- For unresolved connections, re-run restoration after world/node state is ready.

## Lifecycle and version notes

- API is at `mods.eln.api.v1.electrical`; keep package usage fixed to avoid binary breakage when later versions are added.
- `Load`/`NodeLoadRef` serialization keys are namespaced by caller-provided prefixes, so collisions are avoidable.
- Do **not** store simulator internals directly; use only API wrappers.

## Cross-mod compatibility notes

- No dedicated cross-mod communication device is required for this API integration pattern.
- Use standard Forge cross-mod registration patterns in your mod initialization and keep persistence in your tile/entity NBT.
