package mods.eln.sim.mna.misc

/**
 * IRootSystemPreStepProcess
 *
 * If your function extends this and registers it with RootSystem or the Simulator,
 * it will be called before every single step of the MNA
 */
interface IRootSystemPreStepProcess {
    fun rootSystemPreStepProcess()
}
