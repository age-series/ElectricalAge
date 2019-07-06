package mods.eln.sim.mna.misc

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Component

interface IAbstractor {

    var abstractorSubSystem: SubSystem

    fun dirty(component: Component)
}
