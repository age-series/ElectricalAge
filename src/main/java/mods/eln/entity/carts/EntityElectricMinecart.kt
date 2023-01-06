package mods.eln.entity.carts

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils
import mods.eln.node.NodeManager
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.transparentnode.railroad.OverheadLinesElement
import mods.eln.transparentnode.railroad.PoweredMinecartSimulationSingleton
import mods.eln.transparentnode.railroad.RailroadPowerInterface
import mods.eln.transparentnode.railroad.UnderTrackPowerElement
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.server.MinecraftServer
import net.minecraft.world.World
import kotlin.math.abs
import kotlin.math.sign

class EntityElectricMinecart(world: World, x: Double, y: Double, z: Double): EntityMinecart(world, x, y, z) {

    private var lastPowerElement: RailroadPowerInterface? = null
    private val locomotiveMaximumResistance = 200.0
    var energyBufferTargetJoules = 10_000.0
    var energyBufferJoules = 0.0

    var count = 0

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
                PoweredMinecartSimulationSingleton.powerCart(this, locomotiveMaximumResistance, 0.1)
            } else {
                PoweredMinecartSimulationSingleton.powerCart(this, MnaConst.highImpedance, 1.0)
            }
            energyBufferJoules += PoweredMinecartSimulationSingleton.cartCollectEnergy(this)
        }

        if (count > 20) count = 0
        if (count == 0) {
            MinecraftServer.getServer().entityWorld.playerEntities.forEach {it ->
                val player = it as EntityPlayer
                Utils.addChatMessage(player, "Cart Energy: ${Utils.plotEnergy(energyBufferJoules)}")
            }
        }
        count++

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

    private fun getOverheadWires(coordinate: Coordinate): OverheadLinesElement? {
        // Pass coordinate of tracks and check vertically the next 3-4 blocks
        val originalY = coordinate.y
        while (coordinate.y <= (originalY + 4)) {
            coordinate.y
            val node = NodeManager.instance!!.getTransparentNodeFromCoordinate(coordinate)
            if (node is OverheadLinesElement) {
                return node
            }
            coordinate.y++
        }
        return null
    }

    private fun getUnderTrackWires(coordinate: Coordinate): UnderTrackPowerElement? {
        coordinate.y -= 1 // check the block below the cart
        val node = NodeManager.instance!!.getTransparentNodeFromCoordinate(coordinate)
        if (node is UnderTrackPowerElement) {
            return node
        }
        return null
    }

    override fun getMinecartType(): Int {
        return -1
    }

    override fun func_145817_o(): Block? {
        return Blocks.iron_block
    }

    override fun func_145820_n(): Block? {
        return Blocks.iron_block
    }
}
