package mods.eln.sim.process.destruct

interface IDestructable {
    fun destructImpl()

    fun describe(): String
}
