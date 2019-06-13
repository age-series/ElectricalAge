package mods.eln.misc

import cpw.mods.fml.client.registry.ClientRegistry
import mods.eln.Eln
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.StatCollector

/**
 * KeyRegistry: a class for registering keyboard keys for mod use.
 */
class KeyRegistry {
    companion object {

        /**
         * registerKeyClient - register a key on the client
         * @param name: name of the key
         * @param key: the Keyboard.KEY_ that you want to trigger on
         */
        @JvmStatic
        fun registerKeyClient(name: String, key: Int) {
            val keyBinding = KeyBinding(name, key, StatCollector.translateToLocal("ElectricalAge"))
            val nkey = Key(Eln.keyList.size, name, key, false, keyBinding)
            Eln.keyList.set(nkey.name, nkey)
            ClientRegistry.registerKeyBinding(nkey.keyBinding)
        }

        /**
         * registerKeyServer - register a key on the server
         * @param name: name of the key
         * @param key: the Keyboard.KEY_ that you want to trigger on
         */
        @JvmStatic
        fun registerKeyServer(name: String, key: Int) {
            val nkey = Key(Eln.keyList.size, name, key, false, null)
            Eln.keyList.set(nkey.name, nkey)
        }

        /**
         * getKeyID: Get the key ID used in network packets for a named keyboard key
         * @param name the key name ("Wrench" for example)
         * @return the key ID in network packets
         */
        @JvmStatic
        fun getKeyID(name: String): Int {
            return Eln.keyList[name]?.id ?: -1
        }
    }
}

/**
 * Key:
 * @param id: The ID to use to communicate with the server network
 * @param name: The name of the key (shown in the UI?
 * @param key: The Keyboard.KEY_ value (for example, Keyboard.KEY_C)
 * @param lastState: The last state of the key (used for edge detection, set false at beginning
 * @param keyBinding: The keyBinding instance (used to get the keyPressed later) - only on client!
 */
data class Key (val id: Int, val name: String, val key: Int, var lastState: Boolean, val keyBinding: KeyBinding?)
