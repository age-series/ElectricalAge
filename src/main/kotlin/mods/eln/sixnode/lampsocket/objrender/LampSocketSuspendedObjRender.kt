package mods.eln.sixnode.lampsocket.objrender

import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.UtilsClient
import mods.eln.sixnode.lampsocket.LampSocketDescriptor
import mods.eln.sixnode.lampsocket.LampSocketRender
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import org.lwjgl.opengl.GL11

class LampSocketSuspendedObjRender(obj: Obj3D, val onOffModel: Boolean, val length: Int, val canSwing: Boolean) : ILampSocketObjRender {

    private val socket = obj.getPart("socket")
    private val chain = obj.getPart("chain")
    private val base = obj.getPart("base")
    private val lightAlphaPlaneNoDepth = obj.getPart("lightAlphaNoDepth")
    private val tOff = obj.getModelResourceLocation(obj.getString("tOff"))
    private val tOn = obj.getModelResourceLocation(obj.getString("tOn"))
    private val chainLength = chain.getFloat("length").toDouble()
    private val chainFactor = chain.getFloat("factor").toDouble()
    private val baseLength = base.getFloat("length").toDouble()

    override fun draw(descriptor: LampSocketDescriptor, type: ItemRenderType, distanceToPlayer: Double) {
        if (type == ItemRenderType.INVENTORY) {
            GL11.glRotated(90.0, 0.0, 0.0, 1.0) // Undo initial rotation
            GL11.glScaled(0.5, 0.5, 0.5)
            GL11.glRotated(90.0, 0.0, 1.0, 0.0)
            GL11.glTranslated(-1.5, 0.0, 0.0)
        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            GL11.glRotated(90.0, 0.0, 0.0, 1.0) // Undo initial rotation
            GL11.glScaled(0.3, 0.3, 0.3)
            GL11.glRotated(45.0, 0.0, 1.0, 0.0)
            GL11.glTranslated(-1.5, 0.0, 0.4)
        }

        draw(LRDU.Up, 0, 0.0, 0.0, distanceToPlayer)
    }

    override fun draw(render: LampSocketRender, distanceToPlayer: Double) {
        draw(render.front!!, render.lightValue, render.perturbPy, render.perturbPz, distanceToPlayer)
    }

    fun draw(front: LRDU, light: Int, perturbPy: Double, perturbPz: Double, distanceToPlayer: Double) {
        // front.glRotateOnX()

        val perturbPy = perturbPy / length.toDouble()
        val perturbPz = perturbPz / length.toDouble()

        base.draw()

        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glTranslated(baseLength, 0.0, 0.0)

        for (idx in 0..<length) {
            if (canSwing) {
                GL11.glRotated(perturbPy, 0.0, 1.0, 0.0)
                GL11.glRotated(perturbPz, 0.0, 0.0, 1.0)
            }

            chain.draw()

            GL11.glTranslated(chainLength, 0.0, 0.0)
        }

        if (canSwing) {
            GL11.glRotated(perturbPy, 0.0, 1.0, 0.0)
            GL11.glRotated(perturbPz, 0.0, 0.0, 1.0)
        }

        GL11.glEnable(GL11.GL_CULL_FACE)

        if (!onOffModel) {
            socket.draw()
        } else {
            if (light >= LampSocketRender.MIN_LIGHT_ON_VALUE) {
                GL11.glColor3d(light / 15.0, light / 15.0, light / 15.0)
                UtilsClient.bindTexture(tOn)
            } else UtilsClient.bindTexture(tOff)

            if (light >= LampSocketRender.MIN_LIGHT_ON_VALUE) UtilsClient.disableLight()

            if (socket != null) socket.drawNoBind()

            if (light >= LampSocketRender.MIN_LIGHT_ON_VALUE) {
                UtilsClient.enableLight()
                GL11.glColor3d(1.0, 1.0, 1.0)
            }
        }

        GL11.glDisable(GL11.GL_CULL_FACE)

        if (lightAlphaPlaneNoDepth != null) {
            // Beautiful effect, but overlay the whole render (i.e. through wall), so distance limited.
            val coefficient = /* 1.5 */ 2.0 - distanceToPlayer

            if (coefficient > 0.0) {
                UtilsClient.enableBlend()
                UtilsClient.disableLight()
                UtilsClient.disableDepthTest()

                GL11.glColor4d(1.0, 1.0, 1.0, light * 0.06667 * coefficient)
                lightAlphaPlaneNoDepth.draw()

                UtilsClient.enableDepthTest()
                UtilsClient.enableLight()
                UtilsClient.disableBlend()
            }
        }

        GL11.glEnable(GL11.GL_CULL_FACE)
    }

}