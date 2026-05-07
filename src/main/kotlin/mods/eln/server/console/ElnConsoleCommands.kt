package mods.eln.server.console

import mods.eln.Eln
import mods.eln.gridnode.GridElement
import mods.eln.gridnode.GridLink
import mods.eln.gridnode.GridSwitchElement
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor
import mods.eln.gridnode.electricalpole.ElectricalPoleElement
import mods.eln.gridnode.transformer.GridTransformerElement
import mods.eln.mechanical.ShaftElement
import mods.eln.environment.BiomeClimateService
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.item.lampitem.LampLists
import mods.eln.misc.Coordinate
import mods.eln.misc.FC
import mods.eln.misc.Version
import mods.eln.node.NodeBase
import mods.eln.node.NodeManager
import mods.eln.node.GhostNode
import mods.eln.node.simple.SimpleNode
import mods.eln.node.six.SixNode
import mods.eln.node.transparent.TransparentNode
import mods.eln.server.ElnDestroyHelper
import mods.eln.server.console.ElnConsoleCommands.Companion.boolToStr
import mods.eln.server.console.ElnConsoleCommands.Companion.cprint
import mods.eln.server.console.ElnConsoleCommands.Companion.getArgBool
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayerMP
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.LinkedHashSet
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

private data class ZoneBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int
)

private fun parseZoneBounds(ics: EntityPlayerMP, args: List<String>, commandName: String): ZoneBounds? {
    if (args.size == 1) {
        val radius = args[0].toIntOrNull()
        if (radius == null || radius < 0) {
            cprint(ics, "${FC.BRIGHT_RED}Invalid radius '${args[0]}'", indent = 1)
            cprint(ics, "${FC.BRIGHT_YELLOW}Usage: /eln $commandName <radius> or /eln $commandName x1 y1 z1 x2 y2 z2", indent = 1)
            return null
        }
        val x = ics.playerCoordinates.posX
        val y = ics.playerCoordinates.posY
        val z = ics.playerCoordinates.posZ
        return ZoneBounds(x - radius, x + radius, y - radius, y + radius, z - radius, z + radius)
    }
    if (args.size != 6) {
        cprint(ics, "${FC.BRIGHT_YELLOW}Usage: /eln $commandName <radius> or /eln $commandName x1 y1 z1 x2 y2 z2", indent = 1)
        return null
    }
    val coords = IntArray(6)
    for (i in 0 until 6) {
        val value = args[i].toIntOrNull()
        if (value == null) {
            cprint(ics, "${FC.BRIGHT_RED}Invalid coordinate '${args[i]}'", indent = 1)
            return null
        }
        coords[i] = value
    }
    return ZoneBounds(
        min(coords[0], coords[3]),
        max(coords[0], coords[3]),
        min(coords[1], coords[4]),
        max(coords[1], coords[4]),
        min(coords[2], coords[5]),
        max(coords[2], coords[5])
    )
}

private fun saveConfigPath(path: String, value: Any): String =
    Eln.config.writePath(path, value.toString())

private fun isConfigPathPattern(path: String): Boolean =
    path.any { it in "*+?[](){}|^$\\" }

class ElnLsCommand: IConsoleCommand {
    override val name = "ls"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        cprint(ics, "${FC.WHITE}Eln Command list:")
        cprint(ics, ElnConsoleCommandList.joinToString(", ") { it.name }, indent = 1)
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Lists all available commands.", indent = 1)
        cprint(ics, "")
        cprint(ics, "No input parameters.", indent = 1)
        cprint(ics, "")
    }
}

class ElnConfigCommand : IConsoleCommand {
    override val name = "config"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        when (args.getOrNull(0)?.lowercase()) {
            "get" -> {
                val path = args.getOrNull(1)
                if (args.size != 2 || path.isNullOrBlank()) {
                    cprint(ics, "Usage: /eln config get <path>", indent = 1)
                    return
                }
                try {
                    if (isConfigPathPattern(path)) {
                        val values = Eln.config.readPathsMatching(path)
                        values.forEach { (matchedPath, value) ->
                            cprint(ics, "${FC.BRIGHT_GREEN}$matchedPath = $value", indent = 1)
                        }
                    } else {
                        val value = Eln.config.readPath(path)
                        if (value == null) {
                            cprint(ics, "${FC.BRIGHT_RED}Unknown config path '$path'.", indent = 1)
                            return
                        }
                        cprint(ics, "${FC.BRIGHT_GREEN}$path = $value", indent = 1)
                    }
                } catch (e: IllegalArgumentException) {
                    cprint(ics, "${FC.BRIGHT_RED}${e.message}", indent = 1)
                }
            }
            "set" -> {
                val path = args.getOrNull(1)
                if (args.size < 3 || path.isNullOrBlank()) {
                    cprint(ics, "Usage: /eln config set <path> <value>", indent = 1)
                    return
                }
                val rawValue = args.drop(2).joinToString(" ")
                try {
                    if (isConfigPathPattern(path)) {
                        val matchedPaths = Eln.config.writePathsMatching(path, rawValue)
                        if (matchedPaths.contains("lighting.lamps")) { // This is only true if attempting to assign an invalid nominal life to lamp(s)
                            cprint(ics, "${FC.BRIGHT_RED}${BoilerplateLampData.SET_NOMINAL_LIFE_ERROR_MESSAGE}", indent = 1)
                            return
                        } else {
                            cprint(ics, "${FC.BRIGHT_GREEN}Updated ${matchedPaths.size} config paths.", indent = 1)
                            matchedPaths.forEach { matchedPath ->
                                cprint(ics, "$matchedPath = ${Eln.config.readPath(matchedPath)}", indent = 2)
                            }
                        }
                    } else {
                        val writtenValue = Eln.config.writePath(path, rawValue)
                        if (writtenValue == "lighting.lamps") { // This is only true if attempting to assign an invalid nominal life to lamp(s)
                            cprint(ics, "${FC.BRIGHT_RED}${BoilerplateLampData.SET_NOMINAL_LIFE_ERROR_MESSAGE}", indent = 1)
                            return
                        } else cprint(ics, "${FC.BRIGHT_GREEN}$path = $writtenValue", indent = 1)
                    }
                    cprint(ics, "Changes saved to config/eln/eln.json.", indent = 1)
                } catch (e: IllegalArgumentException) {
                    cprint(ics, "${FC.BRIGHT_RED}${e.message}", indent = 1)
                }
            }
            "list" -> {
                val prefix = args.getOrNull(1)
                if (args.size > 2) {
                    cprint(ics, "Usage: /eln config list [prefix]", indent = 1)
                    return
                }
                val paths = Eln.config.listPaths(prefix)
                if (paths.isEmpty()) {
                    cprint(ics, "${FC.BRIGHT_YELLOW}No config paths matched.", indent = 1)
                    return
                }
                cprint(ics, "${FC.WHITE}Config paths:", indent = 1)
                paths.forEach { cprint(ics, it, indent = 2) }
            }
            else -> {
                cprint(ics, "Usage: /eln config get <path>", indent = 1)
                cprint(ics, "Usage: /eln config set <path> <value>", indent = 1)
                cprint(ics, "Usage: /eln config list [prefix]", indent = 1)
            }
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Read or modify any JSON-backed Electrical Age config path.", indent = 1)
        cprint(ics, "This command updates the cache and writes back to config/eln/eln.json.", indent = 1)
        cprint(ics, "", indent = 1)
        cprint(ics, "Usage: /eln config get <path>", indent = 1)
        cprint(ics, "Usage: /eln config get <path-regex>", indent = 1)
        cprint(ics, "Usage: /eln config set <path> <value>", indent = 1)
        cprint(ics, "Usage: /eln config set <path-regex> <value>", indent = 1)
        cprint(ics, "Usage: /eln config list [prefix]", indent = 1)
    }

