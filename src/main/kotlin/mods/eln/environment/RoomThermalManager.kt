package mods.eln.environment

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import mods.eln.node.transparent.TransparentNode
import net.minecraft.block.BlockDoor
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World
import java.util.ArrayDeque
import java.util.HashMap
import java.util.HashSet
import kotlin.math.abs
import kotlin.math.floor

object RoomThermalManager {
    private const val ROOM_SCAN_INTERVAL_TICKS = 40

    // Defaults if config is unavailable very early in lifecycle.
    private const val DEFAULT_ROOM_MAX_AXIS_SPAN_BLOCKS = 24
    private const val DEFAULT_ROOM_MAX_VOLUME_BLOCKS = 4096

    private const val PLAYER_AIR_SEED_SEARCH_RADIUS = 2
    private const val AIR_HEAT_CAPACITY_J_PER_BLOCK_C = 1200.0
    private const val ROOM_WALL_LEAK_CONDUCTANCE_W_PER_BLOCK_C = 0.12
    private const val ROOM_OPEN_DOOR_EXTRA_CONDUCTANCE_W_PER_DOOR_C = 30.0
    private const val ROOM_DOOR_SCAN_INTERVAL_TICKS = 10L
    private const val EXCHANGE_LOG_INTERVAL_TICKS = 40L
    private const val EXCHANGE_LOG_MIN_POWER_W = 100.0
    private const val ROOM_NBT_VERSION = 1
    private const val NBT_ROOT = "roomThermal"
    private const val NBT_VERSION = "version"
    private const val NBT_COUNT = "count"
    private const val NBT_ROOM_PREFIX = "room_"
    private const val NBT_ID = "id"
    private const val NBT_DIM = "dim"
    private const val NBT_TEMPERATURE = "tempC"
    private const val NBT_LAST_SEEN = "lastSeen"
    private const val NBT_INTERIOR_COUNT = "interiorCount"
    private const val NBT_BOUNDS = "bounds"
    private const val NBT_MIN_X = "minX"
    private const val NBT_MIN_Y = "minY"
    private const val NBT_MIN_Z = "minZ"
    private const val NBT_MAX_X = "maxX"
    private const val NBT_MAX_Y = "maxY"
    private const val NBT_MAX_Z = "maxZ"
    private const val NBT_INTERIOR_CELLS = "interiorCells"
    private const val NBT_THERMAL_NODES = "thermalNodes"

    private val roomsById = HashMap<RoomId, SimulatedRoom>()
    private val roomByInteriorCellByDimension = HashMap<Int, MutableMap<CellPos, RoomId>>()
    private val roomByThermalAnchorByDimension = HashMap<Int, MutableMap<CellPos, RoomId>>()
    private val recentExchangeByAnchorByDimension = HashMap<Int, MutableMap<CellPos, ExchangeDebugSnapshot>>()
    private val lastExchangeLogTickByAnchor = HashMap<AnchorKey, Long>()
    private val pendingImmediateScanDimensions = HashSet<Int>()
    private var tickCounter = 0L

    private fun roomMaxAxisSpanBlocks(): Int {
        return if (Eln.roomMaxAxisSpanBlocks > 0) Eln.roomMaxAxisSpanBlocks else DEFAULT_ROOM_MAX_AXIS_SPAN_BLOCKS
    }

    private fun roomMaxVolumeBlocks(): Int {
        return if (Eln.roomMaxVolumeBlocks > 0) Eln.roomMaxVolumeBlocks else DEFAULT_ROOM_MAX_VOLUME_BLOCKS
    }

    fun tick(server: MinecraftServer) {
        tickCounter++

        val immediateScanDims = if (pendingImmediateScanDimensions.isEmpty()) null else HashSet(pendingImmediateScanDimensions)
        if (immediateScanDims != null) {
            pendingImmediateScanDimensions.clear()
            scanPlayersForRooms(server, immediateScanDims)
        } else if (tickCounter % ROOM_SCAN_INTERVAL_TICKS == 0L) {
            scanPlayersForRooms(server, null)
        }
    }

