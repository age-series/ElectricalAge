package mods.eln.sim.mna.misc

import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.State
import java.lang.Exception

class Th {
    var R: Double = 0.toDouble()
    var U: Double = 0.toDouble()

    val isHighImpedance: Boolean
        get() = R > 1e8

    companion object {

        @JvmStatic
        fun getTh(d: State, voltageSource: VoltageSource): Th {
            if (d.subSystem != voltageSource.subSystem) {
                throw Exception("Not in same subsystem!")
            }

            val th = Th()
            val originalU = d.state

            if (originalU.isNaN()) {
                System.out.println("originalU NaN!")
            }

            val aU = originalU
            voltageSource.u = aU
            val aI = d.subSystem!!.solve(voltageSource.currentState)

            val bU = originalU * 0.95
            voltageSource.u = bU
            val bI = d.subSystem!!.solve(voltageSource.currentState)

            var Rth = (aU - bU) / (bI - aI)
            //System.out.println("au ai bu bi r" + aU + " " + aI + " " + bU + " " + bI + " " + Rth)

            val Uth: Double

            //DP.println(DPType.MNA, "$Rth")

            if (Rth.isNaN()) Rth = MnaConst.highImpedance
            if (Rth < 0) {
                Rth = MnaConst.highImpedance
                Uth = 0.0
            } else if (Rth < MnaConst.noImpedance) {
                Rth = MnaConst.noImpedance
                Uth = originalU + Rth * aI
            } else if (Rth > MnaConst.highImpedance) {
                Rth = 1000.0
                Uth = 0.0
            } else {
                Uth = originalU + Rth * aI
            }


            /*

            //if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
            if (Rth > 10000000000000000000.0 || Rth < 0.001) {
                Uth = 0.0
                Rth = 10000000000000000000.0
            } else {
                Uth = originalU + Rth * aI
            }
            */

            voltageSource.u = Uth // originanlU

            th.R = Rth
            th.U = Uth
            //System.out.println("" + th.R + " " + th.U)
            return th
        }
    }
}
