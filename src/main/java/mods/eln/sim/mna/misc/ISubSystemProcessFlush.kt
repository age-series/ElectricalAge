package mods.eln.sim.mna.misc

/**
 * ISubSystemProcessFlush
 *
 * This only seems to be used by Line, and is run juust after the MNA matrixes are generated.
 *
 * I think things that extend this are used mostly when creating a combined component that updates multiple other
 * abstracted components.
 */
interface ISubSystemProcessFlush {
    fun simProcessFlush()
}