    override fun getTabCompletion(args: List<String>): List<String> {
        val subcommands = listOf("get", "set", "list")
        return when (args.size) {
            0 -> subcommands
            1 -> subcommands.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> {
                val subcommand = args[0].lowercase()
                if (subcommand !in subcommands) return listOf()
                Eln.config.listPaths(args[1].takeIf { it.isNotBlank() }).take(100)
            }
            else -> listOf()
        }
    }
}

open class ElnAboutCommand: IConsoleCommand {
    override val name = "about"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        cprint(ics, "${FC.WHITE}About Electrical Age:")
        cprint(ics,  "${FC.BRIGHT_GREY}Authors: ${FC.DARK_GREY}${Eln.AUTHORS.joinToString(", ")}")
        cprint(ics, "${FC.BRIGHT_GREY}Version: ${FC.DARK_GREY}" + Version.simpleVersionName)
        if (Version.GIT_REVISION.isNotEmpty()) {
            cprint(ics, "${FC.BRIGHT_GREY}Git Build Version: ${FC.DARK_GREY}${Version.GIT_REVISION}")
            cprint(ics, "${FC.BRIGHT_BLUE}[GitHub Link to Git Version]", "https://github.com/age-series/ElectricalAge/commit/" + Version.GIT_REVISION)
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Returns useful information about Electrical Age.", indent = 1)
        cprint(ics, "")
        cprint(ics, "No input parameters.", indent = 1)
        cprint(ics, "")
    }
}

// Since we tell people to run /eln version a lot, might as well have this alias.
class ElnVersionCommand: ElnAboutCommand() { override val name = "version"}
class ElnNewWindDirectionCommand: IConsoleCommand {
    override val name = "newWind"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        Eln.wind.newWindTarget()
        cprint(ics, "New random wind amplitude target: ${Eln.wind.targetNotFiltered}")
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Changes progressively the wind to another target amplitude.", indent = 1)
        cprint(ics, "Changes stored into the map.", indent = 1)
        cprint(ics, "")
        cprint(ics, "No input parameters.", indent = 1)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)
}

class ElnMatrixCommand: IConsoleCommand {
    override val name = "matrix"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        cprint(ics, FC.BRIGHT_YELLOW + "Dumping all circuits. This will take a moment on large worlds...")
        println("Dumping all circuits. This will take a moment on large worlds...")
        var dumpSubSystems = ""
        val ssc = Eln.simulator.mna.systems.size
        var ct = 0
        for (s in Eln.simulator.mna.systems) {
            ct += s.component.size
            dumpSubSystems += """
                    $s

                    """.trimIndent()
        }
        val f = File("elnDumpSubSystems.txt")
        try {
            val w = BufferedWriter(FileWriter(f))
            w.write(dumpSubSystems)
            w.flush()
            w.close()
        } catch (e: Exception) {
            println("Failed to write to " + f.absolutePath + " because " + e)
        }
        if (ssc == 1) {
            cprint(ics, FC.BRIGHT_YELLOW + "There is 1 subsystem.")
        } else {
            cprint(ics, FC.BRIGHT_YELLOW + "There are " + ssc + " subsystems.")
        }
        cprint(ics, FC.BRIGHT_YELLOW + "Average subsystem size: " + ct / ssc)
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Dumps everything Electrical Age knows about circuit topologies into a file called elnDumpMatrix.txt, which can then be consumed by a nice little program that I wrote that can give visualizations of all of the node graphs that the MNA takes in to calculate circuit values.", indent = 1)
        cprint(ics, "[https://github.com/jrddunbr/eln-matrix-viz]", "https://github.com/jrddunbr/eln-matrix-viz")
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)
}

class ElnManCommand: IConsoleCommand {
    override val name = "man"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        when (args.size) {
            0 -> {
                cprint(ics, "Returns help for a given command.", indent = 1)
                cprint(ics, "")
                cprint(ics, "Parameters :", indent = 1)
                cprint(ics, "@0:string : Command name to get documentation.", indent = 2)
                cprint(ics, "")
            }
            else -> {
                val command = findConsoleCommand(args[0])
                if (command == null) {
                    cprint(ics, "Sorry, but no man page was found for ${args[0]}", indent = 1)
                } else {
                    command.getManPage(ics, args.drop(1))
                }
            }
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Returns help for a given command.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters :", indent = 1)
        cprint(ics, "@0:string : Command name to get documentation.", indent = 2)
        cprint(ics, "")
    }

    override fun getTabCompletion(args: List<String>): List<String> {
        return if (args.isEmpty() || args[0] == "") {
            ElnConsoleCommandList.map {it.name}.toMutableList()
        } else {
            ElnConsoleCommandList.filter {it.name.startsWith(args[0], ignoreCase = true)}.map{it.name}.toMutableList()
        }
    }
}

class ElnZoneDumpCommand : IConsoleCommand {
    override val name = "zonedump"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (ics !is EntityPlayerMP) {
            cprint(ics, "${FC.BRIGHT_RED}This command can only be run by a player.", indent = 1)
            return
        }
        val bounds = parseZoneBounds(ics, args, name) ?: return
        val minX = bounds.minX
        val maxX = bounds.maxX
        val minY = bounds.minY
        val maxY = bounds.maxY
        val minZ = bounds.minZ
        val maxZ = bounds.maxZ

