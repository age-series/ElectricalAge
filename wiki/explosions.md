# Explosions

`ValueWatchdog` is the single place where destructive protection is implemented. Every time a component arms a `ValueWatchdog` with `WorldExplosion`, it opts into explosions. The tables below list every such component, the explosion type (`machineExplosion()` = strength 3, `cableExplosion()` = strength 1.5), and what each watchdog monitors.

## Transparent nodes

| Item (class) | File | Explosion type | Watchdog trigger |
| --- | --- | --- | --- |
| Large Rheostat (`LargeRheostatElement`) | `src/main/kotlin/mods/eln/transparentnode/LargeRheostat.kt` | Machine | `ThermalLoadWatchDog` uses `desc.dissipator.warmLimit` to blow up the block if the coil overheats. |
| Legacy DC-DC Converter (`LegacyDcDcElement`) | `src/main/kotlin/mods/eln/transparentnode/LegacyDcDc.kt` | Machine | Primary and secondary `VoltageStateWatchDog`s enforce their nominal 3.2 kV limits. |
| DC-DC Converter (`DcDcElement`) | `src/main/kotlin/mods/eln/transparentnode/DcDc.kt` | Machine | Primary/secondary `VoltageStateWatchDog`s enforce whatever input/output voltage the inventory configures (up to 120 kV). |
| Variable DC-DC Converter (`VariableDcDcElement`) | `src/main/kotlin/mods/eln/transparentnode/VariableDcDc.kt` | Machine | Primary and secondary `VoltageStateWatchDog`s guard their respective rails. |
| Thermal Heat Exchanger | `src/main/kotlin/mods/eln/transparentnode/ThermalHeatExchanger.kt` | Machine | `ThermalLoadWatchDog` clamps the exchanger’s core temperature to the descriptor’s thermal window. |
| Battery Bank (`BatteryElement`) | `src/main/kotlin/mods/eln/transparentnode/battery/BatteryElement.kt` | Machine | A `ThermalLoadWatchDog` explodes the casing if the pack exceeds `thermalWarmLimit`. |
| Arc Furnace | `src/main/kotlin/mods/eln/transparentnode/ArcFurnace.kt` | Machine | A single `VoltageStateWatchDog` forces the arc electrodes to stay within 800 V. |
| Fuel Heat Furnace | `src/main/kotlin/mods/eln/transparentnode/FuelHeatFurnace.kt` | Machine | `ThermalLoadWatchDog` enforces the descriptor’s min/max furnace temperatures. |
| Thermal Dissipator (Passive) | `src/main/java/mods/eln/transparentnode/thermaldissipatorpassive/ThermalDissipatorPassiveElement.java` | Machine | `ThermalLoadWatchDog.setMaximumTemperature(descriptor.warmLimit)` detonates the body when it overheats. |
| Thermal Dissipator (Active) | `src/main/java/mods/eln/transparentnode/thermaldissipatoractive/ThermalDissipatorActiveElement.java` | Machine | Both `ThermalLoadWatchDog` (warm limit) and `VoltageStateWatchDog` (nominal electrical U) share the same machine explosion. |
| Heat Furnace | `src/main/java/mods/eln/transparentnode/heatfurnace/HeatFurnaceElement.java` | Machine | `ThermalLoadWatchDog` enforces the thermal descriptor for the stone furnace. |
| Electrical Furnace | `src/main/java/mods/eln/transparentnode/electricalfurnace/ElectricalFurnaceElement.java` | Machine | A `VoltageStateWatchDog` watches the main load; its nominal voltage is set from the descriptor or GUI. |
| Teleporter | `src/main/java/mods/eln/transparentnode/teleporter/TeleporterElement.java` | Machine | `VoltageStateWatchDog` clamps the power rail to the connected cable’s nominal voltage. |
| Auto Miner | `src/main/java/mods/eln/transparentnode/autominer/AutoMinerElement.java` | Machine | A single `VoltageStateWatchDog` enforces the miner’s nominal supply voltage. |
| Egg Incubator | `src/main/java/mods/eln/transparentnode/eggincubator/EggIncubatorElement.java` | Machine | `VoltageStateWatchDog` watches the incubator’s supply rail at its descriptor-defined voltage. |
| Electrical Machine | `src/main/java/mods/eln/transparentnode/electricalmachine/ElectricalMachineElement.java` | Machine | `VoltageStateWatchDog` monitors the machine’s `descriptor.nominalU`. |
| Power Capacitor (transparent node) | `src/main/java/mods/eln/transparentnode/powercapacitor/PowerCapacitorElement.java` | Machine | A `BipoleVoltageWatchdog` on the capacitor plates triggers the machine explosion on over-voltage. |
| Turbine | `src/main/java/mods/eln/transparentnode/turbine/TurbineElement.java` | Machine | Both a `ThermalLoadWatchDog` (hot side temp) and a `VoltageStateWatchDog` (positive load voltage) share the same explosion. |

## Six-node elements

