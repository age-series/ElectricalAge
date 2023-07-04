package mods.eln.sim.process.destruct

interface IDestructible {
    fun destructImpl()
    fun describe(): String?
}