        val world = ics.worldObj
        val dim = world.provider.dimensionId
        val rangeDescription = "($minX,$minY,$minZ) -> ($maxX,$maxY,$maxZ) in dim $dim"

        val nodeManager = NodeManager.instance
        val coordToNode = HashMap<Coordinate, NodeBase>()
        val nodesInZone = if (nodeManager != null) {
            nodeManager.nodeList.filter {
                val c = it.coordinate
                c.dimension == dim &&
                    c.x in minX..maxX &&
                    c.y in minY..maxY &&
                    c.z in minZ..maxZ
            }
        } else emptyList()
        nodesInZone.forEach {
            coordToNode[Coordinate(it.coordinate)] = it
        }

        val warnings = mutableListOf<String>()
        val builder = StringBuilder()
        builder.append("Zone dump for $rangeDescription\n")
        builder.append("Player: ${ics.commandSenderName}\n")
        builder.append("Generated: ${Date()}\n\n")

        builder.append("Nodes:\n")
        if (nodesInZone.isEmpty()) {
            builder.append("  <none>\n")
        } else {
            for (node in nodesInZone) {
                val coord = node.coordinate
                builder.append("  ${coord}: ${node.javaClass.simpleName}")
                if (node is TransparentNode) {
                    builder.append(" element=${node.element?.javaClass?.simpleName}")
                } else if (node is SixNode) {
                    builder.append(" sixNode")
                }
                builder.append('\n')

                val expectedBlock = when (node) {
                    is SixNode -> Eln.sixNodeBlock
                    is TransparentNode -> Eln.transparentNodeBlock
                    is GhostNode -> Eln.ghostBlock
                    else -> null
                }
                val actualBlock = world.getBlock(coord.x, coord.y, coord.z)
                if (expectedBlock != null && actualBlock != expectedBlock) {
                    warnings.add("Node ${coord} (${node.javaClass.simpleName}) expected ${expectedBlock.unlocalizedName} but found ${actualBlock.unlocalizedName}")
                }
            }
        }

        builder.append("\nBlocks:\n")
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val block = world.getBlock(x, y, z)
                    val meta = world.getBlockMetadata(x, y, z)
                    val tile = world.getTileEntity(x, y, z)
                    builder.append("  ($x,$y,$z): ${block.unlocalizedName} meta=$meta tile=${tile?.javaClass?.simpleName}\n")
                    val coord = Coordinate(x, y, z, dim)
                    if ((block == Eln.sixNodeBlock || block == Eln.transparentNodeBlock || block == Eln.ghostBlock) && !coordToNode.containsKey(coord)) {
                        warnings.add("Block ${block.unlocalizedName} at $coord has no registered node")
                    }
                }
            }
        }

        if (warnings.isEmpty()) {
            builder.append("\nNo ghost nodes detected.\n")
        } else {
            builder.append("\nWarnings:\n")
            warnings.forEach { builder.append("  - ").append(it).append('\n') }
        }

        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(Date())
        val file = File("zonedump-$timestamp.txt")
        try {
            BufferedWriter(FileWriter(file)).use { it.write(builder.toString()) }
            cprint(ics, "${FC.BRIGHT_GREEN}Zone dump written to ${file.absolutePath}", indent = 1)
        } catch (e: Exception) {
            cprint(ics, "${FC.BRIGHT_RED}Failed to write zone dump: ${e.message}", indent = 1)
        }
        if (warnings.isNotEmpty()) {
            warnings.forEach { cprint(ics, "${FC.BRIGHT_RED}$it", indent = 1) }
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Dump Eln nodes and world blocks in a zone to a zonedump-<timestamp>.txt file.", indent = 1)
        cprint(ics, "Usage: /eln zonedump <radius>", indent = 1)
        cprint(ics, "Usage: /eln zonedump x1 y1 z1 x2 y2 z2", indent = 1)
        cprint(ics, "")
    }
}

class ElnZoneCleanCommand : IConsoleCommand {
    override val name = "zoneclean"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (ics !is EntityPlayerMP) {
            cprint(ics, "${FC.BRIGHT_RED}This command can only be run by a player.", indent = 1)
            return
        }
        val bounds = parseZoneBounds(ics, args, name) ?: return
        val minX = bounds.minX
        val maxX = bounds.maxX
        val minY = bounds.minY
        val maxY = bounds.maxY
        val minZ = bounds.minZ
        val maxZ = bounds.maxZ

        val world = ics.worldObj
        val dim = world.provider.dimensionId
        val nodeManager = NodeManager.instance
        val nodes = nodeManager?.nodeList ?: emptyList()
        val nodesToProcess = nodes.filter {
            val c = it.coordinate
            c.dimension == dim &&
                c.x in minX..maxX &&
                c.y in minY..maxY &&
                c.z in minZ..maxZ
        }
        val coordKeyedNodes = HashMap<Coordinate, NodeBase>()
        nodesToProcess.forEach { coordKeyedNodes[Coordinate(it.coordinate)] = it }

        var nodesRemoved = 0
        for (node in nodesToProcess) {
            val coord = node.coordinate
            val expectedBlock = when (node) {
                is SixNode -> Eln.sixNodeBlock
                is TransparentNode -> Eln.transparentNodeBlock
                is GhostNode -> Eln.ghostBlock
                else -> null
            }
            val actualBlock = world.getBlock(coord.x, coord.y, coord.z)
            val needsRemoval =
                node is GhostNode ||
                    expectedBlock == null ||
                    actualBlock != expectedBlock
            if (needsRemoval) {
                try {
                    node.onBreakBlock()
                } catch (e: Exception) {
                    println("zoneclean: onBreakBlock failed for $coord : ${e.message}")
                }
                nodeManager?.removeNode(node)
                if (expectedBlock != null && actualBlock == expectedBlock) {
                    world.setBlockToAir(coord.x, coord.y, coord.z)
                }
                nodesRemoved++
            }
        }

