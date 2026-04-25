# Power Tier Audit

This document audits the concrete ELN devices and voltage-rated items that currently live in the legacy `50V`, `200V`, `800V`, and `3200V` electrical tiers, and evaluates whether they would map more cleanly to rough real-world-inspired tiers of `5V`, `12V`, `24V`, `48V`, `120V`, `240V`, `480V`, and `7.2kV`.

Scope:
- Included: concrete power devices and voltage-rated items whose registration currently anchors them to a legacy electrical tier through nominal voltage or an LV/MV/HV/VHV cable descriptor.
- Included: a small set of cross-tier and tierless power devices that will matter if the legacy four-tier system is replaced.
- Included: creative and idealized devices that should stay educational rather than realistic.
- Excluded: pure signal-system devices already covered by `docs/signal-system-audit.md`, plus purely thermal or purely mechanical devices with no meaningful electrical tier assignment.

Current legacy assumptions in code:
- `50V` is the low-voltage bucket (`Eln.LVU`).
- `200V` is the medium-voltage bucket (`Eln.MVU`).
- `800V` is the high-voltage bucket (`Eln.HVU`).
- `3200V` is the very-high-voltage bucket (`Eln.VVU`).
- A lot of user-facing equipment is tiered by whichever cable descriptor or nominal voltage was convenient at registration time, not because the resulting voltage is especially realistic.

Proposed interpretation used below:
- `5V`: logic and instrumentation only.
- `12V`: very small battery devices, indicator lighting, hobby-scale loads.
- `24V`: industrial controls, actuators, small DC appliances, small off-grid generation.
- `48V`: telecom/battery DC bus, larger low-voltage machines, chargers, compact generators.
- `120V`: light building loads and modest single-phase generation.
- `240V`: residential/commercial mains class equipment, sockets, chargers, medium appliances.
- `480V`: industrial motors, heaters, process machines, larger antennas, compact industrial generation.
- `7.2kV`: distribution feeders, utility poles, grid switches, and other explicit grid hardware.

Migration headline:
- `50V` devices usually want to become `48V`, except obvious lighting/battery oddities that fit `12V` or `24V`.
- `200V` devices usually want to become `240V`, with a few generator/solar cases better at `120V`.
- `800V` endpoint equipment usually wants `480V`; true utility hardware wants `7.2kV`.
- `3200V` is too high for most endpoint machines and should mostly split into either `480V` plant equipment or `7.2kV` grid equipment.

## Proposed Tier Tables

