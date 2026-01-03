package mods.eln.mqtt

import mods.eln.Eln
import mods.eln.misc.Coordinate
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap

object MqttSignalControllerManager {
    private data class ControllerSubscription(
        val key: String,
        val controllerId: String,
        val subscriptionHandle: MqttSubscriptionHandle
    )

    private data class ControllerInfoState(
        var name: String = "",
        var level: String = "",
        var position: String = "",
        val portModes: EnumMap<SignalPort, SignalPortMode> = EnumMap(SignalPort::class.java)
    )

    private val subscriptions = ConcurrentHashMap<String, ControllerSubscription>()
    private val infoCache = ConcurrentHashMap<String, ControllerInfoState>()

    fun ensureSubscription(
        controllerId: String,
        coordinate: Coordinate,
        serverConfig: MqttServerConfig,
        commandHandler: (MqttSignalControllerCommand) -> Unit
    ) {
        val key = keyOf(coordinate)
        val existing = subscriptions[key]
        if (existing != null && existing.controllerId == controllerId) {
            return
        }
        existing?.subscriptionHandle?.close()

        val client = MqttManager.getClient(serverConfig.name) ?: return
        val baseTopic = topicFor(serverConfig.prefix.orEmpty(), controllerId)
        val handlers = ArrayList<MqttSubscriptionHandle>()
        SignalPort.values().forEach { port ->
            val portId = port.label
            val normalTopic = "$baseTopic/ctrl/port/$portId/normal"
            val voltsTopic = "$baseTopic/ctrl/port/$portId/volts"
            handlers += client.subscribe(normalTopic) { _, payload ->
                parseNormalized(payload)?.let { normalized ->
                    val clamped = (normalized).coerceIn(0.0, 1.0) * Eln.SVU
                    commandHandler.invoke(
                        MqttSignalControllerCommand.SetPortVoltage(port, clampVoltage(clamped))
                    )
                }
            }
            handlers += client.subscribe(voltsTopic) { _, payload ->
                parseVolts(payload)?.let { value ->
                    commandHandler.invoke(
                        MqttSignalControllerCommand.SetPortVoltage(port, clampVoltage(value))
                    )
                }
            }
        }
        val combined = MqttSubscriptionHandle {
            handlers.forEach { it.close() }
        }
        subscriptions[key] = ControllerSubscription(key, controllerId, combined)
    }

    fun publishSnapshot(snapshot: MqttSignalControllerSnapshot) {
        val server = MqttManager.getServerByName(snapshot.info.serverName) ?: return
        val client = MqttManager.getClient(server.name) ?: return
        val baseTopic = topicFor(server.prefix.orEmpty(), snapshot.info.controllerId)
        snapshot.ports.filter { it.mode != SignalPortMode.DISABLED }.forEach { port ->
            val portId = port.port.label
            publishText(client, "$baseTopic/stat/port/$portId/volts", port.voltage)
            publishText(client, "$baseTopic/stat/port/$portId/normal", port.normalized)
        }
        publishInfoIfNeeded(client, baseTopic, snapshot)
    }

    fun release(coordinate: Coordinate) {
        val key = keyOf(coordinate)
        val subscription = subscriptions.remove(key) ?: return
        infoCache.remove(subscription.controllerId)
        subscription.subscriptionHandle.close()
    }

    private fun publishInfoIfNeeded(
        client: SimpleMqttClient,
        baseTopic: String,
        snapshot: MqttSignalControllerSnapshot
    ) {
        val state = infoCache.computeIfAbsent(snapshot.info.controllerId) { ControllerInfoState() }
        val desiredName = snapshot.info.controllerName
        val desiredLevel = snapshot.dimensionName
        val desiredPos = "${snapshot.coordinate.x},${snapshot.coordinate.y},${snapshot.coordinate.z}"
        if (state.name != desiredName) {
            state.name = desiredName
            publishText(client, "$baseTopic/info/name", desiredName, retain = true)
        }
        if (state.level != desiredLevel) {
            state.level = desiredLevel
            publishText(client, "$baseTopic/info/level", desiredLevel, retain = true)
        }
        if (state.position != desiredPos) {
            state.position = desiredPos
            publishText(client, "$baseTopic/info/pos", desiredPos, retain = true)
        }

        val snapshotModes = snapshot.ports.associateBy({ it.port }, { it.mode })
        SignalPort.values().forEach { port ->
            val mode = snapshotModes[port]
            val cached = state.portModes[port]
            if (mode == null || mode == SignalPortMode.DISABLED) {
                if (cached != null) {
                    state.portModes.remove(port)
                    publishEmptyRetained(client, "$baseTopic/info/port/${port.label}")
                }
            } else if (cached != mode) {
                state.portModes[port] = mode
                val value = when (mode) {
                    SignalPortMode.READ -> "read"
                    SignalPortMode.WRITE -> "write"
                    SignalPortMode.DISABLED -> ""
                }
                if (value.isNotEmpty()) {
                    publishText(client, "$baseTopic/info/port/${port.label}", value, retain = true)
                }
            }
        }
    }

    private fun publishEmptyRetained(client: SimpleMqttClient, topic: String) {
        client.publish(topic, ByteArray(0), true)
    }

    private fun publishText(client: SimpleMqttClient, topic: String, value: Any, retain: Boolean = false) {
        client.publish(topic, value.toString().toByteArray(), retain)
    }

    private fun topicFor(prefix: String, controllerId: String): String {
        val builder = StringBuilder()
        if (prefix.isNotBlank()) {
            builder.append(prefix.trimEnd('/'))
            builder.append('/')
        }
        builder.append("eln/signal/")
        builder.append(controllerId)
        return builder.toString()
    }

    private fun keyOf(coordinate: Coordinate): String {
        return "${coordinate.dimension}:${coordinate.x}:${coordinate.y}:${coordinate.z}"
    }

    private fun parseNormalized(payload: ByteArray): Double? {
        val text = payload.toString(StandardCharsets.UTF_8).trim()
        return text.toDoubleOrNull()
    }

    private fun parseVolts(payload: ByteArray): Double? {
        val text = payload.toString(StandardCharsets.UTF_8).trim()
        return text.toDoubleOrNull()
    }

    private fun clampVoltage(voltage: Double): Double {
        if (voltage.isNaN()) return 0.0
        return voltage.coerceIn(0.0, Eln.SVU)
    }
}
