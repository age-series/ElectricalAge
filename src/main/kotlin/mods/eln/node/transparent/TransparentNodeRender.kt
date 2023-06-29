package mods.eln.node.transparent

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

class TransparentNodeRender : TileEntitySpecialRenderer() {
    override fun renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, var8: Float) {
        val tileEntity = entity as TransparentNodeEntity
        if (tileEntity.elementRender == null) return
        GL11.glPushMatrix()
        GL11.glTranslatef(x.toFloat() + .5f, y.toFloat() + .5f, z.toFloat() + .5f)
        tileEntity.elementRender!!.draw()
        GL11.glPopMatrix()
    }
}
