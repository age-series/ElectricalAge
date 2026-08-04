package mods.eln.partnode

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.partnode.PartNode
import mods.eln.node.partnode.PartNodeElement
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad

/** One server node per TileMultipart coordinate; each FMP part contributes an element. */
class FmpMultipartNode : PartNode() {
    override val nodeUuid: String = "eln.partnode.multipart"
    override fun mustBeSaved() = false

    override fun initializeFromThat(front: Direction, entityLiving: net.minecraft.entity.EntityLivingBase?, itemStack: net.minecraft.item.ItemStack?) = Unit

    override fun initializeFromNBT() = Unit
}

/** Server-side simulation element for a Creative Resistor FMP part. */
class FmpCreativeResistorElement(
    private val mountSide: Int,
    private val rotation: Int,
    resistance: Double
) : PartNodeElement {
    private val aLoad = NbtElectricalLoad("fmpA")
    private val bLoad = NbtElectricalLoad("fmpB")
    private val resistor = Resistor(aLoad, bLoad)

    init { resistor.resistance = resistance.coerceAtLeast(1.0e-9) }

    override fun connectJob() {
        Eln.simulator.addElectricalLoad(aLoad)
        Eln.simulator.addElectricalLoad(bLoad)
        Eln.simulator.addElectricalComponent(resistor)
    }

    override fun disconnectJob() {
        Eln.simulator.removeElectricalLoad(aLoad)
        Eln.simulator.removeElectricalLoad(bLoad)
        Eln.simulator.removeElectricalComponent(resistor)
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int =
        if (lrdu == LRDU.Down && (side == firstTerminal() || side == secondTerminal())) NodeBase.maskElectricalPower else 0

    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad? =
        if (lrdu != LRDU.Down) null else when (side) {
            firstTerminal() -> aLoad
            secondTerminal() -> bLoad
            else -> null
        }

    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? = null

    private fun firstTerminal() = terminalDirections().first
    private fun secondTerminal() = terminalDirections().second
    private fun terminalDirections(): Pair<Direction, Direction> = when {
        mountSide < 2 && rotation and 1 == 0 -> Direction.ZN to Direction.ZP
        mountSide < 2 -> Direction.XN to Direction.XP
        rotation and 1 == 0 -> Direction.YN to Direction.YP
        mountSide == 2 || mountSide == 3 -> Direction.XN to Direction.XP
        else -> Direction.ZN to Direction.ZP
    }
}
