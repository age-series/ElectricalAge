package mods.eln.railroad

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils
import mods.eln.node.simple.SimpleNode
import mods.eln.railroad.PoweredMinecartSimulationData
import mods.eln.railroad.PoweredMinecartSimulationSingleton.poweredMinecartSimulationData
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad

/**
 * Electrical node that doubles as a Minecraft rail segment. Exposes
 * an electrical load so it behaves like an ELN wire while also
 * fulfilling [RailroadPowerInterface] for powered minecarts.
 */
class ThirdRailNode : SimpleNode(), RailroadPowerInterface {

    private val electricalLoad = NbtElectricalLoad("thirdRailLoad")

    init {
        electricalLoad.setCanBeSimplifiedByLine(true)
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? {
        return null
    }

    override val nodeUuid: String
        get() = NODE_UUID

    override fun initialize() {
        Eln.applySmallRs(electricalLoad)
        electricalLoadList.add(electricalLoad)
        connect()
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int {
        return if (side in HORIZONTAL_SIDES) {
            maskElectricalPower
        } else {
            0
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? {
        return if (side in HORIZONTAL_SIDES && mask and maskElectricalPower != 0) {
            electricalLoad
        } else {
            null
        }
    }

    override fun multiMeterString(side: Direction): String {
        return if (side in HORIZONTAL_SIDES) {
            Utils.plotUIP(electricalLoad.voltage, electricalLoad.current, electricalLoad.serialResistance)
        } else {
            ""
        }
    }

    override fun registerCart(cart: EntityElectricMinecart) {
        val existing = poweredMinecartSimulationData.firstOrNull { it.minecart == cart }
        if (existing != null) {
            if (existing.owningElement === this) {
                return
            }
            existing.owningElement.deregisterCart(cart)
        }

        val resistor = Resistor()
        val resistorLoad = ElectricalLoad().apply { setAsPrivate() }
        val connection = ElectricalConnection(electricalLoad, resistorLoad)

        resistor.connectTo(resistorLoad, null)
        resistor.resistance = MnaConst.highImpedance
        resistorLoad.serialResistance = MnaConst.noImpedance
        connection.resistance = MnaConst.noImpedance

        electricalLoadList.add(resistorLoad)
        electricalComponentList.add(connection)
        electricalComponentList.add(resistor)
        Eln.simulator.addElectricalLoad(resistorLoad)
        Eln.simulator.addElectricalComponent(connection)
        Eln.simulator.addElectricalComponent(resistor)

        val slowProcess = RailroadResistorSlowProcess(this, cart, 0.05)
        Eln.simulator.addSlowProcess(slowProcess)

        poweredMinecartSimulationData.add(
            PoweredMinecartSimulationData(
                cart,
                resistor,
                resistorLoad,
                connection,
                slowProcess,
                this
            )
        )
        electricalLoad.subSystem?.invalidate()
        needPublish()
    }

    override fun deregisterCart(cart: EntityElectricMinecart) {
        val search = poweredMinecartSimulationData.filter { it.minecart == cart }
        if (search.isNotEmpty()) {
            search.forEach {
                if (it.owningElement == this) {
                    electricalComponentList.remove(it.resistor)
                    electricalComponentList.remove(it.electricalConnection)
                    electricalLoadList.remove(it.resistorElectricalLoad)
                    Eln.simulator.removeElectricalComponent(it.resistor)
                    Eln.simulator.removeElectricalComponent(it.electricalConnection)
                    Eln.simulator.removeElectricalLoad(it.resistorElectricalLoad)
                    it.resistor.breakConnection()
                    it.electricalConnection.breakConnection()
                    electricalLoad.subSystem?.invalidate()
                    Eln.simulator.removeSlowProcess(it.slowProcess)
                    poweredMinecartSimulationData.remove(it)
                    needPublish()
                }
            }
        }
    }

    companion object {
        const val NODE_UUID = "ElnThirdRail"
        private val HORIZONTAL_SIDES = setOf(Direction.XN, Direction.XP, Direction.ZN, Direction.ZP)
    }
}
