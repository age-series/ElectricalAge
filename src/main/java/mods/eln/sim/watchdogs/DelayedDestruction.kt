package mods.eln.sim.watchdogs

import mods.eln.Eln
import mods.eln.sim.IProcess

class DelayedDestruction(val dest: IDestructible, var tmout: Double): IProcess {
    init {
        Eln.simulator.addSlowProcess(this)
    }

    override fun process(time: Double) {
        tmout -= time
        if(tmout <= 0.0) {
            dest.destructImpl()
            Eln.simulator.removeSlowProcess(this)
        }
    }
}