    fun clear() {
        roomsById.clear()
        roomByInteriorCellByDimension.clear()
        roomByThermalAnchorByDimension.clear()
        recentExchangeByAnchorByDimension.clear()
        lastExchangeLogTickByAnchor.clear()
        pendingImmediateScanDimensions.clear()
        tickCounter = 0L
    }

    fun unloadDimension(dimension: Int) {
        val iterator = roomsById.entries.iterator()
        while (iterator.hasNext()) {
            val (_, room) = iterator.next()
            if (room.dimension != dimension) continue
            deindexRoom(room)
            iterator.remove()
        }
        roomByInteriorCellByDimension.remove(dimension)
        roomByThermalAnchorByDimension.remove(dimension)
        recentExchangeByAnchorByDimension.remove(dimension)
        lastExchangeLogTickByAnchor.keys.removeIf { it.dimension == dimension }
        pendingImmediateScanDimensions.remove(dimension)
    }

    fun onBlockChanged(world: World, x: Int, y: Int, z: Int) {
        if (world.isRemote) return

        val changed = CellPos(x, y, z)
        val dim = world.provider.dimensionId

        val touchedRoomIds = HashSet<RoomId>()
        collectRoomIdsAtOrAdjacent(dim, changed, touchedRoomIds)
        for (roomId in touchedRoomIds) {
            val room = roomsById.remove(roomId) ?: continue
            deindexRoom(room)
            Eln.logger.info(
                "[room-thermal] room-invalidated id={} dim={} cause=block-change at {},{},{}",
                roomId.signature, dim, x, y, z
            )
        }

        pendingImmediateScanDimensions.add(dim)
    }

    fun getRoomVolumeAt(world: World, x: Int, y: Int, z: Int): Int? {
        return getRoomAt(world, x, y, z)?.volumeBlocks
    }

    data class RoomLookup(
        val id: String,
        val dimension: Int,
        val temperatureCelsius: Double,
        val volumeBlocks: Int,
        val airHeatCapacityJoulesPerCelsius: Double
    )

    fun getRoomAt(world: World, x: Int, y: Int, z: Int): RoomLookup? {
        val dim = world.provider.dimensionId
        val roomId = roomByInteriorCellByDimension[dim]?.get(CellPos(x, y, z)) ?: return null
        val room = roomsById[roomId] ?: return null
        return RoomLookup(
            id = room.id.signature,
            dimension = room.dimension,
            temperatureCelsius = room.temperatureCelsius,
            volumeBlocks = room.interiorCellCount,
            airHeatCapacityJoulesPerCelsius = room.airHeatCapacityJoulesPerCelsius
        )
    }

    fun getRoomAt(coord: Coordinate): RoomLookup? {
        if (!coord.worldExist) return null
        return getRoomAt(coord.world(), coord.x, coord.y, coord.z)
    }

    data class ExchangeDebugInfo(
        val roomId: String,
        val roomVolumeBlocks: Int,
        val roomTemperatureCelsius: Double,
        val loadToRoomWatts: Double,
        val sampleTick: Long
    )

    fun getExchangeDebugAt(dimension: Int, x: Int, y: Int, z: Int): ExchangeDebugInfo? {
        val snapshot = recentExchangeByAnchorByDimension[dimension]?.get(CellPos(x, y, z)) ?: return null
        val room = roomsById[snapshot.roomId] ?: return null
        return ExchangeDebugInfo(
            roomId = room.id.signature,
            roomVolumeBlocks = room.interiorCellCount,
            roomTemperatureCelsius = room.temperatureCelsius,
            loadToRoomWatts = snapshot.loadToRoomWatts,
            sampleTick = snapshot.tick
        )
    }

