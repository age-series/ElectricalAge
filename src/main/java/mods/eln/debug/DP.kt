package mods.eln.debug

import mods.eln.Eln
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * DP - Debug Print - a class for unified printing. This class allows you to print to the log,
 * without needing to worry if Minecraft is running. In general, large portions of the Electrical Age source code
 * are actually designed to run without Minecraft, to the point that it should be possible to get the same quality
 * of logging, with or without Minecraft running, and all from one place.
 *
 * This is that one place. The class init's itself on its own, on the first call. No instantiations needed.
 */
class DP {
    companion object {
        // good way to determine if Minecraft is running or not.
        private var isMinecraftRunning: Boolean? = null

        // enabled printing types
        private val enabledTypes: MutableList<DPType> = DPType.values().toMutableList()

        // Log to talk to for anything Eln related. It will work with and without Minecraft running.
        private var log: Logger = LogManager.getLogger("Eln")

        /**
         * ProperInit - a proper init class once Minecraft is ready for us to begin printing to their logger.
         * It also allows us to dump in a new list of enabled types, in case we have those from a config somewhere.
         *
         * @param enabledTypes the types you want enabled
         * @param externalLogger the Minecraft logger (or any other logger that is compatible) once ready
         */
        @JvmStatic
        fun properInit(enabledTypes: MutableList<DPType>?, externalLogger: Logger?) {
            if (enabledTypes != null) {
                this.enabledTypes.clear()
                for (type in enabledTypes) this.enabledTypes.add(type)
            }
            if (externalLogger != null) {
                isMinecraftRunning = true
                log = externalLogger
                log.info("Debugger enabled?: " + Eln.debugEnabled)
            }else{
                isMinecraftRunning = false
            }
            log.info("Enabled Debugging types: ${Companion.enabledTypes}")
        }

        @JvmStatic
        fun add(t: DPType) {
            if (!(t in enabledTypes)) {
                enabledTypes.add(t)
            }
        }

        @JvmStatic
        fun remove(t: DPType) {
            if (enabledTypes.contains(t)) {
                enabledTypes.remove(t)
            }
        }

        @JvmStatic
        fun clear() {
            enabledTypes.clear()
        }

        @JvmStatic
        fun get(): List<DPType> {
            return enabledTypes
        }

        @JvmStatic
        fun println(type: DPType, str: String) {
            if (enabledTypes.contains(type)) {
                log.info("[" + type.name + "]: " + str)
            }
        }

        @JvmStatic
        fun print(type: DPType, str: String) {
            if (enabledTypes.contains(type)) {
                log.info("[" + type.name + "]: " + str)
            }
        }

        @JvmStatic
        fun println(type: DPType, format: String, vararg data: Any) {
            println(type, String.format(format, *data))
        }

        @JvmStatic
        fun print(type: DPType, format: String, vararg data: Any) {
            print(type, String.format(format, *data))
        }
    }
    

}

enum class DPType {
    MNA, SIM, FSM, SIX_NODE, TRANSPARENT_NODE, SIMPLE_NODE, MECHANICAL, NODE, SOUND, GUI, RENDER, NETWORK, FILE, CONSOLE, LEGACY, OTHER

    // MNA: For any code in mod.eln.sim.mna.*
    // SIM: For any code in mod.eln.sim.* (that is not mna)
    // FSM: For any code in mod.eln.fsm.*
    // SIX_NODE: For any code in mod.eln.sixnode.*
    // TRANSPARENT_NODE: For any code in mod.eln.transparentnode.*
    // SIMPLE_NODE: For any code in mod.eln.simplenode.*
    // MECHANICAL: For any code in mod.eln.mechanical.*
    // NODE: For any code in mod.eln.node.*
    // SOUND: For any code in the sound engine(s)
    // GUI: For any code in the GUI libraries
    // RENDER: For any code in the render loader or renderer classes
    // NETWORK: For any network debugging
    // FILE: Anywhere you touch files
    // CONSOLE: Anywhere you're handling commands
    // LEGACY: For any calls that are not converted that go to Utils.println()
}