### `5V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Signal Trimmer / Signal Button | Signal-domain manual sources | Signal system | Move to `5V` signal/instrumentation tier | These are straightforward logic-level signal sources and are best kept in the low-power electronics tier. |
| Redstone-to-Voltage Converter | Redstone bridge with analog signal output | Signal system | Move to `5V` signal/instrumentation tier | This is panel/control electronics, not a power-distribution device. |
| Logic gate family | NOT/AND/NAND/OR/NOR/XOR/XNOR/PAL/Schmitt/DFF/Oscillator/JKFF chips | Signal system | Move to `5V` signal/instrumentation tier | This is the clearest `5V` destination in the whole mod. |
| Analog chip family | OpAmp, PID, VCO saw, VCO sine, amplifier, VCA, summing, sample-hold, lowpass | Signal system | Move to `5V` signal/instrumentation tier | Pure low-power electronics. |
| Signal Processor / Electrical Timer | Signal-domain compute/timing blocks | Signal system | Move to `5V` signal/instrumentation tier | These are logic/control processors rather than power hardware. |
| MQTT Signal Controller / Modbus RTU / ComputerCraft IO | Multi-port bidirectional signal I/O controllers | Signal system | Move to `5V` signal/instrumentation tier | Shared low-voltage logic power is the cleanest interpretation. |
| Electrical VU Meter / Electrical Digital Display / Nixie Tube | Signal displays | Signal system | Move logic/control side to `5V` signal/instrumentation tier | These are display/control electronics first; any special display drive can be handled separately if realism is increased later. |
| Voltage-to-Redstone Converter | Signal-to-redstone bridge | Signal system | Move to `5V` signal/instrumentation tier | Draws local electronics power, not meaningful actuator power. |
| Scanner | Comparator-like signal output device | Signal system | Move to `5V` signal/instrumentation tier | Fits buffered low-power instrumentation. |
| Electrical Probe / Voltage Probe / Thermal Probe / Temperature Probe | Probe-style instrumentation with signal outputs | Signal system | Move to `5V` signal/instrumentation tier | These read as instrumentation electronics rather than standalone appliance loads. |
| Electrical Daylight Sensor / Electrical Light Sensor | Ambient-light signal sensors | Signal system | Move to `5V` signal/instrumentation tier | These fit the same low-power sensing/instrumentation model as the other probes and signal sensors. |
| Electrical Weather Sensor / Humidity Sensor / Thermometer Sensor / Electrical Anemometer Sensor / Electrical Entity Sensor | Environmental and presence sensors | Signal system | Move to `5V` signal/instrumentation tier | These are best treated as low-power sensing electronics. |
| Electrical Fire Detector / Electrical Fire Buzzer | Detector/alarm-family sensors | Signal system | Move sensing/control side to `5V` signal/instrumentation tier | The detection electronics belong in `5V`; any alarm-driving or buzzer power can still be modeled separately if desired. |
| Wireless Button / Wireless Switch | Wireless signal source devices with local battery or logic power | Signal system | Move to `5V` signal/instrumentation tier | These fit naturally into the low-power logic domain whether powered by internal batteries or by local `5V` logic wiring. |
| Wireless Signal Receiver / Wireless Signal Transmitter / Wireless Signal Repeater | Wireless signal transport devices with local electronics | Signal system | Move to `5V` signal/instrumentation tier | These are part of the same low-power wireless signal ecosystem as the wireless button/switch family. |
| Computer Probe / Device Probe | Multi-face bidirectional signal probes | Signal system | Move to `5V` signal/instrumentation tier, except where a host computer explicitly powers them | By default these are logic I/O devices, even though the Computer Probe can be given an exceptional host-provided power budget. |

### `12V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Capacity Oriented Battery | About `12.5V` nominal battery block | Tierless | Move to `12V` | This already lands near a standard small battery voltage. |
| Cost Oriented Battery | `50V` nominal, about `250W` standard output, about `120 kJ` nominal stored energy | Tierless | Plausible `12V` candidate | As a general-purpose small rechargeable battery, this reads more like a compact `12V` block than a `48V` plant battery. |
| Life Oriented Battery | `50V` nominal, about `250W` standard output, about `120 kJ` nominal stored energy | Tierless | Plausible `12V` candidate | The long-life emphasis feels closer to durable low-voltage storage than to a `48V` distribution battery. |
| Single-use Battery | `50V` nominal, about `500W` standard output, about `60 kJ` nominal stored energy | Tierless | Plausible `12V` candidate | The realism notes already frame this as a voltaic-pile style battery; that is easier to justify as a low-voltage source. |
| Weak `50V` Battery Charger | `50V`, about `200W` | `50V` | Move to `12V` | This looks much more like a small charger for compact batteries and lighting systems than like a `48V` bus charger. |
| `50V` light bulb family | `50V` incandescent, carbon incandescent, fluorescent, farming, LED, halogen bulbs | `50V` | Move most low-power lighting to `12V` | Small bulbs and decorative/special-purpose lamps read more naturally as low-voltage lighting than as `48V` distribution loads. |
| `50V` Emergency Lamp | `50V` emergency lighting | `50V` | Move to `12V` | Small self-contained emergency lighting is a very strong `12V` use case. |
| Electrical Alarm | Signal-controlled alarm hardware | Signal system | Move to `12V` | The signal audit already identified this as a better fit for a `12V`, roughly `1A` local load. |
| Current Relay coil supply | Signal-controlled relay/contactor actuation | Signal system | Move coil/control side to `12V` | The switched path is separate; the actuation side belongs with other `12V` relay coils. |

