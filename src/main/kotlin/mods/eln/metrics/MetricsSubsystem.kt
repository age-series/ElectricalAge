package mods.eln.metrics

import mods.eln.Eln
import mods.eln.mqtt.MqttManager
import mods.eln.mqtt.SimpleMqttClient
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private data class SimMnaMetricsPayload(
    val subSystemCount: Int,
    val inversionCount: Int,
    val singularMatrixCount: Int,
    val inversionAverageNanoseconds: Double,
    val inversionMaximumNanoseconds: Long
)

private data class SimRuntimeMetricsPayload(
    val avgTickMicroseconds: Double,
    val electricalMicroseconds: Double,
    val thermalFastMicroseconds: Double,
    val thermalSlowMicroseconds: Double,
    val slowMicroseconds: Double,
    val subSystemCount: Int,
    val electricalProcessCount: Int,
    val thermalFastLoadCount: Int,
    val thermalFastConnectionCount: Int,
    val thermalFastProcessCount: Int,
    val thermalSlowLoadCount: Int,
    val thermalSlowConnectionCount: Int,
    val thermalSlowProcessCount: Int,
    val slowProcessCount: Int
)

private sealed interface MetricsPayload
private data class MnaPayload(val payload: SimMnaMetricsPayload) : MetricsPayload
private data class RuntimePayload(val payload: SimRuntimeMetricsPayload) : MetricsPayload

private interface MetricsSink {
    fun publishMna(payload: SimMnaMetricsPayload)
    fun publishRuntime(payload: SimRuntimeMetricsPayload)
}

private class MqttSimMetricsSink(
    private val serverName: String,
    private val streamId: String
) : MetricsSink {
    private var sourceInfoPublished = false

    override fun publishMna(payload: SimMnaMetricsPayload) {
        val server = MqttManager.getServerByName(serverName) ?: return
        val client = MqttManager.getClient(server.name) ?: return
        val baseTopic = topicFor(server.prefix.orEmpty(), streamId)
        publishText(client, "$baseTopic/stat/subsystems", payload.subSystemCount)
        publishText(client, "$baseTopic/stat/inversions", payload.inversionCount)
        publishText(client, "$baseTopic/stat/singular_matrices", payload.singularMatrixCount)
        publishText(client, "$baseTopic/stat/inversion_avg_ns", payload.inversionAverageNanoseconds)
        publishText(client, "$baseTopic/stat/inversion_max_ns", payload.inversionMaximumNanoseconds)
        if (!sourceInfoPublished) {
            sourceInfoPublished = true
            publishText(client, "$baseTopic/info/source", "simulator", retain = true)
            publishText(client, "$baseTopic/info/id", streamId, retain = true)
        }
    }

    override fun publishRuntime(payload: SimRuntimeMetricsPayload) {
        val server = MqttManager.getServerByName(serverName) ?: return
        val client = MqttManager.getClient(server.name) ?: return
        val baseTopic = topicFor(server.prefix.orEmpty(), streamId)
        publishText(client, "$baseTopic/stat/tick_us", payload.avgTickMicroseconds)
        publishText(client, "$baseTopic/stat/electrical_us", payload.electricalMicroseconds)
        publishText(client, "$baseTopic/stat/thermal_fast_us", payload.thermalFastMicroseconds)
        publishText(client, "$baseTopic/stat/thermal_slow_us", payload.thermalSlowMicroseconds)
        publishText(client, "$baseTopic/stat/slow_us", payload.slowMicroseconds)

        publishText(client, "$baseTopic/stat/electrical_processes", payload.electricalProcessCount)
        publishText(client, "$baseTopic/stat/thermal_fast_loads", payload.thermalFastLoadCount)
        publishText(client, "$baseTopic/stat/thermal_fast_connections", payload.thermalFastConnectionCount)
        publishText(client, "$baseTopic/stat/thermal_fast_processes", payload.thermalFastProcessCount)
        publishText(client, "$baseTopic/stat/thermal_slow_loads", payload.thermalSlowLoadCount)
        publishText(client, "$baseTopic/stat/thermal_slow_connections", payload.thermalSlowConnectionCount)
        publishText(client, "$baseTopic/stat/thermal_slow_processes", payload.thermalSlowProcessCount)
        publishText(client, "$baseTopic/stat/slow_processes", payload.slowProcessCount)
        publishText(client, "$baseTopic/stat/subsystems_current", payload.subSystemCount)
    }

    private fun publishText(client: SimpleMqttClient, topic: String, value: Any, retain: Boolean = false) {
        client.publish(topic, value.toString().toByteArray(), retain)
    }

    private fun topicFor(prefix: String, streamId: String): String {
        val builder = StringBuilder()
        if (prefix.isNotBlank()) {
            builder.append(prefix.trimEnd('/'))
            builder.append('/')
        }
        builder.append("eln/sim/")
        builder.append(streamId)
        return builder.toString()
    }
}

object MetricsSubsystem {
    private const val DUMMY_SERVER_NAME = "dummy"
    private val running = AtomicBoolean(false)
    private val queue = ConcurrentLinkedQueue<MetricsPayload>()
    private val sinks = mutableListOf<MetricsSink>()
    private val dummyPublishCount = AtomicInteger(0)
    private val dummyInversionCount = AtomicInteger(0)
    private val dummySingularCount = AtomicInteger(0)

