package mods.eln.sim.mna.component

import mods.eln.sim.mna.misc.IRootSystemPreStepProcess

class DelayInterSystem2 : VoltageSource("") {

    private var other: DelayInterSystem2? = null

    var Rth: Double = 0.0
    var Uth: Double = 0.0

    var thevnaCalc = false

    fun set(other: DelayInterSystem2) {
        this.other = other
    }

    class ThevnaCalculator(internal var a: DelayInterSystem2, internal var b: DelayInterSystem2) : IRootSystemPreStepProcess {

        override fun rootSystemPreStepProcess() {
            doJobFor(a)
            doJobFor(b)

            var U = (a.Uth - b.Uth) * b.Rth / (a.Rth + b.Rth) + b.Uth
            if (java.lang.Double.isNaN(U)) {
                U = 0.0
            }
            a.u = U
            b.u = U
        }

        internal fun doJobFor(d: DelayInterSystem2) {
            val originalU = d.u

            val aU = 10.0
            d.u = aU
            val aI = d.getSubSystem()!!.solve(d.currentState)

            val bU = 5.0
            d.u = bU
            val bI = d.getSubSystem()!!.solve(d.currentState)

            d.Rth = (aU - bU) / (bI - aI)
            //if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
            if (d.Rth > 10000000000000000000.0) {
                d.Uth = 0.0
                d.Rth = 10000000000000000000.0
            } else {
                d.Uth = aU + d.Rth * aI
            }
            d.u = originalU
        }
    }
}
