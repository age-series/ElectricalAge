package mods.eln.server.console

import net.minecraft.command.ICommandSender
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ElnConsoleCommandLookupTest {
    private class TestCommand(override val name: String) : IConsoleCommand {
        override fun runCommand(ics: ICommandSender, args: List<String>) {
            error("Not used in lookup tests.")
        }
    }

    @Test
    fun findsMixedCaseCommandNamesIgnoringInputCase() {
        val command = findConsoleCommand("polemap", listOf(TestCommand("poleMap"), TestCommand("debug")))

        assertEquals("poleMap", command?.name)
    }

    @Test
    fun returnsNullWhenCommandDoesNotExist() {
        val command = findConsoleCommand("missing", listOf(TestCommand("poleMap"), TestCommand("debug")))

        assertNull(command)
    }
}
