package mods.eln.sim.destruct

interface IDestructable {
    fun destructImpl()

    fun describe(): String
}
