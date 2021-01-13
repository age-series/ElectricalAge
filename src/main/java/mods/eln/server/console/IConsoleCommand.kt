package mods.eln.server.console

import net.minecraft.command.ICommandSender

interface IConsoleCommand {
    val name: String

    fun runCommand(ics: ICommandSender, args: List<String>)
    fun getManPage(ics: ICommandSender, args: List<String>) {}
    fun getTabCompletion(args: List<String>): List<String> = listOf()
    fun isIndexOfUsername(args: List<String>, index: Int): Boolean = false
    fun requiredPermission(): List<UserPermission> = listOf(UserPermission.IS_OPERATOR)
}
