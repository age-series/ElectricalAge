package mods.eln.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent
import mods.eln.Eln
import mods.eln.ServerKeyHandler
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient.clientOpenGui
import mods.eln.misc.UtilsClient.sendPacketToServer
import mods.eln.wiki.Root
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

data class ElectricalAgeKey(var defaultKeybind: Int, val name: String, var lastState: Boolean = false, var binding: KeyBinding? = null)

object ClientKeyHandler {
    // These are the defaults, but they can be changed by the player after launching the game
    const val DEFAULT_WRENCH_KEY = Keyboard.KEY_C
    const val DEFAULT_WIKI_KEY = Keyboard.KEY_P

    private val keyboardKeys = listOf(
        ElectricalAgeKey(DEFAULT_WRENCH_KEY, ServerKeyHandler.WRENCH),
        ElectricalAgeKey(DEFAULT_WIKI_KEY, ServerKeyHandler.WIKI)
    )

    init {
        keyboardKeys.forEach {
            it.binding = KeyBinding(it.name, it.defaultKeybind, StatCollector.translateToLocal("ElectricalAge"))
            ClientRegistry.registerKeyBinding(it.binding)
        }
    }

    /**
     * Returns either the numeric value of the keyboard key associated with the specified keybind or -1 if the keybind
     * is not found.
     */
    fun getKeybindValue(keybindName: String): Int {
        return keyboardKeys.firstOrNull { it.name == keybindName }?.binding?.keyCode ?: -1
    }

    /**
     * Returns either the human-readable name of the keyboard key associated with the specified keybind or an empty
     * string if the keybind is not found.
     */
    fun getKeybindKey(keybindName: String): String {
        return try {
            Keyboard.getKeyName(getKeybindValue(keybindName))
        } catch (_: IndexOutOfBoundsException) { // This handles the case in which getKeybindValue() somehow returns -1
            ""
        }
    }

    @SubscribeEvent
    fun onKeyInput(@Suppress("UNUSED_PARAMETER") event: KeyInputEvent?) {
        keyboardKeys.forEach {
            setState(it.name, it.binding?.isKeyPressed ?: return@forEach)
        }
    }

    fun setState(name: String, state: Boolean) {
        val entry = keyboardKeys.firstOrNull { it.name == name }?: return
        if (entry.lastState != state) {
            entry.lastState = state // Be sure to set the state so that it calls again when key released

            if (entry.name == ServerKeyHandler.WIKI && state) {
                // Only trigger if state = true (ie, when pressed, not when released)
                // TODO: Add latch feature to allow closing of the UI by pressing again.
                clientOpenGui(Root(null))
            }

            Utils.println("Sending a client key event to server: ${entry.name} is $state")
            val bos = ByteArrayOutputStream(64)
            val stream = DataOutputStream(bos)
            try {
                stream.writeByte(Eln.packetPlayerKey.toInt())
                stream.writeUTF(entry.name)
                stream.writeBoolean(state)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            sendPacketToServer(bos)
        }
    }
}
