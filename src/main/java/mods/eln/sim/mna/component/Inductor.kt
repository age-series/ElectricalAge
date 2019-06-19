package mods.eln.sim.mna.component

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.state.CurrentState
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound

class Inductor : Bipole, ISubSystemProcessI, INBTTReady {

    var l = 0.0
        set(l) {
            field = l
            dirty()
        }
    internal var ldt: Double = 0.toDouble()

    val currentState = CurrentState()

    override fun getCurrent() = currentState.state

    fun getE() = getCurrent() * getCurrent() * l / 2

    constructor(name: String) {
        this.name = name
    }

    constructor(name: String, aPin: State, bPin: State) : super(aPin, bPin) {
        this.name = name
    }

    override fun applyTo(s: SubSystem) {
        ldt = -this.l / s.dt

        s.addToA(aPin, currentState, 1.0)
        s.addToA(bPin, currentState, -1.0)
        s.addToA(currentState, aPin, 1.0)
        s.addToA(currentState, bPin, -1.0)
        s.addToA(currentState, currentState, ldt)
    }

    override fun simProcessI(s: SubSystem) {
        s.addToI(currentState, ldt * currentState.state)
    }

    override fun quitSubSystem() {
        getSubSystem()!!.states.remove(currentState)
        getSubSystem()!!.removeProcess(this)
        super.quitSubSystem()
    }

    override fun addedTo(s: SubSystem) {
        super.addedTo(s)
        s.addState(currentState)
        s.addProcess(this)
    }

    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        strl += name
        currentState.state = nbt.getDouble(strl + "Istate")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        var strl = str
        strl += name
        nbt.setDouble(strl + "Istate", currentState.state)
    }

    fun resetStates() {
        currentState.state = 0.0
    }
}
