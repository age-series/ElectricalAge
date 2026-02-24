package mods.eln.environment

import net.minecraft.nbt.NBTTagCompound
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RoomThermalManagerTest {
    @AfterTest
    fun tearDown() {
        RoomThermalManager.clear()
    }

    @Test
    fun readFromNbtPopulatesRoomsAndFastInteriorIndex() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-a",
                dim = 0,
                tempC = 23.5,
                interiorCount = 2,
                bounds = intArrayOf(1, 64, 1, 1, 64, 2),
                interiorCells = intArrayOf(1, 64, 1, 1, 64, 2),
                thermalNodes = intArrayOf(2, 64, 1)
            )
        )))

        RoomThermalManager.readFromNbt(nbt, 0)

        val roomsById = roomsById()
        assertEquals(1, roomsById.size)

        val indexByDim = roomIndexByDimension()
        val dimIndex = indexByDim[0]
        assertNotNull(dimIndex, "Dimension room index should be populated.")
        assertEquals(2, dimIndex.size, "Each interior cell should be indexed for O(1) lookup.")
    }

    @Test
    fun writeToNbtRoundTripPreservesRoomData() {
        val source = NBTTagCompound()
        source.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-roundtrip",
                dim = 3,
                tempC = 41.25,
                interiorCount = 3,
                bounds = intArrayOf(10, 50, 10, 11, 50, 11),
                interiorCells = intArrayOf(10, 50, 10, 11, 50, 10, 11, 50, 11),
                thermalNodes = intArrayOf(10, 50, 9)
            )
        )))
        RoomThermalManager.readFromNbt(source, 3)

        val target = NBTTagCompound()
        RoomThermalManager.writeToNbt(target, 3)

        val root = target.getCompoundTag("roomThermal")
        assertEquals(1, root.getInteger("version"))
        assertEquals(1, root.getInteger("count"))

        val room = root.getCompoundTag("room_0")
        assertEquals("room-roundtrip", room.getString("id"))
        assertEquals(3, room.getInteger("dim"))
        assertEquals(3, room.getInteger("interiorCount"))
        assertEquals(9, room.getIntArray("interiorCells").size)
    }

    @Test
    fun readFromNbtIgnoresUnsupportedVersion() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 99, rooms = listOf(
            roomTag(
                id = "room-invalid-version",
                dim = 0,
                tempC = 10.0,
                interiorCount = 1,
                bounds = intArrayOf(0, 64, 0, 0, 64, 0),
                interiorCells = intArrayOf(0, 64, 0),
                thermalNodes = intArrayOf()
            )
        )))

        RoomThermalManager.readFromNbt(nbt, 0)

        assertTrue(roomsById().isEmpty())
        assertFalse(roomIndexByDimension().containsKey(0))
    }

    @Test
    fun readFromNbtRejectsMalformedInteriorCellEncoding() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-malformed",
                dim = 0,
                tempC = 19.0,
                interiorCount = 2,
                bounds = intArrayOf(5, 70, 5, 6, 70, 5),
                interiorCells = intArrayOf(5, 70), // not a multiple of 3
                thermalNodes = intArrayOf(5, 70, 4)
            )
        )))

        RoomThermalManager.readFromNbt(nbt, 0)

        assertTrue(roomsById().isEmpty(), "Malformed room entries should be discarded.")
    }

    private fun buildRootNbt(version: Int, rooms: List<NBTTagCompound>): NBTTagCompound {
        val root = NBTTagCompound()
        root.setInteger("version", version)
        root.setInteger("count", rooms.size)
        for ((index, room) in rooms.withIndex()) {
            root.setTag("room_$index", room)
        }
        return root
    }

    private fun roomTag(
        id: String,
        dim: Int,
        tempC: Double,
        interiorCount: Int,
        bounds: IntArray,
        interiorCells: IntArray,
        thermalNodes: IntArray
    ): NBTTagCompound {
        val room = NBTTagCompound()
        room.setString("id", id)
        room.setInteger("dim", dim)
        room.setDouble("tempC", tempC)
        room.setLong("lastSeen", 0L)
        room.setInteger("interiorCount", interiorCount)
        room.setTag("bounds", NBTTagCompound().apply {
            setInteger("minX", bounds[0])
            setInteger("minY", bounds[1])
            setInteger("minZ", bounds[2])
            setInteger("maxX", bounds[3])
            setInteger("maxY", bounds[4])
            setInteger("maxZ", bounds[5])
        })
        room.setIntArray("interiorCells", interiorCells)
        room.setIntArray("thermalNodes", thermalNodes)
        return room
    }

    @Suppress("UNCHECKED_CAST")
    private fun roomsById(): MutableMap<Any, Any> {
        val field = RoomThermalManager::class.java.getDeclaredField("roomsById")
        field.isAccessible = true
        return field.get(RoomThermalManager) as MutableMap<Any, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun roomIndexByDimension(): MutableMap<Int, MutableMap<Any, Any>> {
        val field = RoomThermalManager::class.java.getDeclaredField("roomByInteriorCellByDimension")
        field.isAccessible = true
        return field.get(RoomThermalManager) as MutableMap<Int, MutableMap<Any, Any>>
    }
}
