package mods.eln.server.console

import mods.eln.Eln
import mods.eln.misc.FC
import mods.eln.misc.Version
import mods.eln.server.SaveConfig
import mods.eln.server.console.ElnConsoleCommands.Companion.boolToStr
import mods.eln.server.console.ElnConsoleCommands.Companion.cprint
import mods.eln.server.console.ElnConsoleCommands.Companion.getArgBool
import net.minecraft.command.ICommandSender
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

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
