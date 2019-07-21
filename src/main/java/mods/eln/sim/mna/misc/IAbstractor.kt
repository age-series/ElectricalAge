package mods.eln.sim.mna.misc

import mods.eln.sim.mna.SubSystem
import mods.eln.sim.mna.component.Component

/**
 * IAbstractor
 *
 * I'm not sure what this interface is for quite yet,
 * but it's currently only used by InterSystem and basically creates a isolated resistor for a Thevenin adjacent system.
 */
interface IAbstractor {

    var abstractorSubSystem: SubSystem

    fun dirty(component: Component)
}
