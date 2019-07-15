package mods.eln.client

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent
import mods.eln.Eln
import mods.eln.misc.UtilsClient
import mods.eln.misc.Key

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

/**
 * Handles all key presses on the client side of things. Sends to the server when keys are pressed.
 */
class ClientKeyHandler {

    /**
     * onKeyInput - fires if the key is pressed. We check to see (for all keys we have) if one was pressed, and if the
     * state changed, we send the new state to the server.
     */
    @SubscribeEvent
    fun onKeyInput(event: KeyInputEvent) {
        // for each key, see if the state has changed. If it has, send it to the server
        Eln.keyList
            .filter{it.value.keyBinding?.isKeyPressed != it.value.lastState}
            .forEach {
                val kbnd = it.value.keyBinding
                if (kbnd != null) {
                    setState(it.value, kbnd.isKeyPressed)
                    it.value.lastState = kbnd.isKeyPressed
                }
        }
    }

    /**
     * setState - send the key state to the server packet handler.
     * @param key the key to send
     * @param state the state of the key
     */
    private fun setState(key: Key, state: Boolean) {
        //Eln.dp.println(DPType.OTHER, "setState called on key " + key.name + " with state " + state)

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
}
