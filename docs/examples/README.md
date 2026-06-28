# Falstad Import Examples

These examples are written for the Falstad Import Tool. Each folder contains an importable `*.txt` netlist and a Markdown explanation with the same text embedded for direct copying.

The examples use large capacitors and inductors when needed so changes are visible in Electrical Age's 20 Hz simulator.

The source voltage is `3.0 V` in each circuit, matching two AA batteries in series. That keeps the resistor examples closer to a hands-on classroom demonstration before moving to the simulator.

Readouts may use metric prefixes such as `mA` or `mV`. See [Metric Prefixes In Readouts](metric-prefixes.md) for a quick reference.

## Tools

| Icon | Tool | Use                                                                                                                         |
| --- | --- |-----------------------------------------------------------------------------------------------------------------------------|
| ![Falstad Import Tool icon](assets/falstad-import-tool.png) | Falstad Import Tool | Right-click to import a Falstad netlist from the clipboard (copy the example netlist from the bottom of each example page). |
| ![Multimeter icon](assets/multimeter.png) | MultiMeter / Voltmeter | Use it to spot-check voltages at wires and components. It can provide more detail than WAILA tooltips.                      |
| ![Nope wand icon](assets/nope-wand.png) | Nope Wand | Right-click to clean up imported circuit examples in a 10 block radius after trying them.                                   |

Note: The Falstad Import Tool currently shares its icon with the Copy Config Tool, so check the item name if both are nearby.

## Suggested Example Order

| Order | Example | Main Idea | What To Look For |
| --- | --- | --- | --- |
| 1 | [Series Voltage Drops](series-voltage-drops/README.md) | One current path, current draw, voltage dividers | Larger resistors drop more voltage |
| 2 | [Parallel Branch Currents](parallel-branch-currents/README.md) | Multiple current paths, branch current draw | Lower-resistance branches carry more current |
| 3 | [RC Charging Curve](rc-charging/README.md) | Capacitors store charge over time | Voltage rises quickly, then levels off |
| 4 | [RLC Ring-Down](rlc/README.md) | Energy moves between capacitor and inductor | Voltage oscillates positive and negative, then fades |

## Notes

- Start with the static DC examples before showing time-changing circuits.
- Try predicting the displayed voltages before importing each circuit.
- The Industrial Data Logger output is useful even for steady circuits because it gives a visible measurement target.
- For capacitor and inductor examples, the component values are intentionally much larger than many Falstad defaults so the behavior is slow enough to see at 20 Hz.

## Simulator Caveats

There are various notes about the simulator that are useful to know for the keen electrical enthusiast:

### System Values and Rounding

- Certain values in the simulator are rounded to a particular number of decimal places.
- For example, we do not display values smaller than microvolts because our cables would show small voltages due to their low resistance.
- WAILA outputs are updated once every 2 seconds to prevent network congestion. If you require more real-time data, use a datalogger.

More about this can be found in [`src/main/kotlin/mod/eln/misc/Utils.kt` on Line 167](https://github.com/age-series/ElectricalAge/blob/fa1d1fda93e5148dbbb6045513b574e4ab3de745/src/main/kotlin/mods/eln/misc/Utils.kt#L167).

### Schematics

- The diagrams are simplified teaching diagrams, not exact Minecraft block-placement maps. Improvements to the Falstad import tool over time may affect placement specifics.
- Extra simulator details are left out when they would distract from the circuit idea being shown. Eg. wire resistance is not shown

### Wires And Nodes

- In many circuit simulators, a drawn line or wire is simply a node: every point on that connected wire is treated as the same voltage.
- Electrical Age models wires as very low resistance conductive paths. They are close to ideal wires for these examples.
- The diagrams do not draw this small wire resistance as extra resistors because that would hide the main teaching point.
- The examples ignore wire capacitance and wire inductance. The simulator does not use those effects for ordinary wires here either.

### Voltage Sources And Ground

- The importer treats most voltage sources as single-point source blocks.
- These examples use a shared negative side for the circuit rather than drawing a separate two-terminal source block or battery everywhere.
- Ground is the voltage reference for the rest of the circuit.
- When a point is labeled `0 V`, that means it is measured relative to ground.

### Instruments

- The Industrial Data Logger has a minimum interval of 0.05 seconds, which is 20Hz. This is the simulator speed.

### Timing

- Capacitor and inductor values are sometimes much larger than real-world classroom parts.
- That is intentional: the simulator runs at 20 Hz, so slow values make charging curves and oscillations visible.

### Capacitors And Inductors Sizing

- Some capacitor and inductor values that work in the simulator are not components you would want to use in real life.
- They may be dangerous in a classroom environment. Please do not use them in real circuits.