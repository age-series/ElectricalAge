package mods.eln.mqtt

import mods.eln.misc.Coordinate

/**
 * Data class that represents a configured MQTT server endpoint.
 */
data class MqttServerConfig(
    val name: String,
    val uri: String,
    val username: String? = null,
    val password: String? = null,
    val prefix: String? = null
)

/**
 * Serialized structure for `eln-mqtt.json`.
 */
data class MqttConfiguration(
    val disable: Boolean = false,
    val mqtt: List<MqttServerConfig> = emptyList()
)

/**
 * Information stored inside each MQTT meter element and mirrored to clients.
 */
data class MqttMeterInfo(
    val meterName: String = "",
    val serverName: String = "",
    val meterId: String = "",
    val enabled: Boolean = true
)

/**
 * Immutable snapshot used for MQTT publishing.
 */
data class MqttMeterSnapshot(
    val info: MqttMeterInfo,
    val coordinate: Coordinate,
    val dimensionName: String,
    val powerWatts: Double,
    val energyWh: Double,
    val timeSeconds: Double,
    val currentAmps: Double,
    val voltage: Double,
    val status: Boolean
)

/**
 * Commands delivered from MQTT control topics to the tile entity.
 */
sealed class MqttMeterCommand {
    object Reset : MqttMeterCommand()
    data class SetStatus(val enabled: Boolean) : MqttMeterCommand()
}
