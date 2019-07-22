package mods.eln.sim.mna.passive

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.ISubSystemProcessI

class Delay : Bipole(), ISubSystemProcessI {

    internal var impedance: Double = 0.0
    internal var conductance: Double = 0.0

    internal var oldIa: Double = 0.0
    internal var oldIb: Double = 0.0

    override fun getCurrent() = oldIa - oldIb

    fun set(impedance: Double) {
        this.impedance = impedance
        this.conductance = 1 / impedance
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        s.addProcess(this)
    }

    override fun applyTo(s: SubSystem) {
        s.addToA(aPin, aPin, conductance)
        s.addToA(bPin, bPin, conductance)
    }

    override fun simProcessI(s: SubSystem) {
        val iA = aPin!!.state * conductance + oldIa
        val iB = bPin!!.state * conductance + oldIb
        val iTarget = (iA - iB) / 2

        val aPinI = iTarget - (aPin!!.state + bPin!!.state) * 0.5 * conductance
        val bPinI = -iTarget - (aPin!!.state + bPin!!.state) * 0.5 * conductance

        s.addToI(aPin, -aPinI)
        s.addToI(bPin, -bPinI)

        oldIa = aPinI
        oldIb = bPinI
    }
}
