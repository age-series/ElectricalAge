package mods.eln.mqtt

import mods.eln.Eln
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

/**
 * Information stored on each MQTT signal controller.
 */
data class MqttSignalControllerInfo(
    val controllerName: String = "",
    val serverName: String = "",
    val controllerId: String = ""
)

/**
 * Ports exposed by the MQTT signal controller.
 */
enum class SignalPort {
    A, B, C, D;

    val label: String
        get() = name

    companion object {
        private val VALUES = values()
        fun fromOrdinal(index: Int): SignalPort? =
            if (index in VALUES.indices) VALUES[index] else null
    }
}

/**
 * Runtime modes for each signal controller port.
 */
enum class SignalPortMode {
    DISABLED,
    READ,
    WRITE;

    fun next(): SignalPortMode {
        val values = VALUES
        return values[(ordinal + 1) % values.size]
    }

    companion object {
        private val VALUES = values()
        fun fromOrdinal(index: Int): SignalPortMode? =
            if (index in VALUES.indices) VALUES[index] else null
    }
}

/**
 * Immutable snapshot describing the controller state for publishing.
 */
data class SignalPortSnapshot(
    val port: SignalPort,
    val mode: SignalPortMode,
    val voltage: Double
) {
    val normalized: Double
        get() = when {
            voltage <= 0.0 -> 0.0
            voltage >= Eln.SVU -> 1.0
            else -> voltage / Eln.SVU
        }
}

data class MqttSignalControllerSnapshot(
    val info: MqttSignalControllerInfo,
    val coordinate: Coordinate,
    val dimensionName: String,
    val ports: List<SignalPortSnapshot>
)

/**
 * Commands emitted from MQTT topics for the controller.
 */
sealed class MqttSignalControllerCommand {
    data class SetPortVoltage(val port: SignalPort, val voltage: Double) : MqttSignalControllerCommand()
}
