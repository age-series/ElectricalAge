package mods.eln.mqtt

import mods.eln.misc.Coordinate
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import java.security.SecureRandom
import java.util.Locale

/**
 * Tracks reserved MQTT meter identifiers globally so that multiple meters never reuse the same ID.
 */
object MqttMeterRegistry {
    private data class RegistryEntry(val coordinate: Coordinate, val meterId: String)

    private val assignments = mutableMapOf<String, RegistryEntry>()
    private val usedIds = mutableSetOf<String>()
    private val random = SecureRandom()

    @JvmStatic
    @Synchronized
    fun ensureMeterId(coordinate: Coordinate?, requestedId: String?): String {
        if (coordinate == null || !coordinate.isValid) {
            return requestedId?.takeIf { it.isNotBlank() } ?: generateCandidate()
        }
        val key = keyOf(coordinate)
        val existing = assignments[key]
        if (existing != null) {
            return existing.meterId
        }

        val trimmed = requestedId?.trim()?.takeIf { it.length >= 4 }
        val candidate = when {
            trimmed != null && !usedIds.contains(trimmed) -> trimmed
            else -> generateUniqueId()
        }
        assignments[key] = RegistryEntry(Coordinate(coordinate), candidate)
        usedIds.add(candidate)
        return candidate
    }

    @JvmStatic
    @Synchronized
    fun release(coordinate: Coordinate?) {
        if (coordinate == null) return
        val key = keyOf(coordinate)
        val entry = assignments.remove(key) ?: return
        usedIds.remove(entry.meterId)
    }

    @JvmStatic
    @Synchronized
    fun readFromNbt(tag: NBTTagCompound?) {
        if (tag == null || !tag.getBoolean("present")) return
        assignments.clear()
        usedIds.clear()
        val list = tag.getTagList("entries", 10)
        for (i in 0 until list.tagCount()) {
            val entryTag = list.getCompoundTagAt(i)
            val coord = Coordinate()
            coord.readFromNBT(entryTag, "coord")
            val id = entryTag.getString("id")
            if (id.isNullOrEmpty()) continue
            val key = keyOf(coord)
            assignments[key] = RegistryEntry(coord, id)
            usedIds.add(id)
        }
    }

    @JvmStatic
    @Synchronized
    fun writeToNbt(tag: NBTTagCompound) {
        tag.setBoolean("present", true)
        val list = NBTTagList()
        assignments.values.forEach { entry ->
            val entryTag = NBTTagCompound()
            entry.coordinate.writeToNBT(entryTag, "coord")
            entryTag.setString("id", entry.meterId)
            list.appendTag(entryTag)
        }
        tag.setTag("entries", list)
    }

    @Synchronized
    private fun generateUniqueId(): String {
        var attempts = 0
        while (attempts < 10_000) {
            val candidate = generateCandidate()
            if (!usedIds.contains(candidate)) {
                return candidate
            }
            attempts++
        }
        // Fall back to timestamp-based ID if random pool is exhausted.
        val fallback = System.currentTimeMillis().toString().takeLast(6).padStart(6, '0')
        return fallback
    }

    private fun generateCandidate(): String {
        val value = random.nextInt(1_000_000)
        return String.format(Locale.US, "%06d", value)
    }

    private fun keyOf(coordinate: Coordinate): String {
        return "${coordinate.dimension}:${coordinate.x}:${coordinate.y}:${coordinate.z}"
    }
}
