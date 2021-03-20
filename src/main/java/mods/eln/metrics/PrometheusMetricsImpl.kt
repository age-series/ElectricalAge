package mods.eln.metrics

import io.prometheus.client.Gauge
import io.prometheus.client.exporter.HTTPServer
import mods.eln.Eln

object PrometheusMetricsImpl: IMetricsAbstraction {

    init {
        // I have no idea how this works. I guess saving it to a value is not necessary?
        HTTPServer("0.0.0.0", Eln.prometheusPort)
    }

    private val metricMap = mutableMapOf<String, Gauge>()

    /**
     * Put a metric to Prometheus.
     *
     * @param value The value of the metric in question
     * @param unit The unit type (Unused for Prometheus)
     * @param metricName The name of the metric (for example, time to complete X)
     * @param namespace The part of the code we are in. For example, "MNA"
     * @param description The description (a help text for Prometheus)
     */
    override fun putMetric(value: Double, unit: UnitTypes, metricName: String, namespace: String, description: String) {
        createMetric(metricName, namespace, description)
        val metric = metricMap[metricName]?: return
        metric.set(value)
    }

    /**
     * You can't have duplicate metric names registered, so we're going to handle that for you.
     *
     * @param metricName The name of the metrics (for example, time to complete X)
     * @param namespace The part of the code we are in. For example, "MNA"
     * @param description The description (a help text for Prometheus)
     */
    fun createMetric(metricName: String, namespace: String, description: String) {
        if (metricName !in metricMap) {
            val metric = Gauge.build().name("${namespace}_$metricName").help(description).register()
            metricMap[metricName] = metric
        }
    }
}