| Item (class) | File | Explosion type | Watchdog trigger |
| --- | --- | --- | --- |
| Power Capacitor (`PowerCapacitorSixElement`) | `src/main/kotlin/mods/eln/sixnode/PowerCapacitor.kt` | Cable | `BipoleVoltageWatchdog` enforces the descriptor’s nominal voltage for the capacitor block. |
| Power Socket | `src/main/kotlin/mods/eln/sixnode/powersocket/PowerSocketElement.kt` | Cable | A `VoltageStateWatchDog` keeps the socket at or under 300 V. |
| Emergency Lamp | `src/main/kotlin/mods/eln/sixnode/EmergencyLamp.kt` | Cable | `VoltageStateWatchDog` enforces the connected cable’s nominal voltage. |
| Lamp Supply | `src/main/java/mods/eln/sixnode/lampsupply/LampSupplyElement.java` | Cable | The supply’s `VoltageStateWatchDog` makes it explode if the power rail exceeds spec. |
| Current Cable | `src/main/kotlin/mods/eln/sixnode/currentcable/CurrentCable.kt` | Cable | Both `ThermalLoadWatchDog` (warm/cool limits) and `VoltageStateWatchDog` (nominal U) arm a cable explosion. |
| Electrical Cable | `src/main/java/mods/eln/sixnode/electricalcable/ElectricalCableElement.java` | Cable | `ThermalLoadWatchDog` and `VoltageStateWatchDog` enforce the cable descriptor limits. |
| Thermal Cable | `src/main/java/mods/eln/sixnode/thermalcable/ThermalCableElement.java` | Cable | A `ThermalLoadWatchDog` enforces the warm/cool bounds. |
| Current Relay | `src/main/java/mods/eln/sixnode/currentrelay/CurrentRelay.kt` | Cable | The relay’s `ThermalLoadWatchDog` keeps its contacts below `Eln.cableWarmLimit`. |
| Electrical Relay | `src/main/java/mods/eln/sixnode/electricalrelay/ElectricalRelayElement.java` | Cable | Two `VoltageStateWatchDog`s police both relay sides at their cable’s nominal voltage. |
| Electrical Switch | `src/main/java/mods/eln/sixnode/electricalswitch/ElectricalSwitchElement.java` | Cable | Two `VoltageStateWatchDog`s watch the switch’s A/B loads. |
| Electrical Sensor | `src/main/java/mods/eln/sixnode/electricalsensor/ElectricalSensorElement.java` | Cable | When the descriptor is voltage-only, a `VoltageStateWatchDog` guards the measurement load. |
| Energy Meter | `src/main/java/mods/eln/sixnode/energymeter/EnergyMeterElement.java` | Cable | Both sides have `VoltageStateWatchDog`s tied to a cable explosion. |
| Diode | `src/main/java/mods/eln/sixnode/diode/DiodeElement.java` | Cable | `ThermalLoadWatchDog` enforces the diode’s thermal initializer limits. |
| Resistor | `src/main/java/mods/eln/sixnode/resistor/ResistorElement.java` | Cable | `ThermalLoadWatchDog` clamps the resistor’s warm/cool limits. |
| Hub | `src/main/java/mods/eln/sixnode/hub/HubElement.java` | Cable | Each populated side gets a `VoltageStateWatchDog` wired to a shared cable explosion. |
| Battery Charger | `src/main/java/mods/eln/sixnode/batterycharger/BatteryChargerElement.java` | Machine | A `VoltageStateWatchDog` forces the charger input to stay within its nominal voltage. |

## Grid nodes

| Item (class) | File | Explosion type | Watchdog trigger |
| --- | --- | --- | --- |
| Grid Switch | `src/main/kotlin/mods/eln/gridnode/GridSwitch.kt` | Machine | Three `VoltageStateWatchDog`s (power, grid A, grid B) share a machine explosion when any line exceeds its limit. |
| Electrical Pole | `src/main/kotlin/mods/eln/gridnode/electricalpole/ElectricalPoleElement.kt` | Cable by default, Machine when a transformer is installed | The thermal watchdog always uses a cable explosion; the voltage watchdog(s) reuse that explosion, which is upgraded to `machineExplosion()` if `includeTransformer` is true. |
| Grid Transformer | `src/main/kotlin/mods/eln/gridnode/transformer/GridTransformerElement.kt` | Machine | Both `VoltageStateWatchDog`s (primary/secondary) and the `ThermalLoadWatchDog` point to a shared machine explosion. |

## Mechanical

| Item (class) | File | Explosion type | Watchdog trigger |
| --- | --- | --- | --- |
| Simple Shaft (`SimpleShaftElement`) | `src/main/kotlin/mods/eln/mechanical/SimpleShaft.kt` | Machine | The `ShaftSpeedWatchdog` (from `mods/eln/sim/process/destruct/Mechanical.kt`) blows up shafts that exceed their allowed angular speed. |
| Motor | `src/main/kotlin/mods/eln/mechanical/Motor.kt` | Machine | The motor’s `ThermalLoadWatchDog` ties over-temperature straight to a machine explosion. |
| Generator | `src/main/kotlin/mods/eln/mechanical/Generator.kt` | Machine | Identical to the motor: a `ThermalLoadWatchDog` on the stator heat sink triggers a machine explosion. |

