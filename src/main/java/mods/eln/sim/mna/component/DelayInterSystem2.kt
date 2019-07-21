package mods.eln.sim.mna.component

import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.MnaConst

class DelayInterSystem2 : VoltageSource("") {

    private var other: DelayInterSystem2? = null

    var Rth: Double = 0.0

    var thevnaCalc = false

    fun set(other: DelayInterSystem2) {
        this.other = other
    }

    class ThevnaCalculator(internal var a: DelayInterSystem2, internal var b: DelayInterSystem2) : IRootSystemPreStepProcess {

        override fun rootSystemPreStepProcess() {
            doJobFor(a)
            doJobFor(b)

            var U = (a.u - b.u) * b.Rth / (a.Rth + b.Rth) + b.u
            if (java.lang.Double.isNaN(U)) {
                U = 0.0
            }
            a.u = U
            b.u = U
        }

        internal fun doJobFor(d: DelayInterSystem2) {
            val originalU = d.u

            // 10.0
            val aU = 10.0 //originalU
            d.u = aU
            val aI = d.getSubSystem()!!.solve(d.currentState)

            // 5.0
            val bU = 5.0 //originalU * 0.95
            d.u = bU
            val bI = d.getSubSystem()!!.solve(d.currentState)

            d.Rth = (aU - bU) / (bI - aI) * 2

            if (d.Rth.isNaN()) d.Rth = MnaConst.highImpedance
            if (d.Rth < MnaConst.noImpedance) {
                d.Rth = MnaConst.noImpedance
            } else if (d.Rth > MnaConst.ultraImpedance) {
                d.Rth = MnaConst.ultraImpedance
            }
            d.u = originalU + d.Rth * aI

            /*
            d.Rth = (aU - bU) / (bI - aI) * 2
            if (d.Rth.isNaN()) d.Rth = MnaConst.noImpedance
            //if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
            if (d.Rth > 10000000000000000000.0) {
                d.u = 0.0
                d.Rth = 10000000000000000000.0
            } else {
                d.u = originalU + d.Rth * aI
            }
             */
        }
    }
}
