package mods.eln.sim.mna.misc

/**
 * IDestructor
 *
 * I'm not sure what this interface is for quite yet,
 * but it's currently only used by InterSystem and basically self-destructs a SubSystem
 */
interface IDestructor {
    fun destruct()
}
