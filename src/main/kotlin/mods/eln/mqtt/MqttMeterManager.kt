package mods.eln.mqtt

import mods.eln.misc.Coordinate
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

object MqttMeterManager {
    private val meterSubscriptions = ConcurrentHashMap<String, MeterSubscription>()
    private val infoCache = ConcurrentHashMap<String, MeterInfoState>()

    data class MeterSubscription(
        val key: String,
        val meterId: String,
        val subscriptionHandle: MqttSubscriptionHandle
    )

    private data class MeterInfoState(
        var name: String = "",
        var level: String = "",
        var position: String = ""
    )

    fun ensureSubscription(meterId: String, coordinate: Coordinate, serverConfig: MqttServerConfig, commandHandler: (MqttMeterCommand) -> Unit) {
        val key = keyOf(coordinate)
        val prefix = serverConfig.prefix.orEmpty()
        val baseTopic = topicFor(prefix, meterId)
        val resetTopic = "$baseTopic/ctrl/reset"
        val statusTopic = "$baseTopic/ctrl/status"

        val current = meterSubscriptions[key]
        if (current != null && current.meterId == meterId) {
            return
        }
        current?.subscriptionHandle?.close()

        val client = MqttManager.getClient(serverConfig.name) ?: return
        val resetHandle = client.subscribe(resetTopic) { _, _ ->
            commandHandler.invoke(MqttMeterCommand.Reset)
        }
        val statusHandle = client.subscribe(statusTopic) { _, payload ->
            val text = payload.toString(StandardCharsets.UTF_8).trim()
            val enabled = text.equals("on", ignoreCase = true)
            commandHandler.invoke(MqttMeterCommand.SetStatus(enabled))
        }
        val combinedHandle = MqttSubscriptionHandle {
            resetHandle.close()
            statusHandle.close()
        }
        meterSubscriptions[key] = MeterSubscription(key, meterId, combinedHandle)
    }

    fun publishSnapshot(snapshot: MqttMeterSnapshot) {
        val server = MqttManager.getServerByName(snapshot.info.serverName) ?: return
        val client = MqttManager.getClient(server.name) ?: return
        val baseTopic = topicFor(server.prefix.orEmpty(), snapshot.info.meterId)
        publishText(client, "$baseTopic/stat/power", snapshot.powerWatts)
        publishText(client, "$baseTopic/stat/energy", snapshot.energyWh)
        publishText(client, "$baseTopic/stat/time", snapshot.timeSeconds)
        publishText(client, "$baseTopic/stat/current", snapshot.currentAmps)
        publishText(client, "$baseTopic/stat/voltage", snapshot.voltage)
        publishText(client, "$baseTopic/stat/status", if (snapshot.status) "on" else "off")
        publishInfoIfNeeded(client, baseTopic, snapshot)
    }

    fun release(coordinate: Coordinate) {
        val key = keyOf(coordinate)
        val subscription = meterSubscriptions.remove(key) ?: return
        infoCache.remove(subscription.meterId)
        subscription.subscriptionHandle.close()
    }

    private fun publishInfoIfNeeded(client: SimpleMqttClient, baseTopic: String, snapshot: MqttMeterSnapshot) {
        val state = infoCache.computeIfAbsent(snapshot.info.meterId) { MeterInfoState() }
        val desiredName = snapshot.info.meterName
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
    }

    private fun publishText(client: SimpleMqttClient, topic: String, value: Any, retain: Boolean = false) {
        val payload = value.toString().toByteArray()
        client.publish(topic, payload, retain)
    }

    private fun topicFor(prefix: String, meterId: String): String {
        val builder = StringBuilder()
        if (prefix.isNotBlank()) {
            builder.append(prefix.trimEnd('/'))
            builder.append('/')
        }
        builder.append("eln/meter/")
        builder.append(meterId)
        return builder.toString()
    }

    private fun keyOf(coordinate: Coordinate): String {
        return "${coordinate.dimension}:${coordinate.x}:${coordinate.y}:${coordinate.z}"
    }
}
