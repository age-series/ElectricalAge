package mods.eln.sim.mna.misc

import mods.eln.misc.MaterialProperties
import mods.eln.misc.MaterialType

object MnaConst {
    val ultraImpedance = 1e16
    val highImpedance = 1e9
    val pullDown = 1e9
    val noImpedance = MaterialProperties.getElectricalResistivity(MaterialType.COPPER) * (25.0 / 1000000.0 / 1.0) * 10_000_000_000
}
