package mods.eln.metrics

import mods.eln.Eln
import mods.eln.misc.Utils
import java.util.concurrent.ConcurrentLinkedQueue

object MetricsSubsystem {
    private val metricSinks = mutableListOf<IMetricsAbstraction>()
    private val metricThread: Thread
    private val metricsIngested = ConcurrentLinkedQueue<MetricData>()

    init {
        if (Eln.prometheusEnable) {
            metricSinks.add(PrometheusMetricsImpl)
        }
        if (Eln.cloudwatchEnable) {
            metricSinks.add(CloudwatchMetricsImpl)
        }

        metricThread = Thread {
            while(true) {
                // Wait a tick for data. We don't need to be exactly on the tick, but it's not worth going faster really.
                Thread.sleep(50)

                var count = 0
                while (true) {
                    val metric = metricsIngested.poll()?: break
                    count++;
                    metricSinks.forEach {
                        try {
                            it.putMetric(metric.value, metric.unit, metric.metricName, metric.namespace, metric.description)
                        } catch (e: Exception) {
                            Utils.println("The ${it.javaClass.name} metric subsystem failed. $e")
                        }
                    }
                }
                //println("Moved the metrics, did $count")
            }
        }
        metricThread.start()
    }

    /**
     * Put a metric to the metrics subsystem (Now with thread safety!)
     *
     * @param value The value of the metric in question (a double)
     * @param unit The unit type (seconds, volts, watts, etc.)
     * @param metricName The name of the metric (for example, time to complete X)
     * @param namespace The part of the code we are in. For example, "MNA"
     */
    fun putMetric(value: Double, unit: UnitTypes, metricName: String, namespace: String, description: String) {
        metricsIngested.add(MetricData(value, unit, metricName, namespace, description))
    }

    /**
     * Simple thing to just put data directly in (for use by simplistic abstractions)
     */
    fun putMetric(metricData: MetricData) {
        metricsIngested.add(metricData)
    }
}

class SimpleMetric(val metricName: String, val namespace: String, val description: String, val unit: UnitTypes) {
    fun putMetric(value: Double) {
        MetricsSubsystem.putMetric(MetricData(value, unit, metricName, namespace, description))
    }
}

data class MetricData(val value: Double, val unit: UnitTypes, val metricName: String, val namespace: String, val description: String)
