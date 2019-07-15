package mods.eln.sim.mna.process

import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.state.State

class TransformerInterSystemProcess(private val aState: State, private val bState: State, private val aVoltgeSource: VoltageSource, private val bVoltgeSource: VoltageSource) : IRootSystemPreStepProcess {

    private var ratio = 1.0

    /**
     * rootSystemPreStepProcess
     *
     * This code is called from the MNA every tick for every isolated DC/DC converter, and needs to figure out
     * <insert smart comment here>
     *
     * It likely needs to be re-written to perform better. Currently, it causes a capacitance problem, likely because
     * of the call to getTh not taking into account the B side voltage. Not sure exactly yet...
     *
    </insert> */
    override fun rootSystemPreStepProcess() {
        // these two lines below probably are up for replacement.
        val a = getTh(aState, aVoltgeSource)
        val b = getTh(bState, bVoltgeSource)

        var aU = (a.U * b.R + ratio * b.U * a.R) / (b.R + ratio * ratio * a.R)
        if (java.lang.Double.isNaN(aU)) {
            aU = 0.0
        }

        aVoltgeSource.u = aU
        bVoltgeSource.u = aU * ratio
    }

    @Deprecated("")
    internal class Th {
        var R: Double = 0.toDouble()
        var U: Double = 0.toDouble()
    }

    /**
     * getTh
     *
     * @param d VoltageState
     * @param voltageSource a voltage source
     * @return instance of class Th (a pair equivelant)
     */
    @Deprecated("")
    private fun getTh(d: State, voltageSource: VoltageSource): Th {
        val th = Th()
        val originalU = d.state

        val aU = 10.0
        voltageSource.u = aU
        val aI = d.subSystem!!.solve(voltageSource.currentState)

        val bU = 5.0
        voltageSource.u = bU
        val bI = d.subSystem!!.solve(voltageSource.currentState)

        th.R = (aU - bU) / (bI - aI)

        if (th.R > 10000000000000000000.0 || th.R < 0) {
            th.U = 0.0
            th.R = 10000000000000000000.0
        } else {
            th.U = aU + th.R * aI
        }
        voltageSource.u = originalU

        /*
        Experimentally, lowering the resistance between two isolated transformers causes a bit of a capacitance bug.
        I can't fix it for some reason by increasing the resistnace here, but this function does some things that are
        not very good, and it should be combined with the function above the static Th class above.

        Before you try to comb the source code for anything usable about how this class works, I already checked all of
        the source code, and it was never re-written or commented. It just "came to be".

        It should be noted that lowering the resistance here does have some interesting effects, but none of the ones I
        was looking for. Namely, it caused a few things to asplode that were fine otherwise.
         */

        // Eln.dp.println(DPType.MNA, "TISP Resistance: " + th.R);
        return th
    }

    /**
     * setRatio - sets the ratio of the voltage, with regards to the side A voltage. (Ua * ratio = Ub)
     *
     * @param ratio the new ratio to use
     */
    fun setRatio(ratio: Double) {
        this.ratio = ratio
    }
}
