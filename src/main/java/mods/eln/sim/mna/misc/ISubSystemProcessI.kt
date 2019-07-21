package mods.eln.sim.mna.misc

import mods.eln.sim.mna.SubSystem

/**
 * ISubSystemProcessI
 *
 * not sure what this is yet, but I think it sets the voltage or current of a state.
 */
interface ISubSystemProcessI {
    fun simProcessI(s: SubSystem)
}
