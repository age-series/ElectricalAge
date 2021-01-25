package mods.eln.metrics

interface IMetricsAbstraction {
    /**
     * Put a metric to a service.
     *
     * @param value The value of the metric in question (a double)
     * @param unit The unit type (seconds, volts, watts, etc.)
     * @param metricName The name of the metric (for example, time to complete X)
     * @param namespace The part of the code we are in. For example, "MNA"
     */
    fun putMetric(value: Double, unit: UnitTypes, metricName: String, namespace: String, description: String)
}

enum class UnitTypes(val symbol: String) {
    SECONDS("s"),
    MINUTES("m"),
    HOURS("h"),
    MILLISECONDS("ms"),
    MICROSECONDS("µs"),
    NANOSECONDS("ns"),
    TICKS("t"),
    VOLTS("V"),
    AMPS("A"),
    WATTS("W"),
    JOULES("J"),
    NO_UNITS("")
}
