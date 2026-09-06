package mods.eln.config

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent
import mods.eln.Eln
import mods.eln.misc.Utils
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

data class ConfigEntry(
    val name: String,
    var value: Any?
)

object ClientConfigSyncHandler {

    private val syncedConfigEntries = mutableListOf<ConfigEntry>()

    @JvmStatic
    fun getSyncedConfigEntry(name: String): Any? {
        return syncedConfigEntries.firstOrNull { it.name == name }?.value
    }

    @JvmStatic
    fun setSyncedConfigEntry(name: String, value: Any?) {
        syncedConfigEntries.forEach { entry ->
            if (entry.name == name) {
                entry.value = value
                return
            }
        }
        syncedConfigEntries.add(ConfigEntry(name, value))
    }

}

object ServerConfigSyncHandler {

    private val serverConfigEntries = listOf(
        ConfigEntry("debug.logging.enabled", false)
    )

    @SubscribeEvent
    fun onClientJoinServer(@Suppress("UNUSED_PARAMETER") event: PlayerEvent.PlayerLoggedInEvent) {
        syncClientConfig()
    }

    /**
     * Reads the relevant config entries from the server config file, then sends them to all connected clients.
     *
     * This is called automatically whenever a client connects to the server (including singleplayer), as well as
     * every time a client calls the console commands `/eln config` or `/eln debug`.
     */
    @JvmStatic
    fun syncClientConfig() {
        updateServerConfigEntries()
        sendServerConfigToAllClients()
    }

    private fun updateServerConfigEntries() {
        serverConfigEntries.forEach { entry ->
            when (entry.value) {
                is Boolean -> entry.value = Eln.config.getBooleanOrElse(entry.name, false)
                is Int -> entry.value = Eln.config.getIntOrElse(entry.name, 0)
                is Double -> entry.value = Eln.config.getDoubleOrElse(entry.name, 0.0)
                is String -> entry.value = Eln.config.getStringOrElse(entry.name, "")
                else -> entry.value = null // Only the above types are currently supported for config entries
            }
        }
    }

    private fun sendServerConfigToAllClients() {
        Utils.println("Sending server config to all clients.")

        serverConfigEntries.forEach { entry ->
            val bos = ByteArrayOutputStream(64)
            val stream = DataOutputStream(bos)

            try {
                stream.writeByte(Eln.packetServerConfigSync.toInt())
                stream.writeUTF(entry.name)
                when (entry.value) {
                    is Boolean -> {
                        stream.writeChar('b'.code)
                        stream.writeBoolean(entry.value as Boolean)
                    }
                    is Int -> {
                        stream.writeChar('i'.code)
                        stream.writeInt(entry.value as Int)
                    }
                    is Double -> {
                        stream.writeChar('d'.code)
                        stream.writeDouble(entry.value as Double)
                    }
                    is String -> {
                        stream.writeChar('s'.code)
                        stream.writeUTF(entry.value as String)
                    }
                    else -> {
                        return@forEach // Don't send config entries with null values
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            Utils.sendPacketToAllClients(bos)
        }
    }

}