package mods.eln.transparentnode.festive

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


class HolidayCandleDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, HolidayCandleElement::class.java, HolidayCandleRender::class.java) {
    private var base: Obj3D.Obj3DPart? = null
    private var glass: Obj3D.Obj3DPart? = null
    private var light: Obj3D.Obj3DPart? = null

    init {
        base = obj.getPart("CandleLamp_Cylinder.001")
        glass = obj.getPart("Glass_Cylinder.000")
        light = obj.getPart("LampOn_Cylinder.002")
    }

    fun draw(front: Direction) {
        if (base != null && light != null && glass != null) {
            front.glRotateZnRef()
            GL11.glTranslatef(-0.5f, -0.5f, 0.5f)
            //GL11.glScalef(0.5f, 0.5f, 0.5f)
            //UtilsClient.drawLight(led);
            base?.draw()
            UtilsClient.disableCulling()
            UtilsClient.drawLight(light)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            glass?.draw()
            GL11.glDisable(GL11.GL_BLEND)
            UtilsClient.enableCulling()
        }
    }
}

class HolidayCandleElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

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

class HolidayCandleRender(val tileEntity: TransparentNodeEntity, val descriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, descriptor) {
    override fun draw() {
        (descriptor as HolidayCandleDescriptor).draw(front)
    }
}
