# Signal System Audit

This document audits the concrete wired users of the ELN signal system.

Scope:
- Included: blocks/elements that own concrete signal endpoints via `NbtElectricalGateInput`, `NbtElectricalGateOutput`, `NbtElectricalGateInputOutput`, or `NbtElectricalGateOutputProcess`.
- Excluded: passive compatibility masks with no owned signal endpoint, and plain power-only/thermal-only devices.
- Also included: a small number of wireless-only signal devices where local electronics power choice is still relevant to signal-system realism.

Current signal-domain assumptions in code:
- Internal signal range: `0.0 V .. 5.0 V`
- Accepted external overdrive window before optional destruction logic: `-0.5 V .. 5.5 V`
- Default signal input read current: about `50 uA` per input
- Default gate-style signal output current: about `5 mA`
- Important exception: the creative `Signal Source` uses an ideal `VoltageSource`, so it is not modeled as a `5 mA`-limited gate driver

Reality check:
- The code currently treats most signal inputs as logic/reference inputs, not as energy-consuming actuator drives.
- That is realistic for logic, sensing, data I/O, and setpoint/control-reference pins.
- That is not realistic for devices where the signal input appears to directly energize a relay, clutch, buzzer, or similar actuator.
- In those cases, the current code is best understood as "the signal pin commands internally powered actuation," not "the signal line itself provides the actuator energy."

Power-source interpretation used below:
- `Battery`: makes sense for small, standalone, low-duty devices such as sensors, wireless nodes, alarm detectors, or handheld/probe-style devices.
- `Lamp/local supply`: makes sense for building-installed devices that could reasonably have a nearby local low-voltage auxiliary supply.
- `5 V conductor in multiconductor wire`: makes sense for panel/control-system devices with several signal I/O lines, where sharing logic power is more realistic than per-device batteries.
- `Needs dedicated actuator power`: the signal line should not be treated as the energy source for the actuation itself.

User-directed realism adjustments:
- Creative `Signal Source` is treated as an ideal educational element and does not need a discernible power source.
- Computer probe faces can reasonably be treated as generously powered by the host computer, up to about `2 A` available per face.
- Alarm should be treated more like a `12 V`, roughly `1 A` load.
- Voltage-to-redstone converter can draw some local electronics power.
- Relays should consume realistic coil current from their own activation supply, not from the switched circuit.
- Grid Switch control already has its own `200 V` power input and should not need signal-line power.
- Floodlight control should use internal/local power for motor actuation rather than drawing that over the signal line.
- Rheostat variants should be treated as requiring actuator power, though the exact realistic threshold is still TBD.

## Endpoint Tables

The same device inventory is grouped below by signal role so it is easier to scan by integration pattern.

### Creative And Idealized Items

| Device / family | Input | Output | Reasonable signal output current | Control inputs need more than default signal power? | Plausible electronics power source | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Signal Source (`ElectricalSourceElement`, signal mode) | None | 1 configurable analog signal output | Ideal / not explicitly current-limited like gate outputs | N/A | None required in the model | Creative/educational source. Intentionally treated as an ideal element for circuit design and teaching. |

### Output-Only And Producer Devices

