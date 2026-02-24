package mods.eln.environment

import net.minecraft.nbt.NBTTagCompound
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun exchangeLoadWithRoomTransfersPowerAndWarmsRoomAir() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-exchange",
                dim = 0,
                tempC = 0.0,
                interiorCount = 1,
                bounds = intArrayOf(8, 64, 8, 8, 64, 8),
                interiorCells = intArrayOf(8, 64, 8),
                thermalNodes = intArrayOf(8, 64, 7)
            )
        )))
        RoomThermalManager.readFromNbt(nbt, 0)

        val exchangedPower = RoomThermalManager.exchangeLoadWithRoom(
            dimension = 0,
            x = 8,
            y = 64,
            z = 8,
            loadTemperatureDeltaCelsius = 120.0,
            loadRp = 60.0,
            dt = 1.0
        )
        assertNotNull(exchangedPower)
        assertTrue(exchangedPower > 0.0, "Hotter load should dump heat into room.")

        val room = roomsById().values.first()
        val temperatureField = room.javaClass.getDeclaredField("temperatureCelsius")
        temperatureField.isAccessible = true
        val roomTempAfter = temperatureField.getDouble(room)
        assertTrue(roomTempAfter > 0.0, "Room air should warm up after receiving heat.")
    }

    @Test
    fun exchangeLoadWithRoomReturnsNullOutsideRoom() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-nonhit",
                dim = 2,
                tempC = 0.0,
                interiorCount = 1,
                bounds = intArrayOf(0, 64, 0, 0, 64, 0),
                interiorCells = intArrayOf(0, 64, 0),
                thermalNodes = intArrayOf()
            )
        )))
        RoomThermalManager.readFromNbt(nbt, 2)

        val power = RoomThermalManager.exchangeLoadWithRoom(
            dimension = 2,
            x = 1,
            y = 64,
            z = 0,
            loadTemperatureDeltaCelsius = 100.0,
            loadRp = 50.0,
            dt = 1.0
        )

        assertNull(power)
    }

    @Test
    fun exchangeLoadWithRoomWorksForThermalAnchorAdjacentToInteriorAir() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-anchor",
                dim = 7,
                tempC = 0.0,
                interiorCount = 1,
                bounds = intArrayOf(20, 70, 20, 20, 70, 20),
                interiorCells = intArrayOf(20, 70, 20),
                thermalNodes = intArrayOf(20, 70, 19)
            )
        )))
        RoomThermalManager.readFromNbt(nbt, 7)

        val exchangedPower = RoomThermalManager.exchangeLoadWithRoom(
            dimension = 7,
            x = 20,
            y = 70,
            z = 19,
            loadTemperatureDeltaCelsius = 80.0,
            loadRp = 40.0,
            dt = 1.0
        )

        assertNotNull(exchangedPower, "Thermal anchor blocks should exchange heat with room air.")
        assertTrue(exchangedPower > 0.0)

        val debug = RoomThermalManager.getExchangeDebugAt(7, 20, 70, 19)
        assertNotNull(debug, "Exchange debug should be available at anchor coordinate.")
        assertEquals("room-anchor", debug.roomId)
    }

    @Test
    fun advanceRoomAmbientExchangeLeaksFasterWhenDoorCountIsHigher() {
        val nbt = NBTTagCompound()
        nbt.setTag("roomThermal", buildRootNbt(version = 1, rooms = listOf(
            roomTag(
                id = "room-door-leak",
                dim = 4,
                tempC = 100.0,
                interiorCount = 1,
                bounds = intArrayOf(0, 64, 0, 0, 64, 0),
                interiorCells = intArrayOf(0, 64, 0),
                thermalNodes = intArrayOf()
            )
        )))
        RoomThermalManager.readFromNbt(nbt, 4)

        val room = roomsById().values.first()
        setRoomField(room, "lastDoorScanTick", 0L)

        setRoomField(room, "temperatureCelsius", 100.0)
        setRoomField(room, "openDoorCount", 0)
        RoomThermalManager.advanceRoomAmbientExchange(1.0)
        val closedTemp = getRoomDoubleField(room, "temperatureCelsius")

        setRoomField(room, "temperatureCelsius", 100.0)
        setRoomField(room, "openDoorCount", 1)
        RoomThermalManager.advanceRoomAmbientExchange(1.0)
        val openTemp = getRoomDoubleField(room, "temperatureCelsius")

        assertTrue(openTemp < closedTemp, "Open doors should increase room-to-ambient heat loss.")
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

    private fun setRoomField(room: Any, name: String, value: Any) {
        val field = room.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(room, value)
    }

    private fun getRoomDoubleField(room: Any, name: String): Double {
        val field = room.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.getDouble(room)
    }
}
