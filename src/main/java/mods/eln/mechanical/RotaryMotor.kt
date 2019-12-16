package mods.eln.mechanical

import mods.eln.fluid.FuelRegistry
import mods.eln.misc.Obj3D
import org.lwjgl.opengl.GL11

class RotaryMotorDescriptor(basename: String, obj: Obj3D) :
    TurbineDescriptor(basename, obj) {
    override val inertia = 3f
    override val fluidConsumption = 24f
    override val fluidDescription = "gasoline"
    override val fluidTypes = FuelRegistry.gasolineList + FuelRegistry.gasList
    override val efficiencyCurve = 1.5f
    override val sound = "eln:RotaryEngine"
    override val obj = obj
    override val static = arrayOf(
        obj.getPart("Body_Cylinder.001")
    )
    override val rotating = arrayOf(
        obj.getPart("Shaft")
    )
    override fun preDraw() {
        GL11.glTranslated(-0.5, -1.5, 0.5)
    }
}
