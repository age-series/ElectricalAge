package mods.eln.sim.mna.component

import mods.eln.sim.mna.SubSystem

interface IAbstractor {

    var abstractorSubSystem: SubSystem

    fun dirty(component: Component)
}