    fun exchangeLoadWithRoom(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        loadTemperatureDeltaCelsius: Double,
        loadRp: Double,
        dt: Double
    ): Double? {
        if (loadRp <= 0.0 || loadRp.isNaN() || loadRp.isInfinite()) return null
        val query = CellPos(x, y, z)
        val roomId = roomByInteriorCellByDimension[dimension]?.get(query)
            ?: roomByThermalAnchorByDimension[dimension]?.get(query)
            ?: return null
        val room = roomsById[roomId] ?: return null
        if (room.airHeatCapacityJoulesPerCelsius <= 0.0) return null

        // Positive value means heat leaves the load and enters room air.
        val powerLoadToRoom = (loadTemperatureDeltaCelsius - room.temperatureCelsius) / loadRp
        room.temperatureCelsius += (powerLoadToRoom * dt) / room.airHeatCapacityJoulesPerCelsius
        rememberExchangeSample(dimension, x, y, z, room.id, powerLoadToRoom)
        maybeLogExchange(dimension, x, y, z, room.id, powerLoadToRoom, loadTemperatureDeltaCelsius, room.temperatureCelsius)
        return powerLoadToRoom
    }

    fun advanceRoomAmbientExchange(dt: Double) {
        if (dt <= 0.0 || dt.isNaN() || dt.isInfinite()) return
        for (room in roomsById.values) {
            if (room.airHeatCapacityJoulesPerCelsius <= 0.0) continue
            refreshOpenDoorCount(room)
            val wallConductance = room.bounds.surfaceAreaEstimate * ROOM_WALL_LEAK_CONDUCTANCE_W_PER_BLOCK_C
            val doorConductance = room.openDoorCount * ROOM_OPEN_DOOR_EXTRA_CONDUCTANCE_W_PER_DOOR_C
            val leakPower = room.temperatureCelsius * (wallConductance + doorConductance)
            room.temperatureCelsius -= (leakPower * dt) / room.airHeatCapacityJoulesPerCelsius
        }
    }

    fun writeToNbt(nbt: NBTTagCompound, dimension: Int) {
        val root = NBTTagCompound()
        root.setInteger(NBT_VERSION, ROOM_NBT_VERSION)

        val dimRooms = roomsById.values.filter { it.dimension == dimension }
        root.setInteger(NBT_COUNT, dimRooms.size)

        for ((index, room) in dimRooms.withIndex()) {
            val roomTag = NBTTagCompound()
            roomTag.setString(NBT_ID, room.id.signature)
            roomTag.setInteger(NBT_DIM, room.dimension)
            roomTag.setDouble(NBT_TEMPERATURE, room.temperatureCelsius)
            roomTag.setLong(NBT_LAST_SEEN, room.lastSeenTick)
            roomTag.setInteger(NBT_INTERIOR_COUNT, room.interiorCellCount)
            roomTag.setTag(NBT_BOUNDS, boundsToNbt(room.bounds))
            roomTag.setIntArray(NBT_INTERIOR_CELLS, encodeCells(room.interiorCells))
            roomTag.setIntArray(NBT_THERMAL_NODES, encodeCells(room.thermalNodeAnchors))
            root.setTag("$NBT_ROOM_PREFIX$index", roomTag)
        }

        nbt.setTag(NBT_ROOT, root)
    }

    fun readFromNbt(nbt: NBTTagCompound, dimension: Int) {
        unloadDimension(dimension)
        if (!nbt.hasKey(NBT_ROOT)) return

        val root = nbt.getCompoundTag(NBT_ROOT)
        val version = root.getInteger(NBT_VERSION)
        if (version != ROOM_NBT_VERSION) {
            Eln.logger.warn("[room-thermal] Ignoring rooms in dim {} due to unsupported NBT version {}", dimension, version)
            return
        }

        val count = root.getInteger(NBT_COUNT)
        for (index in 0 until count) {
            val roomTag = root.getCompoundTag("$NBT_ROOM_PREFIX$index")
            if (roomTag.hasNoTags()) continue

            val room = readRoomFromNbt(roomTag, dimension) ?: continue
            roomsById[room.id] = room
            indexRoom(room)
        }

        Eln.logger.info("[room-thermal] loaded {} rooms for dim {}", roomsById.values.count { it.dimension == dimension }, dimension)
    }

