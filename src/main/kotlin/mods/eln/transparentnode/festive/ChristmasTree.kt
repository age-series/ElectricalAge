package mods.eln.transparentnode.festive

import mods.eln.ghost.GhostGroup
import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class ChristmasTreeDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, FestiveElement::class.java, ChristmasTreeRender::class.java) {
    private var star: Obj3D.Obj3DPart? = null
    private var string1: Obj3D.Obj3DPart? = null
    private var string2: Obj3D.Obj3DPart? = null
    private var tree: Obj3D.Obj3DPart? = null

    init {
        star = obj.getPart("StarOn_Star.002")
        string1 = obj.getPart("Strip1_Star.000")
        string2 = obj.getPart("Strip2_Star.001")
        tree = obj.getPart("Tree_Cone.006")
        val gg = GhostGroup()
        gg.addRectangle(0, 2, 0, 1, -1, 1)
        gg.addElement(1, 2, 0)
        gg.addElement(1, 3, 0)
        gg.removeElement(0, 0, 0)
        ghostGroup = gg
    }

    fun draw(front: Direction, delta: Int, powered: Boolean) {
        if (star != null && tree != null && string1 != null && string2 != null) {
            front.glRotateZnRef()
            GL11.glTranslatef(0.5f, -0.5f, 0.5f)
            if (powered) {
                UtilsClient.drawLight(star)
                if (delta > 10) {
                    UtilsClient.drawLight(string2)
                    string1?.draw()
                } else {
                    UtilsClient.drawLight(string1)
                    string2?.draw()
                }
            } else {
                star?.draw()
                string1?.draw()
                string2?.draw()
            }
            tree?.draw()
        }
    }
}

class ChristmasTreeRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, transparentNodedescriptor) {
    var x = 0
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
        (transparentNodedescriptor as ChristmasTreeDescriptor).draw(front!!, x, powered)
    }

    override fun refresh(deltaT: Float) {
        x += 1
        if (x > 20) x = 0
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }
}
