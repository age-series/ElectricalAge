package mods.eln.transparentnode.festive

import mods.eln.ghost.GhostGroup
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

class ChristmasTreeDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, ChristmasTreeElemnent::class.java, ChristmasTreeRender::class.java) {
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

    fun draw(front: Direction, delta: Int) {
        if (star != null && tree != null && string1 != null && string2 != null) {
            front.glRotateZnRef()
            GL11.glTranslatef(0.5f, -0.5f, 0.5f)
            if (delta > 10) {
                UtilsClient.drawLight(star)
                UtilsClient.drawLight(string2)
                string1?.draw()
            } else {
                star?.draw()
                UtilsClient.drawLight(string1)
                string2?.draw()
            }
            tree?.draw()
        }
    }
}

class ChristmasTreeElemnent(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val loadResistor = Resistor(electricalLoad, null)

    init {
        loadResistor.r = 15.0
        node.lightValue = 8
    }

    override fun thermoMeterString(side: Direction?): String {
        return ""
    }

    override fun multiMeterString(side: Direction?): String {
        return ""
    }

    override fun getElectricalLoad(side: Direction?, lrdu: LRDU?): ElectricalLoad? {
        return electricalLoad
    }

    override fun onBlockActivated(entityPlayer: EntityPlayer?, side: Direction?, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction?, lrdu: LRDU?): Int {
        return NodeBase.maskElectricalPower
    }

    override fun getThermalLoad(side: Direction?, lrdu: LRDU?): ThermalLoad? {
        return null
    }

    override fun initialize() {
        connect()
    }
}

class ChristmasTreeRender(val tileEntity: TransparentNodeEntity, val descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, descriptor) {

    var x = 0

    override fun draw() {
        (descriptor as ChristmasTreeDescriptor).draw(front, x)
    }

    override fun refresh(deltaT: Float) {
        x += 1
        if (x > 20) x = 0
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }
}
