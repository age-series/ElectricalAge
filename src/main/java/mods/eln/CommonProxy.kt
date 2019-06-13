package mods.eln

import mods.eln.misc.KeyRegistry

/**
 * CommonProxy - Base class that allows the ClientProxy (or, in the future, ServerProxy?) to override client-only functionality
 */
open class CommonProxy {

    open fun registerRenderers() {
        // Nothing here as the server doesn't render graphics!
    }

    /**
     * registerKey - register a keybind (server and client friendly)
     * @param name name of the key
     * @param key Keyboard.KEY_ for the key
     */
    open fun registerKey(name: String, key: Int) {
        KeyRegistry.registerKeyServer(name, key)
    }
}
