package mods.eln.sim.mna.component

import mods.eln.misc.INBTTReady
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.misc.ISubSystemProcessI
import mods.eln.sim.mna.state.CurrentState
import mods.eln.sim.mna.state.State
import net.minecraft.nbt.NBTTagCompound


open class VoltageSource : Bipole, ISubSystemProcessI, INBTTReady {

    val currentState = CurrentState()

    var u: Double = 0.0

    override fun getCurrent(): Double {
        return -currentState.state
    }

    open fun getPower(): Double {
        return getVoltage() * getCurrent()
    }

    constructor(name: String) {
        this.name = name
    }

    constructor(name: String, aPin: State?, bPin: State?) : super(aPin, bPin) {
        this.name = name
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

    override fun applyTo(s: SubSystem) {
        s.addToA(aPin, currentState, 1.0)
        s.addToA(bPin, currentState, -1.0)
        s.addToA(currentState, aPin, 1.0)
        s.addToA(currentState, bPin, -1.0)
    }

    override fun simProcessI(s: SubSystem) {
        s.addToI(currentState, u)
    }

    // TODO: Move to a VoltageSourceNbt abstraction class
    override fun readFromNBT(nbt: NBTTagCompound, str: String) {
        u = (nbt.getDouble(str + name + "U"))
        currentState.state = nbt.getDouble(str + name + "Istate")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String) {
        nbt.setDouble(str + name + "U", getVoltage())
        nbt.setDouble(str + name + "Istate", currentState.state)
    }
}