        var orphanBlocks = 0
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val block = world.getBlock(x, y, z)
                    val isNodeBlock = block == Eln.sixNodeBlock || block == Eln.transparentNodeBlock || block == Eln.ghostBlock
                    if (!isNodeBlock) continue
                    val coord = Coordinate(x, y, z, dim)
                    if (coordKeyedNodes.containsKey(coord)) continue
                    if (ElnDestroyHelper.clearElnBlock(world, x, y, z)) {
                        orphanBlocks++
                    }
                }
            }
        }

        cprint(
            ics,
            "${FC.BRIGHT_GREEN}Zone clean complete: removed $nodesRemoved ghost nodes and cleared $orphanBlocks orphan blocks.",
            indent = 1
        )
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Removes ghost nodes and orphaned Eln blocks within a rectangular zone.", indent = 1)
        cprint(ics, "Usage: /eln zoneclean <radius>", indent = 1)
        cprint(ics, "Usage: /eln zoneclean x1 y1 z1 x2 y2 z2", indent = 1)
        cprint(ics, "Blocks removed this way must be rebuilt manually.", indent = 1)
        cprint(ics, "")
    }
}

class ElnZoneRemoveCommand : IConsoleCommand {
    override val name = "zoneremove"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (ics !is EntityPlayerMP) {
            cprint(ics, "${FC.BRIGHT_RED}This command can only be run by a player.", indent = 1)
            return
        }
        val bounds = parseZoneBounds(ics, args, name) ?: return
        val minX = bounds.minX
        val maxX = bounds.maxX
        val minY = bounds.minY
        val maxY = bounds.maxY
        val minZ = bounds.minZ
        val maxZ = bounds.maxZ

        val world = ics.worldObj
        val dim = world.provider.dimensionId
        val nodeManager = NodeManager.instance
        if (nodeManager == null) {
            cprint(ics, "${FC.BRIGHT_RED}Node manager unavailable, cannot run zoneremove.", indent = 1)
            return
        }
        val targetNodes = nodeManager.nodeList.filter {
            val c = it.coordinate
            c.dimension == dim &&
                c.x in minX..maxX &&
                c.y in minY..maxY &&
                c.z in minZ..maxZ
        }
        val ownerNotifications = LinkedHashSet<Coordinate>()
        var nodesRemoved = 0
        for (node in targetNodes) {
            val coord = node.coordinate
            var removed = false
            if (node is GhostNode) {
                val ghost = Eln.ghostManager.getGhost(coord)
                if (ghost != null) {
                    removed = try {
                        ghost.breakBlock()
                        if (isOnBoundary(coord, minX, maxX, minY, maxY, minZ, maxZ)) {
                            ghost.observatorCoordonate?.let { ownerNotifications.add(Coordinate(it)) }
                        }
                        true
                    } catch (e: Exception) {
                        println("zoneremove: ghost break failed at $coord : ${e.message}")
                        false
                    }
                }
            }
            if (!removed) {
                try {
                    node.onBreakBlock()
                    removed = true
                } catch (e: Exception) {
                    println("zoneremove: onBreakBlock failed for $coord : ${e.message}")
                }
            }
            if (removed) {
                nodesRemoved++
            }
        }

        var blocksCleared = 0
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    if (ElnDestroyHelper.clearElnBlock(world, x, y, z)) {
                        blocksCleared++
                    }
                }
            }
        }

        cprint(
            ics,
            "${FC.BRIGHT_GREEN}Zone remove complete: removed $nodesRemoved nodes, cleared $blocksCleared blocks, notified ${ownerNotifications.size} owners.",
            indent = 1
        )
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Removes all Eln nodes and blocks within a rectangular zone.", indent = 1)
        cprint(ics, "Ghost nodes on the zone boundary notify their owners before removal.", indent = 1)
        cprint(ics, "Usage: /eln zoneremove <radius>", indent = 1)
        cprint(ics, "Usage: /eln zoneremove x1 y1 z1 x2 y2 z2", indent = 1)
        cprint(ics, "")
    }

    private fun isOnBoundary(
        coord: Coordinate,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        minZ: Int,
        maxZ: Int
    ): Boolean {
        return coord.x == minX || coord.x == maxX ||
            coord.y == minY || coord.y == maxY ||
            coord.z == minZ || coord.z == maxZ
    }

}

class ElnZoneDestroyCommand : IConsoleCommand {
    override val name = "zonedestroy"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (ics !is EntityPlayerMP) {
            cprint(ics, "${FC.BRIGHT_RED}This command can only be run by a player.", indent = 1)
            return
        }
        val bounds = parseZoneBounds(ics, args, name) ?: return
        val world = ics.worldObj
        val nodeManager = NodeManager.instance
        if (nodeManager == null) {
            cprint(ics, "${FC.BRIGHT_RED}Node manager unavailable, cannot run zonedestroy.", indent = 1)
            return
        }
        val summary = ElnDestroyHelper.destroyWithinBounds(
            world = world,
            nodeManager = nodeManager,
            minX = bounds.minX,
            maxX = bounds.maxX,
            minY = bounds.minY,
            maxY = bounds.maxY,
            minZ = bounds.minZ,
            maxZ = bounds.maxZ,
            player = ics
        )

        cprint(
            ics,
            "${FC.BRIGHT_GREEN}Zone destroy complete: destroyed ${summary.nodesDestroyed} nodes and cleared ${summary.blocksCleared} blocks without dropping items.",
            indent = 1
        )
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Destroys all Eln nodes and blocks within a rectangular zone without dropping items.", indent = 1)
        cprint(ics, "Usage: /eln zonedestroy <radius>", indent = 1)
        cprint(ics, "Usage: /eln zonedestroy x1 y1 z1 x2 y2 z2", indent = 1)
        cprint(ics, "")
    }
}

class ElnStopShaftCommand : IConsoleCommand {
    override val name = "stop-shaft"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (ics !is EntityPlayerMP) {
            cprint(ics, "${FC.BRIGHT_RED}This command can only be run by a player.", indent = 1)
            return
        }
        if (args.isNotEmpty()) {
            cprint(ics, "${FC.BRIGHT_YELLOW}Usage: /eln stop-shaft", indent = 1)
            return
        }

        val radius = 3.0
        val radiusSq = radius * radius
        val nodeManager = NodeManager.instance
        if (nodeManager == null) {
            cprint(ics, "${FC.BRIGHT_RED}Node manager unavailable.", indent = 1)
            return
        }

        val world = ics.worldObj
        val dim = world.provider.dimensionId
        var bestDistanceSq = Double.MAX_VALUE
        var bestShaftElement: ShaftElement? = null

        nodeManager.nodeList.forEach { node ->
            if (node.coordinate.dimension != dim) return@forEach
            if (node !is TransparentNode) return@forEach

            val shaftElement = node.element as? ShaftElement ?: return@forEach
            val dx = (node.coordinate.x + 0.5) - ics.posX
            val dy = (node.coordinate.y + 0.5) - ics.posY
            val dz = (node.coordinate.z + 0.5) - ics.posZ
            val distanceSq = dx * dx + dy * dy + dz * dz
            if (distanceSq <= radiusSq && distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq
                bestShaftElement = shaftElement
            }
        }

