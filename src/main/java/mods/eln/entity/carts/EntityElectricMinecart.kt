package mods.eln.entity.carts

import mods.eln.misc.Coordinate
import mods.eln.node.NodeManager
import mods.eln.transparentnode.railroad.GenericRailroadPowerElement
import mods.eln.transparentnode.railroad.OverheadLinesElement
import mods.eln.transparentnode.railroad.UnderTrackPowerElement
import net.minecraft.block.Block
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.init.Blocks
import net.minecraft.world.World

class EntityElectricMinecart(world: World, x: Double, y: Double, z: Double): EntityMinecart(world, x, y, z) {

    private var lastPowerElement: GenericRailroadPowerElement? = null
    private val locomotiveResistance = 500.0

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

        val cartCoordinate = Coordinate(posX.toInt(), posY.toInt(), posZ.toInt(), worldObj)
        val overheadWires = getOverheadWires(cartCoordinate)
        val underTrackWires = getUnderTrackWires(cartCoordinate)

        /**
         * Minecarts don't trigger an event in this class when they are removed in creative mode
         * This means we need to use the actual element of the Overhead Lines or Under Track Power classes to handle
         * the resistors and that no MNA elements can be owned by this class.
         *
         * The Replicator does this by registering a process that will remove the resistor from the simulator thread
         * periodically and registers that apart from the entity (ish) although it probably isn't tested well and
         * theoretically a creative mode removed replicator could be buggy or something
         *
         * Anyhow. Best way to implement this is to ask for the nearest wires (prefer the under track ones) and then
         * pull power from that and save that. Might be able to make some generics here but I think that there
         * may need to be differing code for sending the under power track rendering as opposed to the over track ones.
         * This is because I want to render a wire _above_ the block if there's a track there. Presumably by rendering
         * a simple quad over the top of the rails by a small distance and detecting the rail rendering for the rail
         * above the device to make sure that we 'follow' the track with the custom renderer.
         *
         * This will have to be raw OpenGL code, similar to how the CableRender class works. It's probably not going
         * to be super fun but it does help that the code will be 2D quads (rendered in 3D space/orientation).
         *
         * Some various textures may be required but I'm hoping I can sorta follow the cable render by rendering a quad
         * in each direction and possibly also a center quad that is always rendered - and making it render diagonally
         * in 3D space if the rail is going up or something. Turns should also be detectable. Might need to do something
         * for mods that add tracks or have a default of just rendering the center square (2x2 pixels in MC) as wire.
         */
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
        return 2
    }

    override fun func_145817_o(): Block? {
        return Blocks.iron_block
    }
}
