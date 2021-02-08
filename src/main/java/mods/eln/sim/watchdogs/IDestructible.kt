package mods.eln.sim.watchdogs

interface IDestructible {
    fun destructImpl()
    fun describe(): String?
}
