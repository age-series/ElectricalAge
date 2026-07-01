package mods.eln.simplenode.test

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.simple.SimpleNode
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad

class TestNode : SimpleNode() {
    private val load = NbtElectricalLoad("load")
    private val resistor = Resistor(load, null)

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return maskElectricalPower
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad {
        return load
    }

    override val nodeUuid: String
        get() = getNodeUuidStatic()

    override fun initialize() {
        electricalLoadList.add(load)
        electricalComponentList.add(resistor)

        load.serialResistance = 10.0
        resistor.resistance = 90.0

        connect()
    }

    companion object {
        @JvmStatic
        fun getNodeUuidStatic(): String {
            return "eln.TestNode"
        }
    }
}