| Device / family | Input | Output | Reasonable signal output current | Control inputs need more than default signal power? | Plausible electronics power source | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Signal Trimmer / Signal Switch / Signal Button (`ElectricalGateSourceElement`) | None | 1 manual/configurable signal output | `5 mA` | N/A | Battery, lamp/local supply, or `5 V` conductor | Button variant auto-resets. Reasonable as a logic-level source. |
| Redstone-to-Voltage Converter (`ElectricalRedstoneInputElement`) | No wired signal input; takes vanilla redstone | 1 analog signal output | `5 mA` | N/A | Lamp/local supply or `5 V` conductor | Redstone-to-signal bridge. Reasonable as a logic/source output. |
| Wireless Signal Receiver (`WirelessSignalRxElement`) | No wired signal input; takes wireless channel data | 1 signal output | `5 mA` | N/A | Battery or lamp/local supply | Wireless-to-wired bridge. Reasonable as a logic/source output. |
| Scanner (`ScannerElement`) | None | 1 analog signal output | `5 mA` | N/A | Lamp/local supply or `5 V` conductor | Comparator-like scanner for inventories, tanks, and blocks. Reasonable as a logic/source output. |
| Electrical Probe / Voltage Probe (`ElectricalSensorElement`) | Measured electrical line(s), not signal inputs | 1 analog signal output | `5 mA` | N/A | Lamp/local supply or `5 V` conductor | Probe variants convert measured electrical values into signal voltage. Reasonable as buffered outputs. |
| Thermal Probe / Temperature Probe (`ThermalSensorElement`) | Measured thermal line or temperature point, not signal inputs | 1 analog signal output | `5 mA` | N/A | Lamp/local supply or `5 V` conductor | Thermal-to-signal bridge. Reasonable as buffered outputs. |
| Electrical Daylight Sensor / Electrical Light Sensor (`ElectricalLightSensorElement`) | Environment light level | 1 analog signal output | `5 mA` | N/A | Battery, lamp/local supply, or `5 V` conductor | Ambient-light-to-signal. Reasonable as buffered outputs. |
| Electrical Weather Sensor (`ElectricalWeatherSensorElement`) | Environment weather state | 1 analog signal output | `5 mA` | N/A | Battery, lamp/local supply, or `5 V` conductor | Weather-to-signal. Reasonable as buffered outputs. |
| Humidity Sensor (`ElectricalHumiditySensorElement`) | Environment humidity | 1 analog signal output | `5 mA` | N/A | Battery, lamp/local supply, or `5 V` conductor | Humidity-to-signal. Reasonable as buffered outputs. |
| Thermometer Sensor (`ThermometerSensorElement`) | Environment / sampled temperature | 1 analog signal output | `5 mA` | N/A | Battery, lamp/local supply, or `5 V` conductor | Separate from the thermal-cable probe family. Reasonable as buffered outputs. |
| Electrical Anemometer Sensor (`ElectricalWindSensorElement`) | Environment wind | 1 analog signal output | `5 mA` | N/A | Battery or lamp/local supply | Wind-speed-to-signal. Reasonable as buffered outputs. |
| Electrical Entity Sensor (`ElectricalEntitySensorElement`) | Nearby entity presence/range | 1 signal output | `5 mA` | N/A | Battery or lamp/local supply | Proximity-style output. Reasonable as a logic/source output. |
| Electrical Fire Detector (`ElectricalFireDetectorElement`, non-battery variant) | Nearby fire presence | 1 signal output | `5 mA` | N/A | Battery or lamp/local supply | The battery-powered fire buzzer variant has no wired signal output. Non-battery detector output is reasonable as logic-level. |
| Tachometer (`Tachometer`) | Shaft speed / mechanics | 1 analog signal output | `5 mA` | N/A | Lamp/local supply or `5 V` conductor | Mechanical-to-signal bridge. Reasonable as buffered output. |

### Bidirectional And Processing Devices

| Device / family | Input | Output | Reasonable signal output current | Control inputs need more than default signal power? | Plausible electronics power source | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Logic gate family (`LogicGateElement`) | `1..3` signal inputs depending on chip | 1 digital signal output | `5 mA` | No | `5 V` conductor in multiconductor wire | Covers NOT, AND, NAND, OR, NOR, XOR, XNOR, PAL, Schmitt Trigger, D Flip Flop, Oscillator, JK Flip Flop. This is exactly the kind of device where low input current and `5 mA` outputs make sense. |
| Analog chip family (`AnalogChipElement`) | `1..3` signal inputs depending on chip | 1 analog signal output | `5 mA` | No | `5 V` conductor in multiconductor wire | Covers OpAmp, PID, VCO saw, VCO sine, Amplifier, VCA, Summing Unit, Sample-and-Hold, Lowpass Filter. This is exactly the kind of device where low input current and `5 mA` outputs make sense. |
| Signal Processor (`ElectricalMathElement`) | Up to 3 signal inputs, only where the equation uses them | 1 analog signal output | `5 mA` | No | `5 V` conductor in multiconductor wire | Expression-driven processor. Redstone inventory consumption is separate from signal-line power. Low input current is appropriate. |
| Electrical Timer (`ElectricalTimeoutElement`) | 1 signal input | 1 signal output | `5 mA` | No | `5 V` conductor in multiconductor wire | Signal-domain timer / timeout block. Low input current is appropriate. |
| MQTT Signal Controller (`MqttSignalControllerElement`) | 4 bidirectional signal ports | 4 bidirectional signal ports | `5 mA` per port when configured as output | No | `5 V` conductor in multiconductor wire | Each port can be input, output, or high-impedance. Reasonable as internally powered I/O. |
| Modbus RTU (`ModbusRtuElement`) | 4 bidirectional signal ports | 4 bidirectional signal ports | `5 mA` per port when configured as output | No | `5 V` conductor in multiconductor wire | Per-port analog I/O over Modbus integration. Reasonable as internally powered I/O. |
| ComputerCraft IO (`ComputerCraftIoElement`) | 4 bidirectional signal ports | 4 bidirectional signal ports | `5 mA` per port when configured as output | No | `5 V` conductor in multiconductor wire | Per-port computer-controlled analog I/O. Reasonable as internally powered I/O. |
| Computer Probe (`ComputerProbeNode`) | 6 bidirectional signal faces | 6 bidirectional signal faces | Up to about `2 A` per face is acceptable under the user’s realism assumption | No | Powered by the attached computer | This is a special-case exception: the host computer is assumed to provide substantial per-face power budget. |
| Device Probe (`DeviceProbe`) | 6 bidirectional signal faces | 6 bidirectional signal faces | `5 mA` per face when configured as output unless explicitly treated like a computer-powered probe | No | Battery or `5 V` conductor in multiconductor wire | Dev/debug-style multi-face signal probe. If tied to a host computer, it could share the same higher-power exception as the computer probe. |
| Electrical Antenna TX (`ElectricalAntennaTxElement`) | 1 signal command input | 1 signal output | `5 mA` | No | Already has separate power input; signal electronics could also use local `5 V` | Also has a separate power input. Signal output is a normal gate-style output, while transmission power comes from the power side. |

