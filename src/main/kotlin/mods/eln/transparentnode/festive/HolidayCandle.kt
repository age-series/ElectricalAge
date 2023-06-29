package mods.eln.transparentnode.festive

import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.node.transparent.*
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class HolidayCandleDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, FestiveElement::class.java, HolidayCandleRender::class.java) {
    private var base: Obj3D.Obj3DPart? = null
    private var glass: Obj3D.Obj3DPart? = null
    private var light: Obj3D.Obj3DPart? = null

    init {
        base = obj.getPart("CandleLamp_Cylinder.001")
        glass = obj.getPart("Glass_Cylinder.000")
        light = obj.getPart("LampOn_Cylinder.002")
    }

    fun draw(front: Direction, powered: Boolean) {
        if (base != null && light != null && glass != null) {
            front.glRotateZnRef()
            GL11.glTranslatef(-0.5f, -0.5f, 0.5f)
            //GL11.glScalef(0.5f, 0.5f, 0.5f)
            //UtilsClient.drawLight(led);
            base?.draw()
            UtilsClient.disableCulling()
            if (powered) {
                UtilsClient.drawLight(light)
            }
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            glass?.draw()
            GL11.glDisable(GL11.GL_BLEND)
            UtilsClient.enableCulling()
        }
    }
}

class HolidayCandleRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, transparentNodedescriptor) {

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
        (transparentNodedescriptor as HolidayCandleDescriptor).draw(front!!, powered)
    }
}