### `24V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| `50V` Egg Incubator | `50V`, about `50W` | `50V` | Plausible `24V` candidate | Small appliance-scale heater/control loads fit `24V` reasonably well. |
| Small Active Thermal Dissipator | `50V`, about `50W` | `50V` | Plausible `24V` candidate | This reads more like an auxiliary fan/control assembly than a true `48V` bus appliance. |
| Small Solar Panel / Small Rotating Solar Panel | About `36V` max output, about `200W` for roughly `1 m^2` | Tierless | Move toward `24V` service via charger/controller | A panel this size should gain mostly current/power from area, not arbitrary tier voltage. `36V` max is a plausible small-module value that feeds a `24V` storage/control ecosystem well. |
| Wind Turbine / Water Turbine | LV-cable small generation | Tierless | Plausible `24V` candidate | Small renewable sources often make sense as low-voltage charging hardware rather than as full `48V` plant equipment. |
| Current Oriented Battery | `50V` nominal, about `1 kW` standard output, about `80 kJ` nominal stored energy | Tierless | Move to `24V` | High-current behavior is easier to justify by paralleling lower-voltage cells than by treating it as a `48V` distribution battery. |
| `50V` Battery Charger | `50V`, about `400W` | `50V` | Plausible `24V` candidate | This is a better fit as a heavier low-voltage charger than as a small `48V` plant charger. |
| Grid Switch control power | Already separate from the switched path | Tierless / grid | Use `24V` control power | This is a good fit for explicit industrial control power. |
| Floodlight control side | Swivel/head/beam signal controls on an internally powered fixture | Signal system | Move control side to `24V` actuator/control power | The floodlight signal wiring should command a locally powered actuator system rather than provide actuator energy directly. |
| Clutch control side | Signal throttle/engagement input on mechanical hardware | Signal system | Plausible `24V` actuator/control candidate | This is better treated as industrial control/actuator power than as logic-level power. |
| Large Rheostat / Rheostat actuator side | Signal-controlled position/actuation | Signal system | Plausible `24V` actuator/control candidate | The control signal should not be treated as the actuator energy source. |
| Variable DC-DC / Thermal Heat Exchanger / Variable Inductor | Signal-controlled setpoint devices | Signal system | Plausible `24V` control-side candidates | These are good examples of low-power control systems attached to larger power hardware. |
| Fuel Heat Furnace / Heat Furnace control side | Signal command inputs on process hardware | Signal system | Plausible `24V` control-side candidates | The process energy is elsewhere, but the local control system fits an auxiliary-control tier well. |
| Radial Motor / small Turbine throttle control side | Signal throttle inputs on machine hardware | Signal system | Plausible `24V` control-side candidates | These are machine-control inputs rather than signal-bus loads. |

### Relay control supplies

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Relay coil supplies across the relay families | Currently implicit signal/control actuation | `50V` / `200V` / `800V` / `3200V` families | Use `12V` explicit control power | The switched path and the control coil should not be collapsed into one tier. A standardized `12V` coil supply is a better fit for the relay family than `24V`. |

### `48V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Low Power Transmitter / Receiver Antenna | `50V`, about `250W` | `50V` | Move to `48V` | Small radio/power electronics gear fits a `48V` DC plant well. |
| `50V` Turbine | `50V`, about `1kW` | `50V` | Move to `48V` | Small generator output and storage coupling make sense on a `48V` bus. |
| `50V` Fuel Generator | `50V`, about `1.2kW` | `50V` | Move to `48V` | Strong fit for off-grid or battery-bus generation. |
| `50V` Macerator / Plate Machine / Compressor / Magnetizer | `50V`, about `200W` machine family | `50V` | Move to `48V` | Small DC workshop machines fit better at `48V` than at a nonstandard `50V`. |
| Small / standard `50V` heating elements | `50V` copper, iron, tungsten heater elements | `50V` | Move to `48V` | Resistive heater cartridges at these powers fit a `48V` bus better than `50V`. |
| Voltage Oriented Battery | `200V` nominal, about `250W` standard output, about `120 kJ` nominal stored energy | Tierless | Move to `48V` | Even though the current descriptor is series-stacked to `200V`, its modest power and energy make more sense as a `48V` storage battery than as a mains-class battery block. |
| Experimental Battery | `100V` nominal, about `2 kW` standard output, about `240 kJ` nominal stored energy | Tierless | Plausible `48V` candidate if redesigned as a plant battery | Its current nominal voltage suggests `120V`, but its use as a high-power storage block could also be reframed as a larger `48V` battery system. |

