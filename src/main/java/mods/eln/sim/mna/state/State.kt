package mods.eln.sim.mna.state

import mods.eln.Eln
import mods.eln.debug.DebugType
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.component.IAbstractor

import java.util.ArrayList

open class State {

    var id = -1
    var name = "State"

    constructor()
    constructor(name: String) {
        this.name = name
    }

    var state: Double = 0.0
    var subSystem: SubSystem? = null
        get() {
            if (abstractedBy != null) {
                return abstractedBy!!.abstractorSubSystem
            } else {
                return field
            }
        }

    var connectedComponents = ArrayList<Component>()
        internal set

    var isPrivateSubSystem = false
        internal set
    internal var mustBeFarFromInterSystem = false

    var abstractedBy: IAbstractor? = null

    fun getConnectedComponentsNotAbstracted(): ArrayList<Component> {
            val list = ArrayList<Component>()
            for (c in connectedComponents) {
                if (c.abstractedBy != null) continue
                list.add(c)
            }
            return list
        }

    fun isNotSimulated(): Boolean = subSystem == null && abstractedBy == null

    fun addedTo(s: SubSystem) {
        this.subSystem = s
    }

    fun quitSubSystem() {
        subSystem = null
    }

    fun add(c: Component) {
        connectedComponents.add(c)
    }

    fun remove(c: Component?) {
        if (c == null)
            Eln.dp.println(DebugType.MNA, "state.State: remove(Component c) - Component was null!")
        connectedComponents.remove(c)
    }

    open fun canBeSimplifiedByLine(): Boolean {
        return false
    }

    fun setAsPrivate(): State {
        isPrivateSubSystem = true
        return this
    }

    fun setAsMustBeFarFromInterSystem(): State {
        mustBeFarFromInterSystem = true
        return this
    }

    fun mustBeFarFromInterSystem(): Boolean {
        return mustBeFarFromInterSystem
    }

    fun returnToRootSystem(root: RootSystem) {
        root.addStates.add(this)
    }

    override fun toString(): String {
        return "(" + this.id + "," + this.javaClass.simpleName + "_" + name + ")"
    }
}
