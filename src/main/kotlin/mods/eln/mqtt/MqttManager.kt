package mods.eln.mqtt

import com.google.gson.GsonBuilder
import mods.eln.Eln
import mods.eln.misc.Utils
import net.minecraft.nbt.NBTTagCompound
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central entry point for accessing MQTT configuration and client instances.
 */
object MqttManager {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val logger = Eln.logger
    private val clients = ConcurrentHashMap<String, SimpleMqttClient>()
    private val configFile = File("config/eln-mqtt.json")
    private var configuration = MqttConfiguration()
    private val listeners = CopyOnWriteArrayList<(MqttConfiguration) -> Unit>()
    private val initialized = AtomicBoolean(false)

    @JvmStatic
    fun init() {
        if (initialized.compareAndSet(false, true)) {
            configuration = applyGlobalToggle(readConfiguration())
        }
    }

    @JvmStatic
    fun refreshConfiguration() {
        configuration = applyGlobalToggle(readConfiguration())
        listeners.forEach { listener ->
            try {
                listener.invoke(configuration)
            } catch (e: Exception) {
                logger.warn("[MQTT] Listener threw during refresh: ${e.message}")
            }
        }
    }

    @JvmStatic
    fun addListener(listener: (MqttConfiguration) -> Unit) {
        listeners.add(listener)
    }

    @JvmStatic
    fun removeListener(listener: (MqttConfiguration) -> Unit) {
        listeners.remove(listener)
    }

    @JvmStatic
    fun getConfig(): MqttConfiguration {
        if (!Eln.mqttEnabled && !configuration.disable) {
            configuration = configuration.copy(disable = true)
        }
        return configuration
    }

    @JvmStatic
    fun getServerByName(name: String?): MqttServerConfig? {
        if (name == null) return null
        return configuration.mqtt.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    @JvmStatic
    fun getClient(name: String): SimpleMqttClient? {
        if (configuration.disable) return null
        val server = getServerByName(name) ?: return null
        return clients.computeIfAbsent(server.name.lowercase()) {
            SimpleMqttClient(server)
        }
    }

    @JvmStatic
    fun shutdown() {
        clients.values.forEach { client ->
            try {
                client.shutdown()
            } catch (e: Exception) {
                logger.warn("[MQTT] Failed to shut down ${e.message}")
            }
        }
        clients.clear()
    }

    private fun readConfiguration(): MqttConfiguration {
        if (!configFile.exists()) {
            configFile.parentFile?.mkdirs()
            writeConfiguration(MqttConfiguration())
            return MqttConfiguration()
        }
        return try {
            FileReader(configFile).use { reader ->
                gson.fromJson(reader, MqttConfiguration::class.java) ?: MqttConfiguration()
            }
        } catch (e: Exception) {
            logger.warn("[MQTT] Failed to read config: ${e.message}")
            MqttConfiguration()
        }
    }

    fun writeConfiguration(configuration: MqttConfiguration) {
        configFile.parentFile?.mkdirs()
        FileWriter(configFile, false).use { writer ->
            gson.toJson(configuration, writer)
        }
    }

    fun readWorldData(tag: NBTTagCompound?) {
        MqttMeterRegistry.readFromNbt(tag)
        val controllers = tag?.getCompoundTag("signalControllers")
        MqttSignalControllerRegistry.readFromNbt(controllers)
    }

    fun writeWorldData(tag: NBTTagCompound) {
        MqttMeterRegistry.writeToNbt(tag)
        val controllers = Utils.newNbtTagCompund(tag, "signalControllers")
        MqttSignalControllerRegistry.writeToNbt(controllers)
    }
    private fun applyGlobalToggle(source: MqttConfiguration): MqttConfiguration {
        if (Eln.mqttEnabled) return source
        return source.copy(disable = true)
    }
}