### `120V` / `240V` variants

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Battery Charger family | Currently `50V` weak/standard and `200V` charger set | `50V` / `200V` | Add both `120V` and `240V` variants | A lighter mains-fed charger and a heavier mains-fed charger both make sense; this family should not be forced into only one mains tier. |
| Floodlight family | Supply side is still effectively `200V`-class, but actual light output is driven by inserted `LampDescriptor` bulbs | Tierless | Add both `120V` and `240V` bulb-backed variants | The basic unit can map cleanly to `120V` bulbs and the heavier/motorized unit to `240V` bulbs, but the family as a whole is naturally dual-voltage. |
| Lamp Supply | Tierless lighting distribution block | Tierless | Add both `120V` and `240V` variants | Lighting distribution is a strong candidate for parallel lighter and heavier mains versions. |
| Lamp family as a whole | Lamp items already encode nominal voltage directly | `50V` / `200V` legacy lamp families | Add explicit `120V` and `240V` bulb families | Lamp sockets and floodlights already read bulb voltage/resistance from the inserted lamp item, so dual mains bulb families fit the code structure well. |
| Appliance-class mains devices as a whole | Many current recommendations are being funneled into `240V` because the legacy system had no good `120V` bucket | Tierless | Prefer paired `120V` / `240V` families where appropriate | Small kitchen appliances, lighting systems, and chargers are strong candidates for parallel `120V` and `240V` versions rather than a single unified mains tier. |

### `120V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Experimental Battery | About `100V` nominal | Tierless | Plausible `120V` candidate | This sits between the old low and medium tiers and is closest to `120V`, unless it is intentionally reimagined as a larger `48V` plant battery. |
| `2x3` Solar Panel / `2x3` Rotating Solar Panel | About `108V` max output, about `1200W` for roughly `6 m^2` | Tierless | Plausible `120V` candidate | Treating the `2x3` as a `6 m^2` array makes `~1.2 kW` a more believable power target. The larger panel should scale mostly in wattage/current, with voltage landing near light mains rather than jumping to a bespoke tier. |

### `240V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| `200V` Emergency Lamp | `200V` emergency lighting | `200V` | Move to `240V` | Cleaner fit for building-mains lighting. |
| Type J Socket | Uses the `50V` socket submodel | `50V` | Reassign to `240V` | The real-world idea is a mains socket, not a `48V` accessory outlet. |
| Type E Socket | Uses the `200V` socket submodel | `200V` | Move to `240V` | Already conceptually a European mains socket. |
| Medium Power Transmitter / Receiver Antenna | `200V`, about `1kW` | `200V` | Move to `240V` | Easier to justify as mains-fed radio/power equipment than as a custom `200V` class. |
| `200V` Turbine | `200V`, about `2kW` | `200V` | Move to `240V` | Small mains-class generator output fits here cleanly. |
| `200V` Fuel Generator | `200V`, about `6kW` | `200V` | Move to `240V` | Strong fit for workshop or residential split-phase scale output. |
| `200V` Macerator / Plate Machine / Compressor / Magnetizer | `200V`, about `2kW` machine family | `200V` | Move to `240V` | These are mains-class shop machines. |
| `200V` Active Thermal Dissipator | `200V`, about `60W` | `200V` | Move to `240V` | If it stays as a building appliance rather than a control-side accessory, `240V` is consistent. |
| `200V` light bulb family | `200V` incandescent, fluorescent, farming, LED, halogen bulbs | `200V` | Replace with explicit `240V` bulb tier | The lamp item already carries nominal voltage, so moving from the legacy `200V` family to a true `240V` bulb family is straightforward. |
| Small / standard `200V` heating elements | `200V` copper, iron, tungsten heater elements | `200V` | Move to `240V` | Resistive heating at these powers is easier to explain at `240V`. |

