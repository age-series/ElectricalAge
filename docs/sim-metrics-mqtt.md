# Simulator MQTT Metrics

This feature publishes low-overhead MNA simulator metrics to MQTT.

The design goal is minimal simulator impact:

- Metrics are captured with primitive counters in the MNA code path.
- Publishing is batched by `integrations.mqtt.simMetrics.publishIntervalTicks`.
- Network writes happen on a dedicated async thread (`eln-sim-metrics`), not on the simulator thread.

## Configuration

Configure in the main mod config under `integrations.mqtt`:

- `enabled`: enables MQTT support globally.
- `simMetrics.enabled`: enables simulator metrics publishing.
- `simMetrics.server`: MQTT server name from `config/eln/mqtt.json`.
- `simMetrics.id`: stream ID used in topic paths (default `server`).
- `simMetrics.publishIntervalTicks`: number of simulator ticks per publish batch (default `20`).

`config/eln/mqtt.json` still defines broker connection info (name/uri/credentials/prefix) as used by MQTT meters/controllers.

## Topics

`$id` below is `integrations.mqtt.simMetrics.id`.
`$prefix` below is the optional per-server MQTT prefix from `config/eln/mqtt.json`.

Base topic:

- `$prefix/eln/sim/$id`

Stat topics:

- `.../stat/subsystems`
- `.../stat/inversions`
- `.../stat/singular_matrices`
- `.../stat/inversion_avg_ns`
- `.../stat/inversion_max_ns`
- `.../stat/tick_us`
- `.../stat/electrical_us`
- `.../stat/thermal_fast_us`
- `.../stat/thermal_slow_us`
- `.../stat/slow_us`
- `.../stat/subsystems_current`
- `.../stat/electrical_processes`
- `.../stat/thermal_fast_loads`
- `.../stat/thermal_fast_connections`
- `.../stat/thermal_fast_processes`
- `.../stat/thermal_slow_loads`
- `.../stat/thermal_slow_connections`
- `.../stat/thermal_slow_processes`
- `.../stat/slow_processes`

Info topics (retained):

- `.../info/source` (`simulator`)
- `.../info/id` (stream ID)

## Notes

- If `integrations.mqtt.simMetrics.enabled=true` and `integrations.mqtt.simMetrics.server` is blank, metrics are not published and a warning is logged.
- This replaces Prometheus/CloudWatch ingestion for simulator metrics with MQTT outputs only.
