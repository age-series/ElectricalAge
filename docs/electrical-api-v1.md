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
- `NodeLoad`: a read-only endpoint resolved from an existing ELN node.
- `NodeLoadRef`: API-owned node address for resolving ELN ports from world position plus side/port info.
- `ResistorLoad`: resistor between two owned loads.
- `NodeConnectedResistorLoad`: resistor between one owned load and one node port.
- `NodeConnection`: direct short connection between two electrical endpoints.
- `Watchdog`: timeout/fault protection wrappers for voltage and power.
- `BlockFace` / `NodePort`: API enums for block-face and node-port selection.
- `ElectricalMasks`: API constants for electrical connection masks.
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