### Input-Only Logic, Display, And Bridge Devices

| Device / family | Input | Output | Reasonable signal output current | Control inputs need more than default signal power? | Plausible electronics power source | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Electrical VU Meter (`ElectricalVuMeter`) | 1 signal input | No wired signal output | N/A | No | Lamp/local supply or `5 V` conductor | Signal display only. Low input current is appropriate. |
| Electrical Digital Display (`ElectricalDigitalDisplayElement`) | 3 signal inputs: value, strobe, dots | No wired signal output | N/A | No | Lamp/local supply or `5 V` conductor | Display/latch behavior only. Low input current is appropriate. |
| Nixie Tube (`NixieTube`) | 3 signal inputs: digit, blank, dots | No wired signal output | N/A | No | Lamp/local supply or `5 V` conductor | Display only. Under the current design these are logic inputs, not tube-drive power inputs. |
| Voltage-to-Redstone Converter (`ElectricalRedstoneOutputElement`) | 1 signal input | No wired signal output; emits vanilla redstone | N/A | No | Lamp/local supply or `5 V` conductor | Signal-to-redstone bridge. Should draw some local electronics power, but not from the signal input itself. |
| Wireless Signal Transmitter (`WirelessSignalTxElement`) | 1 signal input | No wired signal output; emits wireless signal | N/A | No | Battery or lamp/local supply | Wired-to-wireless bridge. Low input current is appropriate if internally powered. Battery is plausible for low-duty standalone nodes. |
| Electrical Antenna RX (`ElectricalAntennaRxElement`) | 1 signal input | No wired signal output; drives a power output instead | N/A | No | Already has separate power path; signal electronics could use local `5 V` | Signal input controls produced power on the power side. Low input current is appropriate. |
| Grid Switch control (`GridSwitch`) | 1 signal control input | No wired signal output | N/A | No | Already has separate `200 V` input | Grid Switch already has its own power source, so the signal input should remain a low-power control/reference input only. |
| Fuel Heat Furnace (`FuelHeatFurnaceElement`) | 1 signal control input | No wired signal output | N/A | No under current assumptions | Local auxiliary supply or `5 V` conductor | Signal only sets external heat command; burner energy comes from fuel. If modeled as an internally powered controller input, low draw is fine. |
| Heat Furnace (`HeatFurnaceElement`) | 1 signal control input | No wired signal output | N/A | No under current assumptions | Local auxiliary supply or `5 V` conductor | Signal only regulates heater behavior; heat energy is not sourced from the signal line. |
| Floodlight (`FloodlightElement`) | 3 signal control inputs: swivel, head, beam | No wired signal output | N/A | No | Internal/local floodlight power | Floodlight control should use the floodlight’s own internal/local power for motors and beam control, not the signal wiring. |
| Variable DC-DC (`VariableDcDc`) | 1 signal control load exposed on 2 ports | No wired signal output | N/A | No | Local auxiliary supply or `5 V` conductor | Control is mirrored to two signal-compatible ports. Main conversion power path is separate. |
| Thermal Heat Exchanger (`ThermalHeatExchanger`) | 1 signal control input | No wired signal output | N/A | No | Local auxiliary supply or `5 V` conductor | Signal only commands the exchanger. Low-draw setpoint/control input is reasonable. |
| Variable Inductor (`VariableInductorSix`) | 1 signal control input | No wired signal output | N/A | No | Local auxiliary supply or `5 V` conductor | Signal only tunes the inductance. Low-draw control/reference input is reasonable. |
| Radial Motor (`RadialMotor`) | 1 signal throttle input | No wired signal output | N/A | No | Local auxiliary supply or `5 V` conductor | Throttle signal is treated as a control/reference input; engine power comes from fuel/mechanics. |
| Turbines (`Turbines`) | 1 signal throttle input | No wired signal output | N/A | No | Local auxiliary supply or `5 V` conductor | Throttle signal is treated as a control/reference input; turbine power path is separate. |

