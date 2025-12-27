package mods.eln.server.console

import mods.eln.Eln
import mods.eln.gridnode.GridElement
import mods.eln.gridnode.GridLink
import mods.eln.gridnode.GridSwitchElement
import mods.eln.gridnode.electricalpole.ElectricalPoleDescriptor
import mods.eln.gridnode.electricalpole.ElectricalPoleElement
import mods.eln.gridnode.transformer.GridTransformerElement
import mods.eln.misc.Coordinate
import mods.eln.misc.FC
import mods.eln.misc.Version
import mods.eln.node.NodeBase
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.server.SaveConfig
import mods.eln.server.console.ElnConsoleCommands.Companion.boolToStr
import mods.eln.server.console.ElnConsoleCommands.Companion.cprint
import mods.eln.server.console.ElnConsoleCommands.Companion.getArgBool
import net.minecraft.command.ICommandSender
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Locale
import java.util.LinkedHashSet
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

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

open class ElnAboutCommand: IConsoleCommand {
    override val name = "about"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        cprint(ics, "${FC.WHITE}About Electrical Age:")
        cprint(ics,  "${FC.BRIGHT_GREY}Authors: ${FC.DARK_GREY}${Eln.AUTHORS.joinToString(", ")}")
        cprint(ics, "${FC.BRIGHT_GREY}Version: ${FC.DARK_GREY}" + Version.simpleVersionName)
        if (Version.GIT_REVISION.isNotEmpty()) {
            cprint(ics, "${FC.BRIGHT_GREY}Git Build Version: ${FC.DARK_GREY}${Version.GIT_REVISION}")
            cprint(ics, "${FC.BRIGHT_BLUE}[GitHub Link to Git Version]", "https://github.com/jrddunbr/ElectricalAge/commit/" + Version.GIT_REVISION)
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

class ElnAgingCommand: IConsoleCommand {
    override val name = "aging"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val aging = getArgBool(ics, args[0])?: return
            SaveConfig.instance?.batteryAging = aging
            SaveConfig.instance?.electricalLampAging = aging
            SaveConfig.instance?.heatFurnaceFuel = aging
            SaveConfig.instance?.infinitePortableBattery = !aging
            cprint(ics, "Batteries / Furnace Fuel / Lamp aging: ${FC.DARK_GREEN}${boolToStr(aging)}", indent = 1)
            cprint(ics, "Parameter saved in the map.", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables aging on:", indent = 1)
        cprint(ics, "- Portable and standards batteries", indent = 1)
        cprint(ics, "- Lamps", indent = 1)
        cprint(ics, "- Fuel into electrical furnaces", indent = 1)
        cprint(ics, "Acts as a combination of the following commands:", indent = 1)
        cprint(ics, "- batteryAging, lampAging, furnaceFuel", indent = 1)
        cprint(ics, "Changes stored into the map.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters :", indent = 1)
        cprint(ics, "@0:bool : Aging state (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            val returnList: List<String>
            try {
                // Sorting the list can cause the game to crash... so let's try to handle the situation
                returnList = options.filter {it.startsWith(args[0], ignoreCase = true)}
                return returnList
            } catch (ex: java.lang.Exception) {
                ex.printStackTrace()
            }
            return listOf()
        }
    }
}

class ElnCablePaceCommand: IConsoleCommand {
    override val name = "cablePace"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        cprint(ics, "The cable pace is set to ${Eln.cablePowerFactor}")
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Simply prints the Cable Pace on the server.", indent = 1)
        cprint(ics, "")
        cprint(ics, "No input parameters.", indent = 1)
        cprint(ics, "")
    }
}

class ElnLampAgingCommand: IConsoleCommand {
    override val name = "lampAging"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val lampAging = getArgBool(ics, args[0])?: return
            SaveConfig.instance?.electricalLampAging = lampAging
            cprint(ics, "Lamp aging: ${FC.DARK_GREEN}${boolToStr(lampAging)}", indent = 1)
            cprint(ics, "Parameter saved in the map.", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables aging on lamps.", indent = 1)
        cprint(ics, "Changes stored into the map.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters :", indent = 1)
        cprint(ics, "@0:bool : Lamp aging (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnBatteryAgingCommand: IConsoleCommand {
    override val name = "batteryAging"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val lampAging = getArgBool(ics, args[0])?: return
            SaveConfig.instance?.batteryAging = lampAging
            cprint(ics, "Non portable batteries aging: ${FC.DARK_GREEN}${boolToStr(lampAging)}", indent = 1)
            cprint(ics, "Parameter saved in the map.", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables aging on standard batteries.", indent = 1)
        cprint(ics, "Changes stored into the map.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:bool : Battery aging (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnHeatFurnaceFuelCommand: IConsoleCommand {
    override val name = "furnaceFuel"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val furnaceFuelAging = getArgBool(ics, args[0])?: return
            SaveConfig.instance?.heatFurnaceFuel = furnaceFuelAging
            cprint(ics, "Furnace fuel aging: ${FC.DARK_GREEN}${boolToStr(furnaceFuelAging)}", indent = 1)
            cprint(ics, "Parameter saved in the map.", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables aging on fuel into electrical furnaces.", indent = 1)
        cprint(ics, "Changes stored into the map.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:bool : Furnace fuel aging (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

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


class ElnRegenOreQueueCommand: IConsoleCommand {
    override val name = "regenOre"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        Eln.saveConfig.reGenOre = true
        cprint(ics, "Will regenerate ore at next map reload", indent = 1)
        cprint(ics, "Parameter saved in the map and effective once.", indent = 1)
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Regenerates ELN ores at the next map reload.", indent = 1)
        cprint(ics, "Changes stored into the map and effective once when set.", indent = 1)
        cprint(ics, "")
        cprint(ics, "No input parameters.", indent = 1)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)
}

class ElnLampsKillMonstersCommand: IConsoleCommand {
    override val name = "killMonstersAroundLamps"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val killMonstersAroundLamps = getArgBool(ics, args[0])?: return
            Eln.instance.killMonstersAroundLamps = killMonstersAroundLamps
            cprint(ics, "Avoid monsters spawning around lamps: ${FC.DARK_GREEN}${boolToStr(killMonstersAroundLamps)}", indent = 1)
            cprint(ics, "Warning: Command effective to this game instance only, when you close the game, this config will be reverted.", indent = 1)
        } else {
            cprint(ics, "This command only takes one argument - true or false")
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "When set, monsters don't spawn around the lamps (default).", indent = 1)
        cprint(ics, "When clear, leaving lights on in dark zones is recommended...", indent = 1)
        cprint(ics, "Effective only during this game instance.", indent = 1)
        cprint(ics, "(See \"Eln.cfg\" for permanent effect.)", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters :", indent = 1)
        cprint(ics, "@0:bool : Enable/disable.", indent = 1)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
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
        println("man command args: $args")
        when (args.size) {
            0 -> {
                cprint(ics, "Returns help for a given command.", indent = 1)
                cprint(ics, "")
                cprint(ics, "Parameters :", indent = 1)
                cprint(ics, "@0:string : Command name to get documentation.", indent = 2)
                cprint(ics, "")
            }
            else -> {
                val command = ElnConsoleCommandList.mapNotNull { if (it.name.lowercase() == args[0]) it else null }.firstOrNull()
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
            return ElnConsoleCommandList.filter {it.name.startsWith(args[0], ignoreCase = true)}.map{it.name}.toMutableList()
        }
    }
}

class ElnWailaEasyModeCommand: IConsoleCommand {
    override val name = "wailaEasyMode"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val wailaEasyMode = getArgBool(ics, args[0])?: return
            Eln.wailaEasyMode = wailaEasyMode
            var nonsense = false
            Eln.config.get("balancing", "wailaEasyMode", nonsense).set(Eln.wailaEasyMode)
            Eln.config.save()
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
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnDebugCommand: IConsoleCommand {
    override val name = "debug"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val debug = getArgBool(ics, args[0])?: return
            Eln.debugEnabled = debug
            val nonsense = false
            Eln.config.get("debug", "enable", nonsense).set(Eln.debugEnabled)
            Eln.config.save()
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
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnSimSnapshotCommand: IConsoleCommand {
    override val name = "sim-snapshot"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val enableSnapshots = getArgBool(ics, args[0]) ?: return
            Eln.simSnapshotEnabled = enableSnapshots
            val nonsense = false
            Eln.config.get("debug", "simSnapshot", nonsense).set(Eln.simSnapshotEnabled)
            Eln.config.save()
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

class ElnExplosionsCommand: IConsoleCommand {
    override val name = "explosions"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        when (args.size) {
            1 -> {
                val explosions = getArgBool(ics, args[0]) ?: return
                Eln.explosionEnable = explosions
                val nonsense = false
                Eln.config.get("gameplay", "explosion", nonsense).set(Eln.explosionEnable)
                Eln.config.save()
                cprint(ics, "Explosions: ${FC.DARK_GREEN}${boolToStr(explosions)}", indent = 1)
            }
            2 -> {
                if (!args[0].equals("debug", ignoreCase = true)) return
                val debugWatchdog = getArgBool(ics, args[1]) ?: return
                val nonsense = false
                Eln.config.get("debug", "watchdog", nonsense).set(Eln.debugExplosions)
                Eln.config.save()
                cprint(ics, "The debug watchdog is now ${FC.DARK_GREEN}${boolToStr(debugWatchdog)}", indent = 1)
            }
            else -> {
                cprint(ics, "This command only takes one argument - true or false")
            }
        }
    }

    override fun getManPage(ics: ICommandSender, args: List<String>) {
        cprint(ics, "Enables/disables explosions.", indent = 1)
        cprint(ics, "This will save to the server config file.", indent = 1)
        cprint(ics, "")
        cprint(ics, "Parameters:", indent = 1)
        cprint(ics, "@0:bool : Explosions (enabled/disabled).", indent = 2)
        cprint(ics, "")
    }

    override fun requiredPermission() = listOf(UserPermission.IS_OPERATOR)

    override fun getTabCompletion(args: List<String>): List<String> {
        val options = listOf("true", "false")
        return if (args.isEmpty() || args[0] == "") {
            options
        } else {
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
        }
    }
}

class ElnIconsCommand: IConsoleCommand {
    override val name = "icons"

    override fun runCommand(ics: ICommandSender, args: List<String>) {
        if (args.size == 1) {
            val symbols = args[0].equals("symbols", ignoreCase = true)
            Eln.noSymbols = symbols
            val nonsense = false
            Eln.config.get("gameplay", "noSymbols", nonsense).set(Eln.noSymbols)
            Eln.config.save()
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
            return options.filter {it.startsWith(args[0], ignoreCase = true)}
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
