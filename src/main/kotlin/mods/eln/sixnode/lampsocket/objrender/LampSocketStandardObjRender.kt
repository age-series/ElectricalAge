package mods.eln.sixnode.lampsocket.objrender

import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.sixnode.lampsocket.LampSocketDescriptor
import mods.eln.sixnode.lampsocket.LampSocketRender
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import org.lwjgl.opengl.GL11

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
class LampSocketStandardObjRender(obj: Obj3D, val onOffModel: Boolean) : ILampSocketObjRender {

    private val socket = obj.getPart("socket")
    private val lampOn = obj.getPart("lampOn")
    private val lampOff = obj.getPart("lampOff")
    private val socketUnlightable = obj.getPart("socket_unlightable")
    private val socketLightable = obj.getPart("socket_lightable")
    private val lightAlphaPlane = obj.getPart("lightAlpha")
    private val lightAlphaPlaneNoDepth = obj.getPart("lightAlphaNoDepth")
    private val tOff = obj.getModelResourceLocation(obj.getString("tOff"))
    private val tOn = obj.getModelResourceLocation(obj.getString("tOn"))

    override fun draw(descriptor: LampSocketDescriptor, type: ItemRenderType, distanceToPlayer: Double) {
        if (type == ItemRenderType.INVENTORY) {
            if (descriptor.hasGhostGroup()) {
                GL11.glScaled(0.5, 0.5, 0.5)
                GL11.glRotated(90.0, 0.0, -1.0, 0.0)
                GL11.glTranslated(-1.5, 0.0, 0.0)
            }
        } else if (type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
            if (descriptor.hasGhostGroup()) {
                GL11.glScaled(0.3, 0.3, 0.3)
                GL11.glRotated(90.0, 0.0, -1.0, 0.0)
                GL11.glTranslated(-0.5, 0.0, -1.0)
            }
        }

        draw(LRDU.Up, 0.0, 0, true, 15, distanceToPlayer)
    }

    override fun draw(render: LampSocketRender, distanceToPlayer: Double) {
        val color = if (render.descriptor.paintable) render.paintColor else 15

        draw(render.front!!, render.alphaZ, render.light, render.lampInInventory, color, distanceToPlayer)
    }

    fun draw(front: LRDU, alphaZ: Double, light: Int, hasBulb: Boolean, color: Int, distanceToPlayer: Double) {
        front.glRotateOnX()

        UtilsClient.disableCulling()
        Utils.setGlColorFromLamp(color)

        if (!onOffModel) {
            if (socket != null) socket.draw()
        } else {
            if (light > LampSocketDescriptor.MIN_LIGHT_ON_VALUE) {
                UtilsClient.bindTexture(tOn)
            } else {
                UtilsClient.bindTexture(tOff)
            }

            if (socketUnlightable != null) socketUnlightable.drawNoBind()

            if (light > LampSocketDescriptor.MIN_LIGHT_ON_VALUE) {
                UtilsClient.disableLight()

                // GL11.glColor3d(light / 15.0, light / 15.0, light / 15.0)

                if (socketLightable != null) socketLightable.drawNoBind()

                // GL11.glColor3d(1.0, 1.0, 1.0)
            }

            if (hasBulb) {
                if (light > LampSocketDescriptor.MIN_LIGHT_ON_VALUE) {
                    if (lampOn != null) lampOn.draw()
                } else {
                    if (lampOff != null) lampOff.draw()
                }
            }

            if (socket != null) socket.drawNoBind()

            if (light > LampSocketDescriptor.MIN_LIGHT_ON_VALUE) UtilsClient.enableLight()
        }

        UtilsClient.enableBlend()
        UtilsClient.disableLight()

        if (lightAlphaPlaneNoDepth != null) {
            // Beautiful effect, but overlay the whole render (i.e. through wall), so distance limited.
            val coefficient = /* 1.5 */ 2.0 - distanceToPlayer

            if (coefficient > 0.0) {
                UtilsClient.enableCulling()
                UtilsClient.disableDepthTest()

                GL11.glColor4d(1.0, 1.0, 1.0, light * 0.06667 * coefficient)
                lightAlphaPlaneNoDepth.draw()

                UtilsClient.enableDepthTest()
                UtilsClient.disableCulling()
            }
        }

        if (lightAlphaPlane != null) {
            GL11.glColor4d(1.0, 0.98, 0.92, light * 0.06667)
            lightAlphaPlane.draw()
        }

        UtilsClient.enableLight()
        UtilsClient.disableBlend()
        UtilsClient.enableCulling()
    }

}