        if (bestShaftElement == null) {
            cprint(ics, "${FC.BRIGHT_YELLOW}No shaft network found within ${radius.toInt()} blocks.", indent = 1)
            return
        }

        val network = bestShaftElement!!
            .shaftConnectivity
            .asSequence()
            .mapNotNull { bestShaftElement!!.getShaft(it) }
            .firstOrNull()

        if (network == null) {
            cprint(ics, "${FC.BRIGHT_YELLOW}Nearest shaft has no connected network.", indent = 1)
            return
        }

        network.energy = 0.0
        network.elements.forEach { it.needPublish() }

        val coord = bestShaftElement!!.coordonate()
        cprint(
            ics,
            "${FC.BRIGHT_GREEN}Stopped shaft network at (${coord.x}, ${coord.y}, ${coord.z}) in dimension ${coord.dimension}. Energy set to 0 J.",
            indent = 1
        )
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Sets the nearest shaft network energy to 0 J within a 3-block radius.", indent = 1)
        cprint(ics, "Usage: /eln stop-shaft", indent = 1)
        cprint(ics, "")
    }
}

class ElnResetAmbientTempsCommand : IConsoleCommand {
    override val name = "resetAmbientTemps"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (ics !is EntityPlayerMP) {
            cprint(ics, "${FC.BRIGHT_RED}This command can only be run by a player.", indent = 1)
            return
        }
        if (args.size != 1) {
            cprint(ics, "${FC.BRIGHT_YELLOW}Usage: /eln resetAmbientTemps <range 1..32>", indent = 1)
            return
        }

        val range = args[0].toIntOrNull()
        if (range == null || range < 1 || range > 32) {
            cprint(ics, "${FC.BRIGHT_RED}Range must be an integer between 1 and 32.", indent = 1)
            return
        }

        val nodeManager = NodeManager.instance
        if (nodeManager == null) {
            cprint(ics, "${FC.BRIGHT_RED}Node manager unavailable.", indent = 1)
            return
        }

        val rangeSq = range.toDouble() * range.toDouble()
        val dim = ics.worldObj.provider.dimensionId
        var devicesTouched = 0
        var thermalLoadsReset = 0
        var minAmbientC = Double.POSITIVE_INFINITY
        var maxAmbientC = Double.NEGATIVE_INFINITY

        nodeManager.nodeList.forEach { node ->
            if (node.coordinate.dimension != dim) return@forEach

            val dx = (node.coordinate.x + 0.5) - ics.posX
            val dy = (node.coordinate.y + 0.5) - ics.posY
            val dz = (node.coordinate.z + 0.5) - ics.posZ
            val distanceSq = dx * dx + dy * dy + dz * dz
            if (distanceSq > rangeSq) return@forEach

            val coordinate = node.coordinate
            val world = coordinate.world()
            val targetTempC = BiomeClimateService.sample(world, coordinate.x, coordinate.y, coordinate.z).temperatureCelsius

            var changedForNode = 0
            when (node) {
                is TransparentNode -> {
                    val element = node.element
                    element?.thermalLoadList?.forEach { load ->
                        load.temperatureCelsius = 0.0
                        changedForNode++
                    }
                }
                is SixNode -> {
                    node.sideElementList.filterNotNull().forEach { element ->
                        element.thermalLoadList.forEach { load ->
                            load.temperatureCelsius = 0.0
                            changedForNode++
                        }
                    }
                }
                is SimpleNode -> {
                    node.thermalLoadList.forEach { load ->
                        load.temperatureCelsius = 0.0
                        changedForNode++
                    }
                }
            }

            if (changedForNode > 0) {
                thermalLoadsReset += changedForNode
                devicesTouched++
                minAmbientC = min(minAmbientC, targetTempC)
                maxAmbientC = max(maxAmbientC, targetTempC)
                node.needPublish = true
            }
        }

        if (thermalLoadsReset == 0) {
            cprint(
                ics,
                "${FC.BRIGHT_YELLOW}No thermal loads found within range $range.",
                indent = 1
            )
            return
        }

        cprint(
            ics,
            "${FC.BRIGHT_GREEN}Reset $thermalLoadsReset thermal loads on $devicesTouched devices to local ambient temperatures within range $range (${String.format(Locale.US, "%.1f", minAmbientC)}°C to ${String.format(Locale.US, "%.1f", maxAmbientC)}°C).",
            indent = 1
        )
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Resets nearby Eln device temperatures to local biome ambient temperature.", indent = 1)
        cprint(ics, "Usage: /eln resetAmbientTemps <range 1..32>", indent = 1)
        cprint(ics, "")
    }
}

class ElnResetLampLifeCommand: IConsoleCommand {
    override val name = "resetLampLives"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.isEmpty()) {
            LampLists.resetLampLifeFlag = true // This flag is automatically set to false up to two seconds after this command is called.
            cprint(ics, "All lamp lives successfully reset to default!", indent = 1)
        } else {
            cprint(ics, "This command does not take any arguments.", indent = 1)
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Resets the actual lives of all light bulbs installed in lamp", indent = 1)
        cprint(ics, "sockets to the nominal values as defined in the config file.", indent = 1)
        cprint(ics, "")
        cprint(ics, "No parameters.", indent = 1)
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)
}

