package mods.eln.sim.mna.process

import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.state.State

class TheveninAbstraction {

    /**
     * getTh
     *
     * What this does isn't understood quite yet
     *
     * @param d State
     * @param voltageSource Voltage Source
     * @return TheveninData instance
     */
    fun getTh(d: State, voltageSource: VoltageSource): TheveninData {
        val th = TheveninData()
        val originalU = d.state

        // these seem very arbitrary
        val aU = 10.0
        voltageSource.u = aU
        val aI = d.subSystem!!.solve(voltageSource.currentState)

        val bU = 5.0
        voltageSource.u = bU
        val bI = d.subSystem!!.solve(voltageSource.currentState)

        // Resistance  = delta of voltages divided by the delta of currents
        var Rth = (aU - bU) / (bI - aI)

        // Voltage
        val Uth: Double

        //if(Double.isInfinite(d.Rth)) d.Rth = Double.MAX_VALUE;
        if (Rth > 10000000000000000000.0 || Rth < 0) {
            Uth = 0.0
            Rth = 10000000000000000000.0
        } else {
            // I think this is incorrect, and that it maybe should be the averages of the two values.
            Uth = aU + Rth * aI
        }
        voltageSource.u = originalU

        th.R = Rth
        th.U = Uth
        return th
    }
}

class TheveninData {

    var U: Double = 0.0
    var R: Double = 0.0

    fun isHighImpedance(): Boolean {
        return R > 1e8
    }
}
