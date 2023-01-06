package mods.eln.transparentnode.railroad

import mods.eln.entity.carts.EntityElectricMinecart
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor

/**
 * @param minecart the electrical minecart entity instance
 * @param resistor the resistor instance used by this minecart
 * @param slowProcess the IProcess slow process for processing the resistor removal after the time specified.
 * @param owningElement the element this minecart was most recently connected to
 */
data class PoweredMinecartSimulationData(
    val minecart: EntityElectricMinecart,
    var resistor: Resistor,
    var resistorElectricalLoad: ElectricalLoad,
    var electricalConnection: ElectricalConnection,
    var slowProcess: RailroadResistorSlowProcess,
    val owningElement: RailroadPowerInterface
    )