    @Volatile
    private var worker: Thread? = null

    @JvmStatic
    @Synchronized
    fun refreshFromConfig() {
        sinks.clear()
        dummyPublishCount.set(0)
        dummyInversionCount.set(0)
        dummySingularCount.set(0)
        val serverName = Eln.simMetricsMqttServer
        val streamId = Eln.simMetricsId.ifBlank { "server" }
        if (!Eln.mqttEnabled || !Eln.simMetricsEnabled) {
            stopWorker()
            return
        }
        if (serverName.equals(DUMMY_SERVER_NAME, ignoreCase = true)) {
            sinks.add(
                object : MetricsSink {
                    override fun publishMna(payload: SimMnaMetricsPayload) {
                        dummyPublishCount.incrementAndGet()
                        dummyInversionCount.addAndGet(payload.inversionCount)
                        dummySingularCount.addAndGet(payload.singularMatrixCount)
                    }

                    override fun publishRuntime(payload: SimRuntimeMetricsPayload) {
                        dummyPublishCount.incrementAndGet()
                    }
                }
            )
            startWorkerIfNeeded()
            return
        }
        if (serverName.isBlank()) {
            Eln.logger.warn("[Metrics] integrations.mqtt.simMetrics.enabled is true but integrations.mqtt.simMetrics.server is blank")
            stopWorker()
            return
        }
        sinks.add(MqttSimMetricsSink(serverName, streamId))
        startWorkerIfNeeded()
    }

    @JvmStatic
    fun publishMnaMetrics(
        subSystemCount: Int,
        inversionCount: Int,
        singularMatrixCount: Int,
        inversionAverageNanoseconds: Double,
        inversionMaximumNanoseconds: Long
    ) {
        if (!Eln.simMetricsEnabled || sinks.isEmpty()) {
            return
        }
        queue.offer(
            MnaPayload(
                SimMnaMetricsPayload(
                subSystemCount = subSystemCount,
                inversionCount = inversionCount,
                singularMatrixCount = singularMatrixCount,
                inversionAverageNanoseconds = inversionAverageNanoseconds,
                inversionMaximumNanoseconds = inversionMaximumNanoseconds
                )
            )
        )
        startWorkerIfNeeded()
    }

    @JvmStatic
    fun publishSimulatorRuntimeMetrics(
        avgTickMicroseconds: Double,
        electricalMicroseconds: Double,
        thermalFastMicroseconds: Double,
        thermalSlowMicroseconds: Double,
        slowMicroseconds: Double,
        subSystemCount: Int,
        electricalProcessCount: Int,
        thermalFastLoadCount: Int,
        thermalFastConnectionCount: Int,
        thermalFastProcessCount: Int,
        thermalSlowLoadCount: Int,
        thermalSlowConnectionCount: Int,
        thermalSlowProcessCount: Int,
        slowProcessCount: Int
    ) {
        if (!Eln.simMetricsEnabled || sinks.isEmpty()) {
            return
        }
        queue.offer(
            RuntimePayload(
                SimRuntimeMetricsPayload(
                    avgTickMicroseconds = avgTickMicroseconds,
                    electricalMicroseconds = electricalMicroseconds,
                    thermalFastMicroseconds = thermalFastMicroseconds,
                    thermalSlowMicroseconds = thermalSlowMicroseconds,
                    slowMicroseconds = slowMicroseconds,
                    subSystemCount = subSystemCount,
                    electricalProcessCount = electricalProcessCount,
                    thermalFastLoadCount = thermalFastLoadCount,
                    thermalFastConnectionCount = thermalFastConnectionCount,
                    thermalFastProcessCount = thermalFastProcessCount,
                    thermalSlowLoadCount = thermalSlowLoadCount,
                    thermalSlowConnectionCount = thermalSlowConnectionCount,
                    thermalSlowProcessCount = thermalSlowProcessCount,
                    slowProcessCount = slowProcessCount
                )
            )
        )
        startWorkerIfNeeded()
    }

    @JvmStatic
    @Synchronized
    fun shutdown() {
        stopWorker()
        queue.clear()
        sinks.clear()
    }

    @JvmStatic
    fun getDummyPublishCountForTests(): Int {
        return dummyPublishCount.get()
    }

    @JvmStatic
    fun getDummyInversionCountForTests(): Int {
        return dummyInversionCount.get()
    }

    @JvmStatic
    fun getDummySingularCountForTests(): Int {
        return dummySingularCount.get()
    }

    @Synchronized
    private fun stopWorker() {
        running.set(false)
        worker?.interrupt()
        worker = null
    }

    @Synchronized
    private fun startWorkerIfNeeded() {
        if (running.get()) {
            return
        }
        running.set(true)
        worker = Thread(
            {
                while (running.get()) {
                    val payload = queue.poll()
                    if (payload == null) {
                        try {
                            Thread.sleep(50)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                        continue
                    }
                    sinks.forEach { sink ->
                        try {
                            when (payload) {
                                is MnaPayload -> sink.publishMna(payload.payload)
                                is RuntimePayload -> sink.publishRuntime(payload.payload)
                            }
                        } catch (e: Exception) {
                            Eln.logger.warn("[Metrics] Sink publish failed: ${e.message}")
                        }
                    }
                }
            },
            "eln-sim-metrics"
        ).apply {
            isDaemon = true
            start()
        }
    }
}
