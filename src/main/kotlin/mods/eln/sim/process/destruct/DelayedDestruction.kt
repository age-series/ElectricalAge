package mods.eln.sim.process.destruct

import mods.eln.Eln
import mods.eln.sim.IProcess

class DelayedDestruction(val dest: IDestructible, var timeout: Double): IProcess {
    init {
        Eln.simulator.addSlowProcess(this)
    }

    override fun process(time: Double) {
        timeout -= time
        if(timeout <= 0.0) {
            dest.destructImpl()
            Eln.simulator.removeSlowProcess(this)
        }
    }
}
