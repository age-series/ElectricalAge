package mods.eln.sim.mna.passive

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State

open class Resistor : Bipole {

    constructor()
    constructor(name: String) : super(name)
    constructor(aPin: State?, bPin: State?) : super(aPin, bPin)

    open var r: Double = MnaConst.highImpedance
        get() {return field}
        set(r) {
            if (field != r) {
                field = r
                rInv = 1 / r
                dirty()
            }
        }
    open var rInv: Double = 1.0 / MnaConst.highImpedance


    override fun getCurrent() = getVoltage() * rInv
    fun getPower() = getVoltage() * getCurrent()


    fun highImpedance() {
        r = MnaConst.highImpedance
    }

    fun ultraImpedance() {
        r = MnaConst.ultraImpedance
    }

    fun pullDown() {
        r = MnaConst.pullDown
    }

    override fun applyTo(s: SubSystem) {
        s.addToA(aPin, aPin, rInv)
        s.addToA(aPin, bPin, -rInv)
        s.addToA(bPin, bPin, rInv)
        s.addToA(bPin, aPin, -rInv)
    }
}
