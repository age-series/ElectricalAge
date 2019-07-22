package mods.eln.sim.core

/**
 * IProcess
 *
 * Anything that extends this can be run as a process at some point in the simulator.
 */
interface IProcess {
    /**
     * process - will run any task you want
     * @param dt The time interval, measured in seconds. This can be used in various calculations,
     * such as power transfer per second or some other relation to a particular change in time.
     *
     * Often, (but not strictly so) this function will be called with a frequency of 1/dt
     */
    fun process(dt: Double)
}
