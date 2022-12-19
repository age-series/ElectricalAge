package mods.eln.entity.carts

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.node.NodeManager
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.transparentnode.OverheadLinesElement
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.init.Blocks
import net.minecraft.util.DamageSource
import net.minecraft.world.World

class EntityElectricMinecart(world: World, x: Double, y: Double, z: Double): EntityMinecart(world, x, y, z) {

    private var lastOverheadWires: OverheadLinesElement? = null
    private var lastRunState = false

    private val locomotiveResistance = 500.0
    val resistor = Resistor()

    init {
        resistor.r = locomotiveResistance
    }

    override fun func_145821_a(
        blockX: Int,
        blockY: Int,
        blockZ: Int,
        speed: Double,
        drag: Double,
        block: Block?,
        direction: Int
    ) {
        super.func_145821_a(blockX, blockY, blockZ, speed + 1, drag, block, direction)
        val overheadWires = getOverheadWires(Coordinate(posX.toInt(), posY.toInt(), posZ.toInt(), worldObj)) ?: return
        // TODO: Configure isRunning to act as the cart needs more battery power in an internal buffer
        // Allow the cart to move when there is power in the buffer
        // Control the cart ... somehow. Just turn it on full bore? lol
        takeOverheadPower(overheadWires, true)
    }

    private fun attachResistor(overheadWires: OverheadLinesElement) {
        if (overheadWires != lastOverheadWires)
            detachResistor()
            lastOverheadWires = overheadWires
            resistor.connectTo(overheadWires.electricalLoad, null)
            overheadWires.reconnect()
            overheadWires.needPublish()
    }

    private fun detachResistor() {
        if (resistor.subSystem != null)
            resistor.subSystem.removeComponent(resistor)
            resistor.dirty()
        updateLastWire()
        lastOverheadWires = null
    }

    private fun updateLastWire() {
        lastOverheadWires?.reconnect()
        lastOverheadWires?.needPublish()
    }

    override fun killMinecart(p_94095_1_: DamageSource?) {
        super.killMinecart(p_94095_1_)
        detachResistor()
    }

    private fun takeOverheadPower(overheadWires: OverheadLinesElement, isRunning: Boolean) {
        Eln.logger.info("Voltage over the cart is ${overheadWires.electricalLoad.u}")

        if (isRunning != lastRunState) {
            resistor.r = if (isRunning) locomotiveResistance else MnaConst.highImpedance
            resistor.dirty()
        }

        attachResistor(overheadWires)
    }

    private fun getOverheadWires(coordinate: Coordinate): OverheadLinesElement? {
        // Pass coordinate of tracks and check vertically the next 3-4 blocks
        val originalY = coordinate.y
        while (coordinate.y <= (originalY + 4)) {
            coordinate.y
            val node = NodeManager.instance!!.getTransparentNodeFromCoordinate(coordinate)
            if (node is OverheadLinesElement) {
                Eln.logger.info("Cart is under an overhead wire")
                return node
            }
            coordinate.y++
        }
        Eln.logger.info("Cart is not under an overhead wire")
        detachResistor()
        return null
    }

    override fun getMinecartType(): Int {
        return 2
    }

    override fun func_145817_o(): Block? {
        return Blocks.iron_block
    }
}
