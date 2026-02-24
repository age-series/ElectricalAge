# Biome Climate JSON Schema

Path: `src/main/resources/assets/eln/biomes.json`

Top-level structure:
- JSON array of objects.

Each object fields:
- `Biomes` (required): array of biome keys/names that map to this profile.
- `DayHigh_C` (required): daytime high temperature in Celsius.
- `NightLow_C` (required): nighttime low temperature in Celsius.
- `DayRH_%` (required): daytime relative humidity percent (`0..100`).
- `NightRH_%` (required): nighttime relative humidity percent (`0..100`).
- `Precipitation` (required): one of `"rain"`, `"snow"`, `"none"`.

Extra fields:
- Additional unknown fields are ignored by the parser.
- This means fields like `Notes` can be added freely for documentation.

Example:
```json
{
  "Biomes": ["Plains", "Plains M"],
  "DayHigh_C": 27,
  "NightLow_C": 15,
  "DayRH_%": 45,
  "NightRH_%": 65,
  "Precipitation": "rain"
}
```
