package mods.eln.railroad

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.sim.mna.misc.MnaConst
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import kotlin.math.abs
import kotlin.math.sign

class EntityElectricMinecart(world: World, x: Double, y: Double, z: Double): EntityMinecart(world, x, y, z) {

    // TODO: Confirm if this is actually needed for anything
    constructor(world: World): this(world, 0.0, 0.0, 0.0)

    private var lastPowerElement: RailroadPowerInterface? = null
    private val locomotiveMaximumResistance = 200.0
    var energyBufferTargetJoules = 10_000.0
    var energyBufferJoules = 0.0

    override fun onUpdate() {
        super.onUpdate()
        val cartCoordinate = Coordinate(posX.toInt(), posY.toInt(), posZ.toInt(), worldObj)
        val overheadWires = getOverheadWires(cartCoordinate)
        val underTrackWires = getUnderTrackWires(cartCoordinate)

        when (val oldPowerElement = lastPowerElement) {
            overheadWires, underTrackWires, null -> {
                // references existing overhead wires or under track wires, or nothing
            }
            else -> {
                // references old overhead wires or under track wires that are not the current ones
                energyBufferJoules += PoweredMinecartSimulationSingleton.cartCollectEnergy(this)
                Eln.logger.info("Deregister cart from $oldPowerElement")
                oldPowerElement.deregisterCart(this)
            }
        }

        val currentElement: RailroadPowerInterface? = underTrackWires ?: overheadWires

        if (currentElement != null) {
            if (currentElement != lastPowerElement) {
                Eln.logger.info("Register cart to $currentElement")
                currentElement.registerCart(this)
            }

            if (energyBufferJoules < energyBufferTargetJoules) {
                val chargeRateInv = energyBufferTargetJoules / (abs(energyBufferTargetJoules - energyBufferJoules) * 2)
                PoweredMinecartSimulationSingleton.powerCart(this, chargeRateInv * locomotiveMaximumResistance, 0.1)
            } else {
                PoweredMinecartSimulationSingleton.powerCart(this, MnaConst.highImpedance, 0.1)
            }
            energyBufferJoules += PoweredMinecartSimulationSingleton.cartCollectEnergy(this)
        }

        lastPowerElement = currentElement
    }

    var pushX: Double = 0.0
    var pushZ: Double = 0.0

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
        if (energyBufferJoules > 0) {
            val maxEnergy = 40.0
            var energyAvailable = maxEnergy
            if (energyBufferJoules < maxEnergy) {
                energyAvailable = energyBufferJoules
            }

            val startingThreshold = 0.0005

            if (abs(motionX) >= startingThreshold || abs(motionZ) >= startingThreshold) {
                if (abs(motionX) < 0.5 && abs(motionZ) < 0.5) {
                    pushX = motionX.sign * 0.05 * (energyAvailable / maxEnergy)
                    pushZ = motionZ.sign * 0.05 * (energyAvailable / maxEnergy)
                    energyBufferJoules -= energyAvailable
                }
            }
        }

        //Eln.logger.info("Push: ($pushX, $pushZ)")

        motionX += pushX
        motionZ += pushZ

        //Eln.logger.info("Speed: ($motionX, $motionZ)")

        pushX = 0.0
        pushZ = 0.0
    }

    private fun getOverheadWires(coordinate: Coordinate): RailroadPowerInterface? {
        val base = Coordinate(coordinate)
        val startY = base.y
        for (offset in 1..3) {
            val current = Coordinate(base)
            current.y = startY + offset
            val candidate = getRailroadPowerInterfaceAt(current)
            if (candidate != null) {
                return candidate
            }
        }
        return null
    }

    private fun getUnderTrackWires(coordinate: Coordinate): RailroadPowerInterface? {
        val trackCoord = Coordinate(coordinate)
        getRailroadPowerInterfaceAt(trackCoord)?.let { return it }

        val below = Coordinate(coordinate)
        below.y -= 1
        return getRailroadPowerInterfaceAt(below)
    }

    private fun getRailroadPowerInterfaceAt(coordinate: Coordinate): RailroadPowerInterface? {
        val node = NodeManager.instance!!.getNodeFromCoordonate(coordinate)
        if (node is RailroadPowerInterface) {
            return node
        }
        if (node is TransparentNode) {
            val element = node.element
            if (element is RailroadPowerInterface) {
                return element
            }
        }
        if (coordinate.dimension == worldObj.provider.dimensionId) {
            val tile = worldObj.getTileEntity(coordinate.x, coordinate.y, coordinate.z)
            if (tile is ThirdRailTileEntity) {
                val thirdRailNode = tile.node
                if (thirdRailNode is RailroadPowerInterface) {
                    return thirdRailNode
                }
            }
        }
        return null
    }

    override fun interactFirst(player: EntityPlayer): Boolean {
        if (player.isSneaking) return false

        if (riddenByEntity != null && riddenByEntity != player) {
            return true
        }

        if (!worldObj.isRemote) {
            player.mountEntity(this)
        }
        return true
    }

    override fun writeEntityToNBT(tag: NBTTagCompound) {
        super.writeEntityToNBT(tag)
        tag.setDouble("EnergyBufferJ", energyBufferJoules)
        tag.setDouble("EnergyBufferTargetJ", energyBufferTargetJoules)
    }

    override fun readEntityFromNBT(tag: NBTTagCompound) {
        super.readEntityFromNBT(tag)
        if (tag.hasKey("EnergyBufferJ")) {
            energyBufferJoules = tag.getDouble("EnergyBufferJ")
        }
        if (tag.hasKey("EnergyBufferTargetJ")) {
            energyBufferTargetJoules = tag.getDouble("EnergyBufferTargetJ")
        }
    }

    override fun getMinecartType(): Int {
        return 0
    }

    override fun func_145817_o(): Block? {
        return Blocks.iron_block
    }

    override fun func_145820_n(): Block? {
        return Blocks.iron_block
    }
}