class ElnWailaEasyModeCommand: IConsoleCommand {
    override val name = "wailaEasyMode"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val wailaEasyMode = getArgBool(ics, args[0])?: return
            saveConfigPath("ui.waila.easyMode", wailaEasyMode)
            cprint(ics, "Waila Easy Mode: ${FC.DARK_GREEN}${boolToStr(wailaEasyMode)}", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables waila easy mode.", indent = 1)
        cprint(ics, "This will save to the server config file.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:bool : Waila easy mode (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnDebugCommand: IConsoleCommand {
    override val name = "debug"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val debug = getArgBool(ics, args[0])?: return
            saveConfigPath("debug.logging.enabled", debug)
            cprint(ics, "Debug mode: ${FC.DARK_GREEN}${boolToStr(debug)}", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables debug mode.", indent = 1)
        cprint(ics, "This will save to the server config file.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:bool : Debug state (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnSimSnapshotCommand: IConsoleCommand {
    override val name = "sim-snapshot"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val enableSnapshots = getArgBool(ics, args[0]) ?: return
            saveConfigPath("debug.logging.simSnapshot", enableSnapshots)
            cprint(ics, "Simulation snapshots: ${FC.DARK_GREEN}${boolToStr(enableSnapshots)}", indent = 1)
            cprint(ics, "Requires debug logging to be enabled as well.", indent = 1)
        } else {
            cprint(ics, "Usage: /eln sim-snapshot <enable|disable>", indent = 1)
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables saving circuit MNA snapshots to disk.", indent = 1)
        cprint(ics, "This will save to the server config file.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:bool : enable/disable snapshotting.", indent = 2)
        cprint(ics, "Debug mode must also be enabled to write files.", indent = 1)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("enable", "disable", "true", "false")
        if (args.isEmpty() || args[0].isEmpty()) return options
        return options.filter { it.startsWith(args[0], ignoreCase = true) }
    }
}

class ElnWatchdogCommand: IConsoleCommand {
    override val name = "watchdog"
    private val validTypes = listOf("all", "thermal", "resistorHeat", "current", "voltage", "shaftSpeed", "other", "defaults")

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.isEmpty() || args.size > 2) {
            cprint(ics, "Usage: /eln watchdog <type> [true|false]", indent = 1)
            cprint(ics, "Types: ${validTypes.joinToString(", ")}", indent = 1)
            return
        }
        val type = validTypes.firstOrNull { it.equals(args[0], ignoreCase = true) } ?: run {
            cprint(ics, "Unknown watchdog type '${args[0]}'.", indent = 1)
            cprint(ics, "Types: ${validTypes.joinToString(", ")}", indent = 1)
            return
        }
        if (type.equals("defaults", ignoreCase = true)) {
            if (args.size != 1) {
                cprint(ics, "Usage: /eln watchdog defaults", indent = 1)
                return
            }
            applyDefaults()
            saveWatchdogConfig()
            cprint(ics, "Watchdog defaults restored (thermal=true, resistorHeat=false, current=true, voltage=true, shaftSpeed=true, other=true).", indent = 1)
            return
        }
        if (args.size != 2) {
            cprint(ics, "Usage: /eln watchdog <type> <true|false>", indent = 1)
            return
        }
        val watchdogEnabled = getArgBool(ics, args[1]) ?: return

        when (type.lowercase()) {
            "all" -> {
                Eln.config.setBoolean("simulation.watchdog.destruction.thermal", watchdogEnabled)
                Eln.config.setBoolean("simulation.watchdog.destruction.resistorHeat", watchdogEnabled)
                Eln.config.setBoolean("simulation.watchdog.destruction.current", watchdogEnabled)
                Eln.config.setBoolean("simulation.watchdog.destruction.voltage", watchdogEnabled)
                Eln.config.setBoolean("simulation.watchdog.destruction.shaftSpeed", watchdogEnabled)
                Eln.config.setBoolean("simulation.watchdog.destruction.other", watchdogEnabled)
            }
            "thermal" -> Eln.config.setBoolean("simulation.watchdog.destruction.thermal", watchdogEnabled)
            "resistorheat" -> Eln.config.setBoolean("simulation.watchdog.destruction.resistorHeat", watchdogEnabled)
            "current" -> Eln.config.setBoolean("simulation.watchdog.destruction.current", watchdogEnabled)
            "voltage" -> Eln.config.setBoolean("simulation.watchdog.destruction.voltage", watchdogEnabled)
            "shaftspeed" -> Eln.config.setBoolean("simulation.watchdog.destruction.shaftSpeed", watchdogEnabled)
            "other" -> Eln.config.setBoolean("simulation.watchdog.destruction.other", watchdogEnabled)
        }

        saveWatchdogConfig()

        if (type.equals("all", ignoreCase = true)) {
            cprint(ics, "All watchdog destruction: ${FC.DARK_GREEN}${boolToStr(watchdogEnabled)}", indent = 1)
        } else {
            cprint(ics, "Watchdog '$type' destruction: ${FC.DARK_GREEN}${boolToStr(watchdogEnabled)}", indent = 1)
        }
    }

    private fun applyDefaults() {
        Eln.config.setBoolean("simulation.watchdog.destruction.thermal", true)
        Eln.config.setBoolean("simulation.watchdog.destruction.resistorHeat", false)
        Eln.config.setBoolean("simulation.watchdog.destruction.current", true)
        Eln.config.setBoolean("simulation.watchdog.destruction.voltage", true)
        Eln.config.setBoolean("simulation.watchdog.destruction.shaftSpeed", true)
        Eln.config.setBoolean("simulation.watchdog.destruction.other", true)
    }

    private fun saveWatchdogConfig() {
        saveConfigPath("simulation.watchdog.destruction.thermal", Eln.config.getBooleanOrElse("simulation.watchdog.destruction.thermal", true))
        saveConfigPath("simulation.watchdog.destruction.resistorHeat", Eln.config.getBooleanOrElse("simulation.watchdog.destruction.resistorHeat", false))
        saveConfigPath("simulation.watchdog.destruction.current", Eln.config.getBooleanOrElse("simulation.watchdog.destruction.current", true))
        saveConfigPath("simulation.watchdog.destruction.voltage", Eln.config.getBooleanOrElse("simulation.watchdog.destruction.voltage", true))
        saveConfigPath("simulation.watchdog.destruction.shaftSpeed", Eln.config.getBooleanOrElse("simulation.watchdog.destruction.shaftSpeed", true))
        saveConfigPath("simulation.watchdog.destruction.other", Eln.config.getBooleanOrElse("simulation.watchdog.destruction.other", true))
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enable or disable destruction for one watchdog category at runtime.", indent = 1)
        cprint(ics, "Use defaults to restore the default watchdog profile.", indent = 1)
        cprint(ics, "This also updates config/eln/eln.json.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:string : Watchdog type (all, thermal, resistorHeat, current, voltage, shaftSpeed, other, defaults).", indent = 2)
        cprint(ics, "@1:bool : Destruction state (enabled/disabled). Not used by defaults.", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        if (args.isEmpty() || args[0].isEmpty()) return validTypes
        if (args.size == 1) return validTypes.filter { it.startsWith(args[0], ignoreCase = true) }
        val boolOptions = listOf("true", "false")
        if (args[0].equals("defaults", ignoreCase = true)) return listOf()
        if (args.size == 2) return boolOptions.filter { it.startsWith(args[1], ignoreCase = true) }
        return listOf()
    }
}

class ElnIconsCommand: IConsoleCommand {
    override val name = "icons"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val symbols = args[0].equals("symbols", ignoreCase = true)
            saveConfigPath("ui.icons.noSymbols", symbols)
            cprint(ics, "Icons mode: ${FC.DARK_GREEN}${boolToStr(symbols)}", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Changes the icon set", indent = 1)
        cprint(ics, "This will save to the server config file.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:string : icons (symbols/items).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("symbols", "items")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnPoleMapCommand: IConsoleCommand {
    override val name = "poleMap"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        val outputName = args.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "eln_power_poles.svg"
        val dimensionFilter = args.getOrNull(1)?.let {
            it.toIntOrNull() ?: run {
                cprint(ics, "${FC.DARK_RED}Invalid dimension id: $it", indent = 1)
                return
            }
        }
        val nodeManager = NodeManager.instance ?: run {
            cprint(ics, "${FC.DARK_RED}Grid data is not available yet.", indent = 1)
            return
        }
        val snapshot = nodeManager.nodeList.toList()
        val svgData = gatherFeatures(snapshot, dimensionFilter)
        if (svgData.points.isEmpty()) {
            val extra = dimensionFilter?.let { " in dimension $it" } ?: ""
            cprint(ics, "${FC.DARK_YELLOW}No T1/T2 poles or grid transformers were found$extra.", indent = 1)
            return
        }
        val svgBody = buildSvg(svgData)
        val outputFile = File(outputName)
        try {
            outputFile.parentFile?.let { parent ->
                if (!parent.exists() && !parent.mkdirs()) {
                    cprint(ics, "${FC.DARK_RED}Unable to create directory ${parent.absolutePath}", indent = 1)
                    return
                }
            }
            outputFile.writeText(svgBody)
            cprint(ics, "${FC.BRIGHT_GREEN}Saved ${svgData.points.size} grid features to ${outputFile.absolutePath}", indent = 1)
        } catch (ex: Exception) {
            cprint(ics, "${FC.DARK_RED}Failed to write SVG: ${ex.message}", indent = 1)
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Creates an SVG map of every T1/T2 pole and grid transformer in the loaded world.", indent = 1)
        cprint(ics, "Altitude is ignored so you get a plan view of the grid layout.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:string? : Optional output path. Defaults to eln_power_poles.svg.", indent = 2)
        cprint(ics, "@1:int? : Optional dimension id filter. When omitted every dimension is included.", indent = 2)
        cprint(ics, "")
        cprint(ics, "Each pole style is rendered with a different color and includes a tooltip with its location.", indent = 1)
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>) = listOf<String>()

    private fun gatherFeatures(nodes: Collection<NodeBase>, dimensionFilter: Int?): SvgData {
        val points = mutableListOf<SvgPoint>()
        val pointIndex = mutableMapOf<CoordKey, SvgPoint>()
        val links = LinkedHashSet<GridLink>()
        for (node in nodes) {
            if (node !is TransparentNode) continue
            val coordinate = node.coordinate
            if (dimensionFilter != null && coordinate.dimension != dimensionFilter) continue
            val element = node.element ?: continue
            when (element) {
                is ElectricalPoleElement -> {
                    val descriptor = element.descriptor as? ElectricalPoleDescriptor ?: continue
                    val style = poleStyle(descriptor) ?: continue
                    val label = "${style.displayName} (dim ${coordinate.dimension}, x=${coordinate.x}, z=${coordinate.z})"
                    val point = SvgPoint(coordinate.x, coordinate.z, coordinate.dimension, style, label)
                    points.add(point)
                    pointIndex[coordKey(coordinate)] = point
                    links.addAll(element.gridLinkList)
                }
                is GridTransformerElement -> {
                    val style = transformerStyle
                    val label = "${style.displayName} (dim ${coordinate.dimension}, x=${coordinate.x}, z=${coordinate.z})"
                    val point = SvgPoint(coordinate.x, coordinate.z, coordinate.dimension, style, label)
                    points.add(point)
                    pointIndex[coordKey(coordinate)] = point
                    links.addAll(element.gridLinkList)
                }
                is GridSwitchElement -> {
                    val style = gridSwitchStyle
                    val label = "${style.displayName} (dim ${coordinate.dimension}, x=${coordinate.x}, z=${coordinate.z})"
                    val point = SvgPoint(coordinate.x, coordinate.z, coordinate.dimension, style, label)
                    points.add(point)
                    pointIndex[coordKey(coordinate)] = point
                    links.addAll(element.gridLinkList)
                }
                is GridElement -> {
                    // Non-pole grid element: track links if it participates
                    links.addAll(element.gridLinkList)
                }
            }
        }
        val edges = mutableListOf<SvgEdge>()
        for (link in links) {
            val start = pointIndex[coordKey(link.a)]
            val end = pointIndex[coordKey(link.b)]
            if (start != null && end != null) {
                edges.add(SvgEdge(start, end, defaultEdgeStyle))
            }
        }
        distributePoints(points)
        scalePoints(points)
        return SvgData(points, edges)
    }

    private fun poleStyle(descriptor: ElectricalPoleDescriptor): TypeStyle? {
        val key = descriptor.name?.lowercase(Locale.ROOT) ?: return null
        return poleStyles[key]
    }

    private fun distributePoints(points: List<SvgPoint>) {
        if (points.isEmpty()) return
        val minSpacing = 3.5
        val step = 1.5
        val maxRadius = 12.0
        for ((_, group) in points.groupBy { it.dimension }) {
            val settled = mutableListOf<SvgPoint>()
            for (point in group) {
                var placed = false
                var radius = 0.0
                while (!placed && radius <= maxRadius) {
                    val samples = if (radius == 0.0) 1 else 12
                    for (i in 0 until samples) {
                        val angle = if (radius == 0.0) 0.0 else (2 * PI * i / samples)
                        val dx = if (radius == 0.0) 0.0 else cos(angle) * radius
                        val dz = if (radius == 0.0) 0.0 else sin(angle) * radius
                        val candidateX = point.x + dx
                        val candidateZ = point.z + dz
                        val ok = settled.all {
                            val dist = hypot(it.drawX - candidateX, it.drawZ - candidateZ)
                            dist >= minSpacing
                        }
                        if (ok) {
                            point.drawX = candidateX
                            point.drawZ = candidateZ
                            placed = true
                            break
                        }
                    }
                    radius += step
                }
                settled.add(point)
            }
        }
    }

    private fun scalePoints(points: List<SvgPoint>) {
        if (points.isEmpty() || MAP_SPACING_SCALE <= 1.0) return
        val baseX = points.minOf { it.drawX }
        val baseZ = points.minOf { it.drawZ }
        for (point in points) {
            point.drawX = baseX + (point.drawX - baseX) * MAP_SPACING_SCALE
            point.drawZ = baseZ + (point.drawZ - baseZ) * MAP_SPACING_SCALE
        }
    }

    private fun buildSvg(data: SvgData): String {
        val points = data.points
        val edges = data.edges
        val drawMinX = points.minOf { it.drawX }
        val drawMaxX = points.maxOf { it.drawX }
        val drawMinZ = points.minOf { it.drawZ }
        val drawMaxZ = points.maxOf { it.drawZ }
        val padding = 16.0
        val mapWidth = max(ceil(drawMaxX - drawMinX), 8.0) + padding * 2
        val mapHeight = max(ceil(drawMaxZ - drawMinZ), 8.0) + padding * 2
        val contentWidth = mapWidth
        val contentHeight = mapHeight
        val usedStyles = points.map { it.style }.distinctBy { it.displayName }
        val legendSpacing = if (usedStyles.isEmpty()) 0.0 else 12.0
        val estimatedLegendWidth = if (usedStyles.isEmpty()) 0.0 else usedStyles.maxOf { it.displayName.length * 4 + 28 }.toDouble()
        val legendWidth = if (usedStyles.isEmpty()) 0.0 else max(estimatedLegendWidth, 110.0)
        val legendHeight = if (usedStyles.isEmpty()) 0.0 else (usedStyles.size * 7 + 10).toDouble()
        val legendAreaWidth = if (usedStyles.isEmpty()) 0.0 else legendWidth + legendSpacing
        val minSvgWidth = 260.0
        val minSvgHeight = 220.0
        val viewWidth = max(contentWidth + legendAreaWidth + padding, minSvgWidth)
        val viewHeight = max(contentHeight, max(legendHeight + padding * 2, minSvgHeight))
        val viewMinX = drawMinX - padding
        val viewMinZ = drawMinZ - padding
        val mapOriginX = drawMinX - padding
        val mapOriginZ = drawMinZ - padding
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"$viewMinX $viewMinZ $viewWidth $viewHeight\" width=\"$viewWidth\" height=\"$viewHeight\">\n")
        builder.append("<style>.pole{stroke:#111827;stroke-width:0.4;opacity:0.95;} .link{stroke-linecap:round;fill:none;} text{font-family:monospace;} .legend text{font-size:5px;fill:#0f172a;} .legend rect{fill:#f8fafc;stroke:#94a3b8;stroke-width:0.4;} .legend circle{stroke:#1e293b;stroke-width:0.3;}</style>\n")
        builder.append("<rect x=\"$mapOriginX\" y=\"$mapOriginZ\" width=\"$contentWidth\" height=\"$contentHeight\" fill=\"#e2e8f0\" stroke=\"#94a3b8\" stroke-width=\"0.4\" />\n")
        builder.append("<g class=\"links\">\n")
        for (edge in edges) {
            builder.append("<line class=\"link\" x1=\"${edge.a.drawX}\" y1=\"${edge.a.drawZ}\" x2=\"${edge.b.drawX}\" y2=\"${edge.b.drawZ}\" stroke=\"${edge.style.color}\" stroke-width=\"${edge.style.width}\" opacity=\"${edge.style.opacity}\" />\n")
        }
        builder.append("</g>\n")
        for ((dimension, group) in points.groupBy { it.dimension }) {
            builder.append("<g class=\"dimension\" data-dimension=\"$dimension\">\n")
            for (point in group) {
                builder.append("<circle class=\"pole\" cx=\"${point.drawX}\" cy=\"${point.drawZ}\" r=\"${point.style.radius}\" fill=\"${point.style.color}\">")
                builder.append("<title>${point.label}</title>")
                builder.append("</circle>\n")
            }
            builder.append("</g>\n")
        }
        if (usedStyles.isNotEmpty()) {
            val legendX = mapOriginX + contentWidth + legendSpacing
            val legendY = viewMinZ + 6
            builder.append("<g class=\"legend\">\n")
            builder.append("<rect x=\"$legendX\" y=\"$legendY\" width=\"$legendWidth\" height=\"$legendHeight\" />\n")
            var rowY = legendY + 6
            for (style in usedStyles) {
                builder.append("<circle class=\"pole\" cx=\"${legendX + 4}\" cy=\"$rowY\" r=\"${style.radius}\" fill=\"${style.color}\" />\n")
                builder.append("<text x=\"${legendX + 10}\" y=\"${rowY + 1.5}\">${style.displayName}</text>\n")
                rowY += 7
            }
            builder.append("</g>\n")
        }
        builder.append("</svg>\n")
        return builder.toString()
    }

    private data class SvgPoint(
        val x: Int,
        val z: Int,
        val dimension: Int,
        val style: TypeStyle,
        val label: String,
        var drawX: Double = x.toDouble(),
        var drawZ: Double = z.toDouble()
    )

    private data class SvgEdge(
        val a: SvgPoint,
        val b: SvgPoint,
        val style: EdgeStyle
    )

    private data class EdgeStyle(
        val color: String,
        val width: Double,
        val opacity: Double
    )

    private data class SvgData(
        val points: List<SvgPoint>,
        val edges: List<SvgEdge>
    )

    private data class CoordKey(val dimension: Int, val x: Int, val z: Int)

    private fun coordKey(coord: Coordinate) = CoordKey(coord.dimension, coord.x, coord.z)

    private data class TypeStyle(
        val key: String,
        val displayName: String,
        val color: String,
        val radius: Double
    )

    companion object {
        private const val MAP_SPACING_SCALE = 4.0
        private val poleStyles = mapOf(
            "utility pole" to TypeStyle("utility pole", "T1 Utility Pole", "#2563eb", 2.6),
            "utility pole w/dc-dc converter" to TypeStyle("utility pole w/dc-dc converter", "T1 Utility Pole w/Transformer", "#f97316", 3.0),
            "direct utility pole" to TypeStyle("direct utility pole", "Direct Utility Pole", "#16a34a", 2.4),
            "transmission tower" to TypeStyle("transmission tower", "T2 Transmission Tower", "#dc2626", 3.2)
        )

        private val transformerStyle = TypeStyle("grid transformer", "Grid Transformer", "#9333ea", 3.4)
        private val gridSwitchStyle = TypeStyle("grid switch", "Grid Switch", "#0ea5e9", 3.0)
        private val defaultEdgeStyle = EdgeStyle("#475569", 0.8, 0.65)
    }
}