### `480V`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Small / standard `800V` tungsten heating elements | `800V`, roughly `3.6-6.0kW` | `800V` | Move to `480V` | Industrial heater banks in this power range fit `480V` much better. |
| Old `800V` Arc Furnace | `800V`, about `10kW` | `800V` | Move to `480V` | Strong `480V` industrial process-heating use case. |
| High Power Transmitter / Receiver Antenna | `800V`, about `2kW` | `800V` | Move to `480V` | Reads like industrial/transmitter-building equipment, not distribution voltage. |
| Auto Miner | Uses high-voltage cable descriptor | `800V` family | Move to `480V` | Heavy industrial machinery, not feeder infrastructure. |
| `800V` Defence Turret | Named `800V` | `800V` | Move to `480V` | Large motorized industrial load. |
| Small / standard `3.2kV` tungsten heating elements | `3.2kV`, roughly `4-15kW` | `3200V` | Move to `480V` | Endpoint heater elements should not live in a feeder-voltage tier. |
| Shaft Motor / Polarized Shaft Motor | Nominal `3200V`, VHV cable family | `3200V` | Move to `480V` | Industrial motors belong much closer to `480V` than to feeder voltage. |
| Large Rheostat | VHV-connected adjustable resistor | `3200V` | Move to `480V` | Plant equipment rather than distribution infrastructure. |
| Generator / Polarized Shaft Generator | Nominal `3200V`, but registered with HV cable family | Mixed `800V`/`3200V` behavior | Prefer `480V` for plant generation | The current implementation mixes endpoint-machine voltage and cable family in a confusing way. |
| Experimental Transporter | Uses the high-voltage cable descriptor | `800V` family | Plausible `480V` candidate | If treated as a facility machine rather than infrastructure, `480V` is the cleaner fit. |
| Electrical Furnace with high-power heating cores | Tierless controller plus inserted heating elements | Tierless | Use `480V` for heavy industrial heater builds | The block itself should stay tierless, but high-power furnace builds likely land here. |

### `7.2kV`

| Device / family | Current implementation | Legacy source tier | Recommendation | Notes |
| --- | --- | --- | --- | --- |
| Grid DC-DC Converter | Uses current high-voltage cable family for grid hardware | Grid | Move to `7.2kV` | Clear utility/distribution device. |
| Utility Pole / Utility Pole w/DC-DC / Transmission Tower / Direct Utility Pole | Uses current high-voltage cable family for grid hardware | Grid | Move to `7.2kV` | These are the devices that most clearly want a true utility/distribution tier. |
| Grid Switch main path | Has separate control input and grid-side behavior | Grid | Move switched path to `7.2kV` | The main path should become genuine distribution equipment. |
| Large Generator | Nominal `12.8kV`, high-voltage cable family | Tierless high-power | Strong `7.2kV` grid/base-load candidate | This is intentionally grid-facing generation rather than ordinary plant equipment. |
| Large Shaft Motor | Nominal `12.8kV`, VHV cable family | Tierless high-power | Strong `7.2kV` grid/base-load candidate | This belongs with the utility-scale rotating machine family rather than with `480V` industrial motors. |
| Experimental Transporter infrastructure interpretation | Uses the high-voltage cable descriptor | `800V` family | Plausible `7.2kV` candidate | Only makes sense here if treated as special infrastructure with local conversion. |

## Cross-Tier Or Tierless Power Devices

| Device / family | Current implementation | Recommendation | Notes |
| --- | --- | --- | --- |
| Battery family as a whole | Mixed nominal voltages and power/capacity tradeoffs | Review as a chemistry/use-case family, not just a voltage family | The present descriptors suggest a better split of roughly `12V` for compact batteries, `24V` for high-current service batteries, and `48V` only for true plant/storage batteries. |
| Lamp family as a whole | Lamp items already encode nominal voltage directly | Review as explicit bulb families instead of legacy ELN tiers | Adding explicit `12V`, `120V`, and `240V` bulb families would work cleanly with both lamp sockets and floodlights because the fixtures already read bulb voltage/resistance from the inserted lamp item. |
| Signal-system devices as a whole | Today split across logic-level electronics, standalone sensors, and signal-controlled actuators | Review as `5V` logic, `12V` field devices, and `24V` actuator/control auxiliaries | Folding the signal audit into this document makes the rebalance much clearer: not every signal endpoint belongs in the same power tier even if they all speak the same signal language. |
| Electrical Furnace | Tierless controller; actual heating is delegated to inserted heating elements | Keep tierless | Let the installed heating core determine whether a build lands at `48V`, `240V`, or `480V`. |
| Lamp Socket family | Tierless; inserted bulb determines real voltage | Keep tierless | The lamp item already carries the meaningful voltage. |
| DC-DC Converter / Variable DC-DC Converter / Legacy DC-DC Converter | Inherently bridge across voltages | Keep cross-tier | These should translate between tiers, not belong to a single one. |
| Power Capacitor / Power Inductor / Variable Inductor / Power Resistor / Rheostat / Thermistor / Diode family | Mostly neutral, rating-based passives | Keep tierless | These components want explicit voltage/current ratings rather than hard tier buckets. |

