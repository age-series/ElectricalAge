package mods.eln.sim.mna.component

import mods.eln.Eln
import mods.eln.debug.DebugType
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.state.State

open class Capacitor : Bipole, ISubSystemProcessI {

    var c = 0.0
        set(c) {
            if (c == java.lang.Double.NaN)
                Eln.dp.println(DebugType.MNA, "component.Capacitor setC(double c) - c was NaN!")
            field = c
            dirty()
        }
    internal var cdt: Double = 0.toDouble()

    override fun getCurrent(): Double {
        return 0.0
    }

    fun getE() = getVoltage() * getVoltage() * this.c / 2

    constructor() {}

    constructor(name: String) {
        this.name = name
    }

    constructor(aPin: State, bPin: State) {
        this.name = "Capacitor"
        connectTo(aPin, bPin)
    }

    constructor(name: String, aPin: State, bPin: State) {
        this.name = name
        connectTo(aPin, bPin)
    }


    override fun applyTo(s: SubSystem) {
        cdt = this.c / s.dt

        s.addToA(aPin, aPin, cdt)
        s.addToA(aPin, bPin, -cdt)
        s.addToA(bPin, bPin, cdt)
        s.addToA(bPin, aPin, -cdt)
    }

    override fun simProcessI(s: SubSystem) {
        val add = (s.getXSafe(aPin) - s.getXSafe(bPin)) * cdt
        s.addToI(aPin, add)
        s.addToI(bPin, -add)
    }

    override fun quitSubSystem() {
        getSubSystem()!!.removeProcess(this)
        super.quitSubSystem()
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        s.addProcess(this)
    }
}