### Input-Only Powered Actuation And Coil Devices

| Device / family | Input | Output | Reasonable signal output current | Control inputs need more than default signal power? | Plausible electronics power source | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Electrical Alarm (`ElectricalAlarmElement`) | 1 signal input | No wired signal output | N/A | Yes in real life, but no in current code | `12 V` local supply, about `1 A` load | The code currently models this as a logic control input only. For realism, treat the alarm hardware as locally powered rather than signal-line powered. |
| Current Relay (`CurrentRelayElement`) | 1 signal control input | No wired signal output | N/A | Yes in real life, but no in current code | Dedicated `12 V` relay/contactor coil supply | Should consume realistic coil current from its own activation supply, not from the switched current path and not from the signal line. Pick coil size to match the physical relay class represented. |
| Electrical Relay family (`ElectricalRelayElement`) | 1 signal control input | No wired signal output | N/A | Yes in real life, but no in current code | LV/MV/HV/VHV relays: dedicated `12 V` coil supply; Signal relay: likely `5 V` coil supply | Includes LV/MV/HV/VHV/Signal relays. Treat the gate as a control command into a proper coil/driver model. Use typical coil current for the apparent relay/contactor size; signal relay can plausibly be a `5 V` coil. |
| Clutch (`Clutch`) | 1 signal control input | No wired signal output | N/A | Yes in real life, and definitely much more than current code models | Dedicated actuator power from local machine auxiliary supply | The code treats this as a logic command input. Realistically the clutch actuator should require substantially more power; exact threshold still TBD. |
| Large Rheostat (`LargeRheostat`) | 1 signal control input | No wired signal output | N/A | Yes for a realistic motorized actuator model | Dedicated actuator power from local auxiliary supply | Signal only sets the rheostat position in code today. For realism, this should be treated as requiring actuator power; exact threshold still TBD. |
| Rheostat (`ResistorElement`, rheostat mode only) | 1 signal control input | No wired signal output | N/A | Yes for a realistic motorized actuator model | Dedicated actuator power from local auxiliary supply | Only the rheostat variant uses the signal input; fixed resistors do not. Same caveat as the large rheostat. |

### Wireless-Only Or No Wired Signal Endpoint Devices

| Device / family | Input | Output | Reasonable signal output current | Control inputs need more than default signal power? | Plausible electronics power source | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Wireless Button / Wireless Switch (`WirelessSignalSourceElement`) | None | No wired signal output; emits wireless signal state | N/A | N/A | Battery is very plausible; lamp/local supply also works | Good example of a device that could reasonably be battery powered in a more realistic design. |

## Summary

- Most wired signal outputs in ELN are backed by `NbtElectricalGateOutputProcess`, so `5 mA` is the right default expectation for their sourcing ability.
- Most wired signal inputs are now backed by a centralized weak read load, so they only draw about `50 uA` each.
- Many control inputs are realistic under the current design only if interpreted as internally powered control/reference inputs rather than as actuator-power inputs.
- Relay-like and actuator-like devices are the main realism mismatch: in real life, relay coils, clutch actuators, buzzers, and motorized rheostat-style controls would typically need more than the default signal input current.
- Devices that already have another primary power source should generally not draw meaningful power from the signal input.
- Devices like relays are different: they are not powered from the circuit they switch, so they should have their own explicit activation supply and coil current model.
- Creative/idealized items should be evaluated separately from physically modeled devices; the main wired-signal outlier there is the creative `Signal Source`, which is implemented as an ideal `VoltageSource` rather than a gate-output process.
