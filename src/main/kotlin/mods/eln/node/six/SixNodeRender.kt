package mods.eln.node.six

import mods.eln.misc.Direction.Companion.fromInt
import mods.eln.misc.UtilsClient.glDefaultColor
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

class SixNodeRender : TileEntitySpecialRenderer() {
    override fun renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, var8: Float) {
        Minecraft.getMinecraft().mcProfiler.startSection("SixNode")
        val tileEntity = entity as SixNodeEntity
        GL11.glPushMatrix()
        GL11.glTranslatef(x.toFloat() + .5f, y.toFloat() + .5f, z.toFloat() + .5f)
        for ((idx, render) in tileEntity.elementRenderList.withIndex()) {
            if (render != null) {
                glDefaultColor()
                GL11.glPushMatrix()
                fromInt(idx)!!.glRotateXnRef()
                GL11.glTranslatef(-0.5f, 0f, 0f)
                render.draw()
                GL11.glPopMatrix()
            }
        }
        GL11.glPopMatrix()
        Minecraft.getMinecraft().mcProfiler.endSection()
    }
}
