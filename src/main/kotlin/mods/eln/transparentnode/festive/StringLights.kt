package mods.eln.transparentnode.festive

import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class StringLightsDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, FestiveElement::class.java, StringLightsRender::class.java) {
    private var base: Obj3D.Obj3DPart? = null
    private var light: Obj3D.Obj3DPart? = null

    init {
        base = obj.getPart("Lights_Cube.009")
        light = obj.getPart("LightOn_Cube.002")
    }

    fun draw(front: Direction, powered: Boolean) {
        if (base != null && light != null) {
            front.glRotateZnRef()
            GL11.glRotatef(180.0f, 0f, 1f, 0f)
            GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
            base?.draw()
            if (powered)
                UtilsClient.drawLight(light)
        }
    }

    override fun mustHaveWall() = true
    override fun mustHaveFloor() = false

    /*

    TODO: Fix Hitbox

    override fun addCollisionBoxesToList(par5AxisAlignedBB: AxisAlignedBB, list: MutableList<AxisAlignedBB>, world: World?, x: Int, y: Int, z: Int) {
        val bb = Blocks.stone.getCollisionBoundingBoxFromPool(world, x, y, z)
        bb.maxZ -= 0.5
        if (par5AxisAlignedBB.intersectsWith(bb)) list.add(bb)
    }
     */
}

class StringLightsRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, transparentNodedescriptor) {

    var powered = false

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            powered = stream.readBoolean()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun draw() {
        (transparentNodedescriptor as StringLightsDescriptor).draw(front!!, powered)
    }
}