## Deprecated Legacy Tier Hardware

| Device / family | Current implementation | Recommendation | Notes |
| --- | --- | --- | --- |
| Low Voltage Cable / Medium Voltage Cable / High Voltage Cable / Very High Voltage Cable | Legacy `50V` / `200V` / `800V` / `3200V` cable families | Deprecate in favor of explicit new-tier cable families | These legacy cable tiers are placeholders for the replacement architecture and should not remain first-class target tiers. |
| Low Current Cable / Medium Current Cable / High Current Cable | Legacy current-cable families | Deprecate in favor of the AWG/mm cable system | The rebalance should keep the explicit conductor-size cable system rather than parallel legacy cable abstractions. |
| Low Voltage Switch / Medium Voltage Switch / High Voltage Switch / Very High Voltage Switch | Legacy tiered switch families tied to the old cable stack | Deprecate in favor of new-tier switch families | These should move with the replacement cable families rather than stay as recommended endpoints in the new tables. |
| Low Voltage Relay / Medium Voltage Relay / High Voltage Relay / Very High Voltage Relay | Legacy relay families tied to the old power tiers | Deprecate in favor of new-tier switched-path families with explicit `12V` coil supplies | The control-side recommendation is still relevant, but the old switched-path voltage families should be retired. |
| Current Relay | Legacy current-relay family | Deprecate in favor of the newer relay direction alongside AWG/mm cable infrastructure | The rebalance should keep one relay path, not both the current-relay abstraction and the newer cable/relay system. |
| Legacy signal cable / signal bus cable / two-port knife-switch-style signal switch / signal relay families | Legacy signal-distribution and switching hardware | Deprecate in favor of the new signal cable, switch, and relay implementations | This deprecation applies to the old signal-path hardware, not to the wired signal source devices like the signal button/trimmer or to the wireless button/switch family. |
| Creative Cable | Extreme creative power cable | Deprecate as gameplay hardware, keep only as creative/debug tool | This should not be part of the realism-oriented replacement tier plan. |

## Creative And Idealized Devices

| Device / family | Current implementation | Recommended target tier | Notes |
| --- | --- | --- | --- |
| Signal Source | Creative configurable signal source | None | Should stay ideal rather than being forced into a realism tier. |
| Electrical Source | Creative configurable power source | None | Should stay ideal rather than being forced into a realism tier. |
| Current Source | Creative configurable current source | None | Same as the electrical source. |
| Creative Power Capacitor | Idealized capacitor | None | Best treated as a sandbox component. |
| Creative Power Inductor | Idealized inductor | None | Best treated as a sandbox component. |
| Creative Power Resistor | Idealized resistor | None | Best treated as a sandbox component. |
| Portable NaN | Explicitly creative/debug-style tool item | None | Not a realism device and should stay outside any tiering pass. |

## Summary

- The new structure should be centered on the proposed target tiers, not on the legacy buckets.
- `48V`, `240V`, `480V`, and `7.2kV` carry most of the realistic power architecture.
- More of the old `50V` ecosystem likely belongs in `12V` and `24V` than in `48V`, especially lighting, compact charging, and many battery-backed devices.
- The battery family should be reviewed by actual role and energy/power profile: compact batteries lean `12V`, high-current service batteries lean `24V`, and only true plant/storage batteries really justify `48V`.
- `5V` remains a signal/instrumentation tier rather than a home for the audited power hardware.
- Tierless controllers, sockets, passives, and converters should stay flexible where their behavior depends on inserts, settings, or surrounding systems rather than on one fixed nominal voltage.
