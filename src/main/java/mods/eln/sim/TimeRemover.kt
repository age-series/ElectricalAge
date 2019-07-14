package mods.eln.sim

import mods.eln.Eln

class TimeRemover(internal var observer: ITimeRemoverObserver) : IProcess {
    internal var timeout = 0.0

    fun getIsArmed(): Boolean = timeout > 0

    fun setTimeout(timeout: Double) {
        if (this.timeout <= 0) {
            observer.timeRemoverAdd()
            Eln.simulator.addProcess(ProcessType.SlowProcess, this)
        }
        this.timeout = timeout
    }

    override fun process(time: Double) {
        if (getIsArmed()) {
            timeout -= time
            if (timeout <= 0) {
                shot()
            }
        }
    }

    fun shot() {
        timeout = 0.0
        observer.timeRemoverRemove()
        Eln.simulator.removeProcess(ProcessType.SlowProcess, this)
    }
}