    private fun scanPlayersForRooms(server: MinecraftServer, dimensionFilter: Set<Int>?) {
        val players = server.configurationManager.playerEntityList
            .mapNotNull { it as? EntityPlayerMP }

        if (players.isEmpty()) return

        val thermalNodeCoordsByDimension = collectThermalNodeCoordinatesByDimension()

        for (player in players) {
            val world = player.worldObj ?: continue
            if (world.isRemote) continue
            if (dimensionFilter != null && !dimensionFilter.contains(world.provider.dimensionId)) continue

            val thermalNodes = thermalNodeCoordsByDimension[world.provider.dimensionId] ?: continue
            if (thermalNodes.isEmpty()) continue

            val seed = findPlayerAirSeed(player) ?: continue
            val candidate = findEnclosedRoom(world, seed) ?: continue

            val containedThermalNodes = findThermalNodesInRoom(candidate, thermalNodes)
            if (containedThermalNodes.isEmpty()) continue

            registerOrRefreshRoom(candidate, containedThermalNodes)
        }
    }

    private fun collectThermalNodeCoordinatesByDimension(): Map<Int, List<Coordinate>> {
        val manager = NodeManager.instance ?: return emptyMap()
        val byDimension = HashMap<Int, MutableList<Coordinate>>()

        for (node in manager.nodeList) {
            if (!nodeHasThermalLoads(node)) continue

            val coord = node.coordinate
            val dimensionRooms = byDimension.getOrPut(coord.dimension) { ArrayList() }
            dimensionRooms.add(Coordinate(coord))
        }

        return byDimension
    }

    private fun nodeHasThermalLoads(node: mods.eln.node.NodeBase): Boolean {
        return when (node) {
            is TransparentNode -> node.element?.thermalLoadList?.isNotEmpty() == true
            is SixNode -> node.sideElementList.any { element -> element?.thermalLoadList?.isNotEmpty() == true }
            else -> false
        }
    }

