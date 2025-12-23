package mods.eln.mqtt

/**
 * Simple handle that allows removing a subscription when going away.
 */
class MqttSubscriptionHandle internal constructor(private val onClose: () -> Unit) {
    fun close() {
        onClose.invoke()
    }
}
