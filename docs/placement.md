# Falstad Placement Notes

This file records empirical placement/orientation findings from testing the Falstad importer against in-game ELN block orientation.

## Area search

- The importer reports the required footprint as `plan.width x plan.height`.
- If the footprint is not square, it also checks the clockwise-rotated footprint.
- The importer now reports which orientation it chose:
  - `original`
  - `rotated clockwise`

## Logic-symbol substitutions

Current Falstad symbol mappings:

- `151` -> `NAND Chip`
- `L` -> `Signal Switch`
- `M` -> `LED vuMeter`

Signal cables are used for these logic substitutions.

## Routing findings

- A simple doubled coordinate grid was too tight for NAND substitutions because adjacent ELN wires auto-connect.
- The current NAND substitute places the gate body directly at the Falstad gate input junction and extends the output with wire, instead of centering the chip body between start/end.
- There is a regression test ensuring nets that are distinct in the Falstad XOR example do not become connected in the generated placement plan.

## Orientation findings

The important pattern is that `baseFront` means "toward the attached wire" in the generated plan, but each ELN substitute interprets its `front` differently.

### Original placement

Empirically, the currently-correct mapping for original placement is:

- `NAND Chip` -> `baseFront.left()`
- `Signal Switch` -> `baseFront.left()`
- `LED vuMeter` -> `baseFront.left()`

### Rotated clockwise placement

Empirically, clockwise-rotated placement inverts that quarter-turn:

- `NAND Chip` -> `baseFront.right()`
- `Signal Switch` -> `baseFront.right()`
- `LED vuMeter` -> `baseFront.right()`

This is the rule currently encoded in `FalstadImporter.frontFor()`.

## Caveat

These orientation rules are based on observed in-game placement results, not a formal derivation from ELN model geometry. If a future substitution is added, assume its `front` semantics need to be measured independently rather than inferred from these three logic blocks.
