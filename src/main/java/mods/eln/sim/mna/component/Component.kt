package mods.eln.sim.mna.component

import mods.eln.Eln
import mods.eln.debug.DebugType
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.state.State

abstract class Component {

    internal var subSystem: SubSystem? = null

    var abstractedBy: IAbstractor? = null

    var name: String = "Component"

    abstract fun getConnectedStates(): Array<State?>

    open fun addedTo(s: SubSystem) {
        this.subSystem = s
    }

    fun getSubSystem(): SubSystem? {
        if (abstractedBy == null) {
            return subSystem
        } else {
            return abstractedBy?.abstractorSubSystem
        }
    }

    abstract fun applyTo(s: SubSystem)

    open fun canBeReplacedByInterSystem(): Boolean {
        return false
    }

    open fun breakConnection() {}

    open fun returnToRootSystem(root: RootSystem?) {
        root!!.addComponents.add(this)
    }

    fun dirty() {
        if (abstractedBy != null) {
            abstractedBy!!.dirty(this)
        } else if (getSubSystem() != null) {
            getSubSystem()!!.invalidate()
        }
    }

    open fun quitSubSystem() {
        subSystem = null
    }

    open fun onAddToRootSystem() {}

    open fun onRemovefromRootSystem() {}

    override fun toString(): String {
        return "(" + this.javaClass.simpleName + "_" + name + ")"
    }
}
