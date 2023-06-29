package mods.eln.server.console

import mods.eln.misc.FC
import net.minecraft.command.ICommand
import net.minecraft.command.ICommandSender
import net.minecraft.event.ClickEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText
import java.lang.Exception
import java.util.*

val ElnConsoleCommandList = mutableListOf<IConsoleCommand>()

class ElnConsoleCommands: ICommand {

    init {
        ElnConsoleCommandList.addAll(listOf(
            ElnLsCommand(),
            ElnAboutCommand(),
            ElnVersionCommand(),
            ElnCablePaceCommand(),
            ElnAgingCommand(),
            ElnBatteryAgingCommand(),
            ElnLampAgingCommand(),
            ElnHeatFurnaceFuelCommand(),
            ElnNewWindDirectionCommand(),
            ElnRegenOreQueueCommand(),
            ElnLampsKillMonstersCommand(),
            ElnMatrixCommand(),
            ElnManCommand(),
            ElnWailaEasyModeCommand(),
            ElnDebugCommand(),
            ElnExplosionsCommand(),
            ElnIconsCommand()
        ))
    }

    companion object {
        fun cprint(ics: ICommandSender, text: String, indent: Int = 0) {
            printIndented(text, indent).forEach {
                ics.addChatMessage(ChatComponentText(it))
            }
        }

        fun cprint(ics: ICommandSender, text: String, url: String) {
            val msg = ChatComponentText(FC.BRIGHT_GREY + text)
            msg.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, url)
            ics.addChatMessage(msg)
        }

        fun printIndented(text: String, indent: Int): List<String> {
            val lineLength = 60 - indent * 2
            val list = mutableListOf<String>()
            val finalLine = text
                .split(' ')
                .fold("", { acc, next ->
                    if (acc.length + next.length > lineLength) {
                        list.add(acc)
                        next
                    } else {
                        "$acc $next"
                    }
                })
            list.add(finalLine)
            var mostRecentColor = '7' // default color is light gray
            val list2 = list.map  {
                line ->
                val lastLine = "§$mostRecentColor$line"
                if ("§" in line) {
                    mostRecentColor = line[line.lastIndexOf("§") + 1]
                }
                lastLine
            }
            val whitespace = (0 until indent * 2).joinToString("") { " " }
            return list2.map{"$whitespace$it"}
        }

        fun getArgBool(ics: ICommandSender, arg: String): Boolean? {
            val lowerArg = arg.lowercase()
            return if (lowerArg.isEmpty()) {
                cprint(ics, "Error: Empty argument.", indent = 1)
                null
            }else if (lowerArg == "0" || lowerArg == "false" || lowerArg == "no" || lowerArg == "disabled") {
                false
            } else if (lowerArg == "1" || lowerArg == "true" || lowerArg == "yes" || lowerArg == "enabled") {
                true
            } else {
                cprint(ics, "Error: Expected (true/false), got $arg",  indent = 1)
                null
            }
        }

        fun boolToStr(value: Boolean): String {
            return if (value) "Enabled" else "Disabled"
        }
    }

    // What the heck was Mojang thinking here?
    override fun compareTo(other: Any?): Int {
        val isString = other !is String
        if (isString) {
            println("CompareTo is not String: ${other?.javaClass?.name}")
        }
        if (other is String) {
            return "eln2".compareTo(other)
        } else {
            return "eln2".compareTo(other.toString())
        }
    }

    override fun getCommandName() = "eln"
    override fun getCommandUsage(p_71518_1_: ICommandSender) =
        "${FC.DARK_CYAN}Electrical Age Console, run /eln ls for commands${FC.BRIGHT_GREY }"

    override fun getCommandAliases() = mutableListOf<String>()

    override fun processCommand(ics: ICommandSender, args: Array<out String>) {
        if (args.isEmpty()) {
            cprint(ics,"${FC.DARK_CYAN}Electrical Age Console, run /eln ls for commands${FC.BRIGHT_GREY }")
            return
        }
        val permissions = determinePermissionsList(ics)
        val command = ElnConsoleCommandList.filter { it.name.equals(args[0], ignoreCase = true) }
        if (command.isEmpty()) {
            cprint(ics,"${FC.DARK_CYAN}Command not found, run /eln ls for commands${FC.BRIGHT_GREY }")
            return
        }
        cprint(ics, "${FC.DARK_CYAN}${ics.commandSenderName} $${FC.DARK_YELLOW} /eln ${args.joinToString(" ")}")
        val canRun = permissions.any { command[0].requiredPermission().contains(it) }
        if (canRun) {
            command[0].runCommand(ics, args.toList().drop(1))
        } else {
            cprint(ics, "${FC.DARK_CYAN}You do not have permission to run that command. " +
                "You need to have one of the following: ${command[0].requiredPermission()}${FC.BRIGHT_GREY }")
        }
    }

    fun determinePermissionsList(ics: ICommandSender): List<UserPermission> {
        var creative = false
        var singlePlayer = false
        var isOperator = false
        val player = ics.entityWorld.getPlayerEntityByName(ics.commandSenderName)
        val console = player == null
        if (!console) {
            creative = player.capabilities.isCreativeMode
            singlePlayer = MinecraftServer.getServer().isSinglePlayer
            isOperator = MinecraftServer.getServer().configurationManager.func_152603_m().func_152700_a(player.displayName) != null
        }
        val playerPerms = mutableListOf<UserPermission>()
        if (creative)
            playerPerms.add(UserPermission.IS_CREATIVE)
        if (console) {
            playerPerms.add(UserPermission.IS_CONSOLE)
            playerPerms.add(UserPermission.IS_OPERATOR)
        }
        if (isOperator)
            playerPerms.add(UserPermission.IS_OPERATOR)
        if (singlePlayer)
            playerPerms.add(UserPermission.IS_OPERATOR)
        return playerPerms.toList()
    }

    // We don't actually use this because we do it on command execution for more control
    override fun canCommandSenderUseCommand(ics: ICommandSender) = true

    override fun addTabCompletionOptions(ics: ICommandSender, args: Array<out String>): MutableList<String> {
        if (args.toList().isEmpty() || args[0] == "") {
            return ElnConsoleCommandList.map {it.name}.toMutableList()
        }
        val command = ElnConsoleCommandList.filter { it.name.equals(args[0], ignoreCase = true) }
        if (command.isEmpty()) {
            return ElnConsoleCommandList.filter {it.name.startsWith(args[0], ignoreCase = true)}.map{it.name}.toMutableList()
        }
        return command.first().getTabCompletion(args.drop(1)).toMutableList()
    }

    override fun isUsernameIndex(args: Array<out String>, index: Int): Boolean {
        return false
    }
}
