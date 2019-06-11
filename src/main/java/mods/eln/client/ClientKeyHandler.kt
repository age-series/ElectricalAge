package mods.eln.client

import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent
import mods.eln.Eln
import mods.eln.misc.UtilsClient
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Handles all key presses on the client side of things. Sends to the server when keys are pressed.
 */
class ClientKeyHandler {

    private var usedIds = 0
    private val keyList = mutableListOf<Key>()

    /**
     * init: register all of the keys we plan to use
      */
    init {
        // Order of registration is IMPORTANT, it must match between client and server.
        registerKey("Wrench", Keyboard.KEY_C)
    }

    /**
     * registerKey
     * @param name: name of the key
     * @param key: the Keyboard.KEY_ that you want to trigger on
     */
    private fun registerKey(name: String, key: Int) {
        val keyBinding = KeyBinding(name, key, StatCollector.translateToLocal("ElectricalAge"))
        val nkey = Key(usedIds, name, key, false, keyBinding)
        keyList.add(nkey.id, nkey)
        usedIds++
        ClientRegistry.registerKeyBinding(nkey.keyBinding)
    }

    /**
     * getKeyID: Get the key ID used in network packets for a named keyboard key
     * @param name the key name ("Wrench" for example)
     * @return the key ID in network packets
     */
    fun getKeyID(name: String): Int {
        for (key in keyList) {
            if (key.name == name) {
                return key.id
            }
        }
        return -1
    }

    /**
     * onKeyInput - fires if the key is pressed. We check to see (for all keys we have) if one was pressed, and if the
     * state changed, we send the new state to the server.
     */
    @SubscribeEvent
    @Suppress("unused") // this does actually fire, despite what IDEA thinks
    fun onKeyInput(event: KeyInputEvent) {
        // for each key, see if the state has changed. If it has, send it to the server
        for (key in keyList) {
            val s = key.keyBinding.isKeyPressed
            if (s != key.lastState) {
                setState(key, s)
                key.lastState = s
            }
        }
    }

    /**
     * setState - send the key state to the server packet handler.
     * @param key the key to send
     * @param state the state of the key
     */
    private fun setState(key: Key, state: Boolean) {
        //Eln.dp.println(DebugType.OTHER, "setState called on key " + key.name + " with state " + state)

        val bos = ByteArrayOutputStream(64)
        val stream = DataOutputStream(bos)

        try {
            stream.writeByte(Eln.PACKET_PLAYER_KEY.toInt())
            stream.writeByte(key.id)
            stream.writeBoolean(state)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        UtilsClient.sendPacketToServer(bos)
    }

    /**
     * Key:
     * @param id: The ID to use to communicate with the server network
     * @param name: The name of the key (shown in the UI?
     * @param key: The Keyboard.KEY_ value (for example, Keyboard.KEY_C)
     * @param lastState: The last state of the key (used for edge detection, set false at beginning
     * @param keyBinding: The keyBinding instance (used to get the keyPressed later)
     */
    data class Key (val id: Int, val name: String, val key: Int, var lastState: Boolean, val keyBinding: KeyBinding)
}
