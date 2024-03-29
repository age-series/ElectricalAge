package mods.eln.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent
import mods.eln.Eln
import mods.eln.ServerKeyHandler
import mods.eln.i18n.I18N.tr
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

class ClientKeyHandler {
    // Note: C is the default wrench key, but it can be changed with the GUI in-game. This is override with the value stored in options.txt
    private val keyboardKeys = listOf(
        ElectricalAgeKey(Keyboard.KEY_C, ServerKeyHandler.WRENCH),
        ElectricalAgeKey(Keyboard.KEY_P, ServerKeyHandler.WIKI)
    )

    init {
        keyboardKeys.forEach {
            it.binding = KeyBinding(it.name, it.defaultKeybind, StatCollector.translateToLocal("ElectricalAge"))
            ClientRegistry.registerKeyBinding(it.binding)
        }
    }

    @SubscribeEvent
    fun onKeyInput(event: KeyInputEvent?) {
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