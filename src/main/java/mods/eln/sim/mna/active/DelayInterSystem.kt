package mods.eln.sim.mna.active

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.passive.Component
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.state.State

class DelayInterSystem : Component(), ISubSystemProcessI {

    private var other: DelayInterSystem? = null
    var pin: State? = null

    internal var impedance: Double = 0.toDouble()
    internal var conductance: Double = 0.toDouble()

    var oldIother = doubleArrayOf(0.0, 0.0)
    internal var doubleBuffer = 0
    var thevnaCalc = false
    var thenvaCurrent: Double = 0.toDouble()
    var Rth: Double = 0.toDouble()
    var Uth: Double = 0.toDouble()

    internal var iTarget: Double = 0.toDouble()

    override fun getConnectedStates(): Array<State?> {
        return arrayOf<State?>()
    }

    operator fun set(pin: State, other: DelayInterSystem) {
        this.other = other
        this.pin = pin
    }

    fun set(impedance: Double): DelayInterSystem {
        this.impedance = impedance
        this.conductance = 1 / impedance

        return this
    }

    override fun quitSubSystem() {
        getSubSystem()!!.removeProcess(this)

        super.quitSubSystem()
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        s.addProcess(this)
    }

    override fun applyTo(s: SubSystem) {
        s.addToA(pin, pin, conductance)
    }

    fun setInitialCurrent(i: Double) {
        oldIother[doubleBuffer] = i
    }

    override fun simProcessI(s: SubSystem) {
        if (!thevnaCalc) {
            //Thevna delay line

            if (Math.abs(Rth) < 1000000.0) {
                val uTarget = Uth - Rth * iTarget
                val aPinI = iTarget - uTarget * conductance
                s.addToI(pin, -aPinI)
            } else {
                val uTarget = other!!.pin!!.state * 0.5 + pin!!.state * 0.5
                //uTarget = 0;
                val aPinI = iTarget - uTarget * conductance
                s.addToI(pin, -aPinI)
            }

            /*
			//STD delay line
			double pinI = 2 * other.getSubSystem().getX(other.pin) * conductance + oldIother[doubleBuffer];
			s.addToI(pin, pinI);

			doubleBuffer = (doubleBuffer + 1) & 1;
			other.oldIother[doubleBuffer] = -pinI;*/

        } else {
            s.addToI(pin, -thenvaCurrent)
        }
    }

    class ThevnaCalculator(internal var a: DelayInterSystem, internal var b: DelayInterSystem) : IRootSystemPreStepProcess {

        override fun rootSystemPreStepProcess() {
            doJobFor(a)
            doJobFor(b)
            val iTarget = (a.Uth - b.Uth) / (a.Rth + b.Rth)
            a.iTarget = iTarget
            b.iTarget = -iTarget
        }

        internal fun doJobFor(d: DelayInterSystem) {
            d.thevnaCalc = true

            d.thenvaCurrent = 2.0
            val aIs = 2.0
            val aU = d.getSubSystem()!!.solve(d.pin!!)

            d.thenvaCurrent = 1.0
            val bIs = 1.0
            val bU = d.getSubSystem()!!.solve(d.pin!!)

            val aC = -(aU * d.conductance + aIs)
            val bC = -(bU * d.conductance + bIs)

            d.Rth = (aU - bU) / (aC - bC)
            d.Uth = aU - d.Rth * aC

            d.thevnaCalc = false
        }
    }
}
