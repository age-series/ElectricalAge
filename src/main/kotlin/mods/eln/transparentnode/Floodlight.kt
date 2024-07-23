package mods.eln.transparentnode

import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11

class BasicFloodlightDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, BasicFloodlightElement::class.java, BasicFloodlightRender::class.java) {
    val base: Obj3D.Obj3DPart
    val swivel: Obj3D.Obj3DPart
    val head: Obj3D.Obj3DPart
    val bulb1_off: Obj3D.Obj3DPart
    val bulb2_off: Obj3D.Obj3DPart
    val bulb1_on: Obj3D.Obj3DPart
    val bulb2_on: Obj3D.Obj3DPart

    init {
        base = obj.getPart("Lamp_Base_Cube.008")
        swivel = obj.getPart("Lamp_Swivel_Cube.014")
        head = obj.getPart("Lamp_Head_Cylinder.004")
        bulb1_off = obj.getPart("Lamp1_OFF_Cylinder.003")
        bulb2_off = obj.getPart("Lamp2_OFF_Cylinder.002")
        bulb1_on = obj.getPart("Lamp1_ON_Cylinder.000")
        bulb2_on = obj.getPart("Lamp2_ON_Cylinder.001")
    }

    fun draw(front: Direction, x: Double, y: Double) {
        front.glRotateZnRefInv()
        GL11.glTranslated(-0.5, -0.5, 0.5)
        base.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(y, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        swivel.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(-x, 1.0, 0.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        head.draw()
        bulb1_off.draw()
        bulb2_off.draw()
    }
}

class BasicFloodlightElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {
    override fun thermoMeterString(side: Direction): String {
        return ""
    }

    override fun multiMeterString(side: Direction): String {
        return ""
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return null
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun initialize() {

    }
}

class BasicFloodlightRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, transparentNodedescriptor) {
    var x: Double = 0.0
    var y: Double = 0.0

    private var countUp: Boolean = true

    override fun draw() {
        (transparentNodedescriptor as BasicFloodlightDescriptor).draw(front!!, x, y)
    }

    override fun refresh(deltaT: Float) {
        val posShiftX: Float = deltaT * 5
        val posShiftY: Float = deltaT * 10

        if (countUp) x += posShiftX
        else x -= posShiftX
        if (x > 180 || x < 0) countUp = !countUp

        y += posShiftY
        if (y > 360) y = 0.0
    }
}

class MotorizedFloodlightDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, MotorizedFloodlightElement::class.java, MotorizedFloodlightRender::class.java) {
    val base: Obj3D.Obj3DPart
    val swivel: Obj3D.Obj3DPart
    val head: Obj3D.Obj3DPart
    val bulb1_off: Obj3D.Obj3DPart
    val bulb2_off: Obj3D.Obj3DPart
    val bulb1_on: Obj3D.Obj3DPart
    val bulb2_on: Obj3D.Obj3DPart

    init {
        base = obj.getPart("Lamp_Base_Cube.008")
        swivel = obj.getPart("Lamp_Swivel_Cube.014")
        head = obj.getPart("Lamp_Head_Cylinder.004")
        bulb1_off = obj.getPart("Lamp1_OFF_Cylinder.003")
        bulb2_off = obj.getPart("Lamp2_OFF_Cylinder.002")
        bulb1_on = obj.getPart("Lamp1_ON_Cylinder.000")
        bulb2_on = obj.getPart("Lamp2_ON_Cylinder.001")
    }

    fun draw(front: Direction, x: Double, y: Double) {
        front.glRotateZnRefInv()
        GL11.glTranslated(-0.5, -0.5, 0.5)
        base.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(y, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        swivel.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(-x, 1.0, 0.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        head.draw()
        bulb1_off.draw()
        bulb2_off.draw()
    }
}

class MotorizedFloodlightElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {
    override fun thermoMeterString(side: Direction): String {
        return ""
    }

    override fun multiMeterString(side: Direction): String {
        return ""
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return null
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun initialize() {

    }
}

class MotorizedFloodlightRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, transparentNodedescriptor) {
    var x: Double = 0.0
    var y: Double = 0.0

    private var countUp: Boolean = true

    override fun draw() {
        (transparentNodedescriptor as MotorizedFloodlightDescriptor).draw(front!!, x, y)
    }

    override fun refresh(deltaT: Float) {
        val posShiftX: Float = deltaT * 5
        val posShiftY: Float = deltaT * 10

        if (countUp) x += posShiftX
        else x -= posShiftX
        if (x > 180 || x < 0) countUp = !countUp

        y += posShiftY
        if (y > 360) y = 0.0
    }
}