    private fun findPlayerAirSeed(player: EntityPlayerMP): CellPos? {
        val world = player.worldObj ?: return null
        val baseX = floor(player.posX).toInt()
        val baseY = floor(player.posY).toInt()
        val baseZ = floor(player.posZ).toInt()

        for (radius in 0..PLAYER_AIR_SEED_SEARCH_RADIUS) {
            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    for (dz in -radius..radius) {
                        if (kotlin.math.abs(dx) + kotlin.math.abs(dy) + kotlin.math.abs(dz) > radius) continue

                        val x = baseX + dx
                        val y = baseY + dy
                        val z = baseZ + dz

                        if (!isValidY(y)) continue
                        if (!world.blockExists(x, y, z)) continue

                        val block = world.getBlock(x, y, z)
                        if (block.isAir(world, x, y, z)) {
                            return CellPos(x, y, z)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun findEnclosedRoom(world: World, seed: CellPos): RoomCandidate? {
        if (!isAir(world, seed)) return null

        val queue = ArrayDeque<CellPos>()
        val visited = HashSet<CellPos>()

        var minX = seed.x
        var maxX = seed.x
        var minY = seed.y
        var maxY = seed.y
        var minZ = seed.z
        var maxZ = seed.z

        queue.add(seed)
        visited.add(seed)

        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()

            for (direction in Direction.values()) {
                val nx = cell.x + deltaX(direction)
                val ny = cell.y + deltaY(direction)
                val nz = cell.z + deltaZ(direction)

                if (!isValidY(ny)) return null
                if (!world.blockExists(nx, ny, nz)) return null

                val neighbor = CellPos(nx, ny, nz)
                if (visited.contains(neighbor)) continue
                if (!isAir(world, neighbor)) continue

                visited.add(neighbor)
                queue.add(neighbor)

                if (nx < minX) minX = nx
                if (nx > maxX) maxX = nx
                if (ny < minY) minY = ny
                if (ny > maxY) maxY = ny
                if (nz < minZ) minZ = nz
                if (nz > maxZ) maxZ = nz

                if ((maxX - minX + 1) > roomMaxAxisSpanBlocks()) return null
                if ((maxY - minY + 1) > roomMaxAxisSpanBlocks()) return null
                if ((maxZ - minZ + 1) > roomMaxAxisSpanBlocks()) return null
                if (visited.size > roomMaxVolumeBlocks()) return null
            }
        }

        val bounds = RoomBounds(
            minX = minX,
            minY = minY,
            minZ = minZ,
            maxX = maxX,
            maxY = maxY,
            maxZ = maxZ
        )

        return RoomCandidate(
            dimension = world.provider.dimensionId,
            interiorCells = visited,
            bounds = bounds
        )
    }

    private fun findThermalNodesInRoom(candidate: RoomCandidate, thermalNodeCoords: List<Coordinate>): Set<CellPos> {
        val contained = HashSet<CellPos>()

        for (coord in thermalNodeCoords) {
            if (!candidate.bounds.contains(coord.x, coord.y, coord.z)) continue

            val nodeCell = CellPos(coord.x, coord.y, coord.z)
            if (candidate.interiorCells.contains(nodeCell) || touchesRoomAir(candidate.interiorCells, nodeCell)) {
                contained.add(nodeCell)
            }
        }

        return contained
    }

    private fun touchesRoomAir(roomCells: Set<CellPos>, cell: CellPos): Boolean {
        for (direction in Direction.values()) {
            val neighbor = CellPos(
                x = cell.x + deltaX(direction),
                y = cell.y + deltaY(direction),
                z = cell.z + deltaZ(direction),
            )
            if (roomCells.contains(neighbor)) return true
        }
        return false
    }

    private fun registerOrRefreshRoom(candidate: RoomCandidate, thermalNodes: Set<CellPos>) {
        val roomId = createRoomId(candidate)
        val existing = roomsById[roomId]

        if (existing == null) {
            val room = SimulatedRoom(
                id = roomId,
                dimension = candidate.dimension,
                bounds = candidate.bounds,
                interiorCellCount = candidate.interiorCells.size,
                thermalNodeAnchors = thermalNodes,
                temperatureCelsius = 0.0,
                lastSeenTick = tickCounter,
                interiorCells = candidate.interiorCells,
                airHeatCapacityJoulesPerCelsius = candidate.interiorCells.size * AIR_HEAT_CAPACITY_J_PER_BLOCK_C,
                openDoorCount = 0,
                lastDoorScanTick = Long.MIN_VALUE
            )
            roomsById[roomId] = room
            indexRoom(room)

            Eln.logger.info(
                "[room-thermal] room-created id={} dim={} volume={} span={}x{}x{} thermalNodes={} tempC={}",
                room.id.signature,
                room.dimension,
                room.interiorCellCount,
                room.bounds.width,
                room.bounds.height,
                room.bounds.depth,
                room.thermalNodeAnchors.size,
                String.format("%.2f", room.temperatureCelsius)
            )
        } else {
            existing.lastSeenTick = tickCounter
            if (existing.thermalNodeAnchors != thermalNodes) {
                deindexRoomThermalAnchors(existing)
                existing.thermalNodeAnchors = thermalNodes
                indexRoomThermalAnchors(existing)
            }
            if (existing.interiorCells != candidate.interiorCells) {
                deindexRoom(existing)
                existing.interiorCells = candidate.interiorCells
                existing.interiorCellCount = candidate.interiorCells.size
                existing.airHeatCapacityJoulesPerCelsius = candidate.interiorCells.size * AIR_HEAT_CAPACITY_J_PER_BLOCK_C
                existing.lastDoorScanTick = Long.MIN_VALUE
                indexRoom(existing)
            } else {
                existing.interiorCellCount = candidate.interiorCells.size
                existing.airHeatCapacityJoulesPerCelsius = candidate.interiorCells.size * AIR_HEAT_CAPACITY_J_PER_BLOCK_C
            }
            existing.bounds = candidate.bounds
        }
    }

    private fun refreshOpenDoorCount(room: SimulatedRoom) {
        if (tickCounter - room.lastDoorScanTick < ROOM_DOOR_SCAN_INTERVAL_TICKS) return
        room.lastDoorScanTick = tickCounter

        val server = MinecraftServer.getServer() ?: return
        val world = server.worldServerForDimension(room.dimension) ?: return
        if (world.isRemote) return

        room.openDoorCount = countOpenBoundaryDoors(world, room.interiorCells)
    }

    private fun countOpenBoundaryDoors(world: World, interiorCells: Set<CellPos>): Int {
        var openDoors = 0
        val seenDoorBottomCells = HashSet<CellPos>()

        for (cell in interiorCells) {
            for (direction in Direction.values()) {
                val neighbor = CellPos(
                    x = cell.x + deltaX(direction),
                    y = cell.y + deltaY(direction),
                    z = cell.z + deltaZ(direction)
                )
                if (interiorCells.contains(neighbor)) continue
                if (!world.blockExists(neighbor.x, neighbor.y, neighbor.z)) continue

                val block = world.getBlock(neighbor.x, neighbor.y, neighbor.z)
                if (block !is BlockDoor) continue

                val bottomCell = toDoorBottomCell(world, neighbor) ?: continue
                if (!seenDoorBottomCells.add(bottomCell)) continue
                if (isDoorOpen(world, bottomCell)) openDoors++
            }
        }

        return openDoors
    }

    private fun toDoorBottomCell(world: World, cell: CellPos): CellPos? {
        if (!world.blockExists(cell.x, cell.y, cell.z)) return null
        val metadata = world.getBlockMetadata(cell.x, cell.y, cell.z)
        if ((metadata and 8) == 0) return cell
        if (cell.y <= 0) return null
        val bottom = CellPos(cell.x, cell.y - 1, cell.z)
        if (!world.blockExists(bottom.x, bottom.y, bottom.z)) return null
        val bottomBlock = world.getBlock(bottom.x, bottom.y, bottom.z)
        return if (bottomBlock is BlockDoor) bottom else null
    }

    private fun isDoorOpen(world: World, doorBottomCell: CellPos): Boolean {
        if (!world.blockExists(doorBottomCell.x, doorBottomCell.y, doorBottomCell.z)) return false
        val block = world.getBlock(doorBottomCell.x, doorBottomCell.y, doorBottomCell.z)
        if (block !is BlockDoor) return false
        val metadata = world.getBlockMetadata(doorBottomCell.x, doorBottomCell.y, doorBottomCell.z)
        return (metadata and 4) != 0
    }

    private fun createRoomId(candidate: RoomCandidate): RoomId {
        var hash = 1469598103934665603L
        val orderedCells = candidate.interiorCells.sortedWith(
            compareBy<CellPos> { it.x }
                .thenBy { it.y }
                .thenBy { it.z }
        )
        for (cell in orderedCells) {
            hash = hash xor mixCell(cell)
            hash *= 1099511628211L
        }
        return RoomId(
            signature = "${candidate.dimension}:${candidate.bounds.minX},${candidate.bounds.minY},${candidate.bounds.minZ}" +
                ":${candidate.bounds.maxX},${candidate.bounds.maxY},${candidate.bounds.maxZ}:${candidate.interiorCells.size}:$hash"
        )
    }

    private fun mixCell(cell: CellPos): Long {
        var value = 1469598103934665603L
        value = (value xor cell.x.toLong()) * 1099511628211L
        value = (value xor cell.y.toLong()) * 1099511628211L
        value = (value xor cell.z.toLong()) * 1099511628211L
        return value
    }

    private fun isAir(world: World, cell: CellPos): Boolean {
        val block = world.getBlock(cell.x, cell.y, cell.z)
        return block.isAir(world, cell.x, cell.y, cell.z)
    }

    private fun isValidY(y: Int): Boolean = y in 0..255

    private fun deltaX(direction: Direction): Int {
        return when (direction) {
            Direction.XN -> -1
            Direction.XP -> 1
            else -> 0
        }
    }

    private fun deltaY(direction: Direction): Int {
        return when (direction) {
            Direction.YN -> -1
            Direction.YP -> 1
            else -> 0
        }
    }

    private fun deltaZ(direction: Direction): Int {
        return when (direction) {
            Direction.ZN -> -1
            Direction.ZP -> 1
            else -> 0
        }
    }

    private data class CellPos(val x: Int, val y: Int, val z: Int)
    private data class AnchorKey(val dimension: Int, val cell: CellPos)
    private data class ExchangeDebugSnapshot(val roomId: RoomId, val loadToRoomWatts: Double, val tick: Long)

    private fun rememberExchangeSample(dimension: Int, x: Int, y: Int, z: Int, roomId: RoomId, loadToRoomWatts: Double) {
        val byAnchor = recentExchangeByAnchorByDimension.getOrPut(dimension) { HashMap() }
        byAnchor[CellPos(x, y, z)] = ExchangeDebugSnapshot(
            roomId = roomId,
            loadToRoomWatts = loadToRoomWatts,
            tick = tickCounter
        )
    }

    private fun maybeLogExchange(
        dimension: Int,
        x: Int,
        y: Int,
        z: Int,
        roomId: RoomId,
        loadToRoomWatts: Double,
        loadTemperatureDeltaCelsius: Double,
        roomTemperatureCelsius: Double
    ) {
        if (abs(loadToRoomWatts) < EXCHANGE_LOG_MIN_POWER_W) return
        val key = AnchorKey(dimension, CellPos(x, y, z))
        val lastTick = lastExchangeLogTickByAnchor[key] ?: Long.MIN_VALUE
        if (tickCounter - lastTick < EXCHANGE_LOG_INTERVAL_TICKS) return
        lastExchangeLogTickByAnchor[key] = tickCounter
        Eln.logger.info(
            "[room-thermal] load-room-exchange dim={} coord={},{},{} room={} pW={} loadDeltaC={} roomDeltaC={}",
            dimension,
            x,
            y,
            z,
            roomId.signature,
            String.format("%.2f", loadToRoomWatts),
            String.format("%.2f", loadTemperatureDeltaCelsius),
            String.format("%.2f", roomTemperatureCelsius)
        )
    }

    private fun roomIndexForDimension(dimension: Int): MutableMap<CellPos, RoomId> {
        return roomByInteriorCellByDimension.getOrPut(dimension) { HashMap() }
    }

    private fun thermalAnchorIndexForDimension(dimension: Int): MutableMap<CellPos, RoomId> {
        return roomByThermalAnchorByDimension.getOrPut(dimension) { HashMap() }
    }

    private fun indexRoom(room: SimulatedRoom) {
        val index = roomIndexForDimension(room.dimension)
        for (cell in room.interiorCells) {
            index[cell] = room.id
        }
        indexRoomThermalAnchors(room)
    }

    private fun deindexRoom(room: SimulatedRoom) {
        val index = roomByInteriorCellByDimension[room.dimension] ?: return
        for (cell in room.interiorCells) {
            if (index[cell] == room.id) {
                index.remove(cell)
            }
        }
        deindexRoomThermalAnchors(room)
    }

    private fun indexRoomThermalAnchors(room: SimulatedRoom) {
        val index = thermalAnchorIndexForDimension(room.dimension)
        for (cell in room.thermalNodeAnchors) {
            index[cell] = room.id
        }
    }

    private fun deindexRoomThermalAnchors(room: SimulatedRoom) {
        val index = roomByThermalAnchorByDimension[room.dimension] ?: return
        for (cell in room.thermalNodeAnchors) {
            if (index[cell] == room.id) {
                index.remove(cell)
            }
        }
    }

    private fun collectRoomIdsAtOrAdjacent(dimension: Int, center: CellPos, out: MutableSet<RoomId>) {
        val index = roomByInteriorCellByDimension[dimension] ?: return
        index[center]?.let(out::add)
        for (direction in Direction.values()) {
            val neighbor = CellPos(
                center.x + deltaX(direction),
                center.y + deltaY(direction),
                center.z + deltaZ(direction)
            )
            index[neighbor]?.let(out::add)
        }
    }

    private fun boundsToNbt(bounds: RoomBounds): NBTTagCompound {
        return NBTTagCompound().apply {
            setInteger(NBT_MIN_X, bounds.minX)
            setInteger(NBT_MIN_Y, bounds.minY)
            setInteger(NBT_MIN_Z, bounds.minZ)
            setInteger(NBT_MAX_X, bounds.maxX)
            setInteger(NBT_MAX_Y, bounds.maxY)
            setInteger(NBT_MAX_Z, bounds.maxZ)
        }
    }

    private fun boundsFromNbt(tag: NBTTagCompound): RoomBounds {
        return RoomBounds(
            minX = tag.getInteger(NBT_MIN_X),
            minY = tag.getInteger(NBT_MIN_Y),
            minZ = tag.getInteger(NBT_MIN_Z),
            maxX = tag.getInteger(NBT_MAX_X),
            maxY = tag.getInteger(NBT_MAX_Y),
            maxZ = tag.getInteger(NBT_MAX_Z)
        )
    }

    private fun encodeCells(cells: Set<CellPos>): IntArray {
        val out = IntArray(cells.size * 3)
        var index = 0
        for (cell in cells) {
            out[index++] = cell.x
            out[index++] = cell.y
            out[index++] = cell.z
        }
        return out
    }

    private fun decodeCells(raw: IntArray): Set<CellPos> {
        if (raw.isEmpty() || raw.size % 3 != 0) return emptySet()
        val out = HashSet<CellPos>(raw.size / 3)
        var i = 0
        while (i + 2 < raw.size) {
            out.add(CellPos(raw[i], raw[i + 1], raw[i + 2]))
            i += 3
        }
        return out
    }

    private fun readRoomFromNbt(roomTag: NBTTagCompound, expectedDimension: Int): SimulatedRoom? {
        val roomDimension = roomTag.getInteger(NBT_DIM)
        if (roomDimension != expectedDimension) return null

        val boundsTag = roomTag.getCompoundTag(NBT_BOUNDS)
        if (boundsTag.hasNoTags()) return null

        val bounds = boundsFromNbt(boundsTag)
        val interiorCells = decodeCells(roomTag.getIntArray(NBT_INTERIOR_CELLS))
        if (interiorCells.isEmpty()) return null
        if (interiorCells.size > roomMaxVolumeBlocks()) return null

        val thermalNodes = decodeCells(roomTag.getIntArray(NBT_THERMAL_NODES))
        val roomId = RoomId(roomTag.getString(NBT_ID).ifBlank {
            val candidate = RoomCandidate(
                dimension = roomDimension,
                interiorCells = interiorCells,
                bounds = bounds
            )
            createRoomId(candidate).signature
        })

        return SimulatedRoom(
            id = roomId,
            dimension = roomDimension,
            bounds = bounds,
            interiorCellCount = roomTag.getInteger(NBT_INTERIOR_COUNT).takeIf { it > 0 } ?: interiorCells.size,
            thermalNodeAnchors = thermalNodes,
            temperatureCelsius = roomTag.getDouble(NBT_TEMPERATURE),
            lastSeenTick = tickCounter,
            interiorCells = interiorCells,
            airHeatCapacityJoulesPerCelsius = (roomTag.getInteger(NBT_INTERIOR_COUNT).takeIf { it > 0 } ?: interiorCells.size) * AIR_HEAT_CAPACITY_J_PER_BLOCK_C,
            openDoorCount = 0,
            lastDoorScanTick = Long.MIN_VALUE
        )
    }

    private data class RoomCandidate(
        val dimension: Int,
        val interiorCells: Set<CellPos>,
        val bounds: RoomBounds
    )

    private data class RoomId(val signature: String)

    private data class RoomBounds(
        val minX: Int,
        val minY: Int,
        val minZ: Int,
        val maxX: Int,
        val maxY: Int,
        val maxZ: Int
    ) {
        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1
        val depth: Int get() = maxZ - minZ + 1
        val surfaceAreaEstimate: Int get() = 2 * (width * height + width * depth + height * depth)

        fun contains(x: Int, y: Int, z: Int): Boolean {
            return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
        }

        fun expanded(amount: Int): RoomBounds {
            return RoomBounds(
                minX = minX - amount,
                minY = minY - amount,
                minZ = minZ - amount,
                maxX = maxX + amount,
                maxY = maxY + amount,
                maxZ = maxZ + amount
            )
        }
    }

    private data class SimulatedRoom(
        val id: RoomId,
        val dimension: Int,
        var bounds: RoomBounds,
        var interiorCellCount: Int,
        var thermalNodeAnchors: Set<CellPos>,
        var temperatureCelsius: Double,
        var lastSeenTick: Long,
        var interiorCells: Set<CellPos>,
        var airHeatCapacityJoulesPerCelsius: Double,
        var openDoorCount: Int,
        var lastDoorScanTick: Long
    )
}
