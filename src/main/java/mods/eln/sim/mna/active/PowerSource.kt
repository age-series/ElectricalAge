package mods.eln.sim.mna.active

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.passive.VoltageSource
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.Th
import mods.eln.sim.mna.state.State
open class PowerSource(name: String, aPin: State) : VoltageSource(name, aPin, null), IRootSystemPreStepProcess {

    var p: Double = 0.0
    var Umax: Double = 0.0
    var Imax: Double = 0.0

    fun getEffectiveP() = getBipoleU() * getCurrent()

    override fun getPower(): Double {
        return p
    }

    override fun quitSubSystem() {
        getSubSystem()!!.root?.removeProcess(this)
        super.quitSubSystem()
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        getSubSystem()!!.root?.addProcess(this)
        s.addProcess(this)
    }

    override fun rootSystemPreStepProcess() {
        val t = Th.getTh(aPin!!, this)

        var U = (Math.sqrt(t.U * t.U + 4.0 * p * t.R) + t.U) / 2
        U = Math.min(Math.min(U, Umax), t.U + t.R * Imax)
        if (java.lang.Double.isNaN(U)) U = 0.0
        if (U < t.U) U = t.U

        u = U
    }
}
