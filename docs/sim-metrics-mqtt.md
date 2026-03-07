# Simulator MQTT Metrics

This feature publishes low-overhead MNA simulator metrics to MQTT.

The design goal is minimal simulator impact:

- Metrics are captured with primitive counters in the MNA code path.
- Publishing is batched by `simMetricsPublishIntervalTicks`.
- Network writes happen on a dedicated async thread (`eln-sim-metrics`), not on the simulator thread.

## Configuration

Configure in the main mod config under `mqtt`:

- `enable`: enables MQTT support globally.
- `simMetricsEnable`: enables simulator metrics publishing.
- `simMetricsServer`: MQTT server name from `config/eln-mqtt.json`.
- `simMetricsId`: stream ID used in topic paths (default `server`).
- `simMetricsPublishIntervalTicks`: number of simulator ticks per publish batch (default `20`).

`eln-mqtt.json` still defines broker connection info (name/uri/credentials/prefix) as used by MQTT meters/controllers.

## Topics

`$id` below is `simMetricsId`.
`$prefix` below is the optional per-server MQTT prefix from `eln-mqtt.json`.

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

- `.../info/source` (`mna`)
- `.../info/id` (stream ID)

## Notes

- If `simMetricsEnable=true` and `simMetricsServer` is blank, metrics are not published and a warning is logged.
- This replaces Prometheus/CloudWatch ingestion for simulator metrics with MQTT outputs only.
