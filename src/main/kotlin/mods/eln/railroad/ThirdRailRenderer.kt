package mods.eln.railroad

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import mods.eln.misc.Direction
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

@SideOnly(Side.CLIENT)
class ThirdRailRenderer : TileEntitySpecialRenderer() {

    override fun renderTileEntityAt(tile: TileEntity?, x: Double, y: Double, z: Double, partialTicks: Float) {
        if (tile !is ThirdRailTileEntity) return
        val world = tile.worldObj ?: return
        val block = tile.blockType as? ThirdRailBlock ?: return
        val metadata = world.getBlockMetadata(tile.xCoord, tile.yCoord, tile.zCoord)
        if (metadata != 0 && metadata != 1) return

        val icon = block.getIcon(0, metadata) ?: return

        val axis = if (metadata == 0) Axis.Z else Axis.X
        val side = determineSide(axis, tile.front)

        val pixelSizeU = (icon.maxU - icon.minU) / 16f
        val pixelSizeV = (icon.maxV - icon.minV) / 16f
        val sliceWidth = pixelSizeU * 3f
        val uBottom = icon.minU
        val uTop = uBottom + sliceWidth
        val vStart = icon.minV
        val vEnd = icon.maxV

        val brightness = world.getLightBrightnessForSkyBlocks(tile.xCoord, tile.yCoord, tile.zCoord, 0)
        val sky = (brightness % 65536).toFloat()
        val blockLight = (brightness / 65536).toFloat()
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, sky, blockLight)

        this.bindTexture(TextureMap.locationBlocksTexture)
        GL11.glPushMatrix()
        GL11.glTranslated(x, y, z)
        val wasCull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glColor4f(1f, 1f, 1f, 1f)

        val tessellator = Tessellator.instance
        tessellator.startDrawingQuads()
        val stripHeight = 3.0 / 16.0
        val railHeightBottom = 0.0
        val railHeightTop = railHeightBottom + stripHeight
        val epsilon = 0.001
        val inset = 1.0 / 16.0

        if (axis == Axis.Z) {
            val pos = if (side == Direction.XP) 1.0 - inset else inset
            drawPlaneAlongZ(tessellator, pos, railHeightTop, railHeightBottom, uBottom, uTop, vStart, vEnd, false)
            drawPlaneAlongZ(tessellator, pos + if (side == Direction.XP) -(epsilon * 2) else (epsilon * 2), railHeightTop, railHeightBottom, uBottom, uTop, vStart, vEnd, true)
        } else {
            val pos = if (side == Direction.ZP) 1.0 - inset else inset
            drawPlaneAlongX(tessellator, pos, railHeightTop, railHeightBottom, uBottom, uTop, vStart, vEnd, false)
            drawPlaneAlongX(tessellator, pos + if (side == Direction.ZP) -(epsilon * 2) else (epsilon * 2), railHeightTop, railHeightBottom, uBottom, uTop, vStart, vEnd, true)
        }

        tessellator.draw()
        if (wasCull) {
            GL11.glEnable(GL11.GL_CULL_FACE)
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE)
        }
        GL11.glPopMatrix()
    }

    private fun drawPlaneAlongZ(
        tessellator: Tessellator,
        xPos: Double,
        yTop: Double,
        yBottom: Double,
        uBottom: Float,
        uTop: Float,
        vStart: Float,
        vEnd: Float,
        invert: Boolean
    ) {
        val zStart = 0.0
        val zEnd = 1.0
        if (!invert) {
            tessellator.addVertexWithUV(xPos, yTop, zEnd, uTop.toDouble(), vEnd.toDouble())
            tessellator.addVertexWithUV(xPos, yTop, zStart, uTop.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xPos, yBottom, zStart, uBottom.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xPos, yBottom, zEnd, uBottom.toDouble(), vEnd.toDouble())
        } else {
            tessellator.addVertexWithUV(xPos, yTop, zStart, uTop.toDouble(), vEnd.toDouble())
            tessellator.addVertexWithUV(xPos, yTop, zEnd, uTop.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xPos, yBottom, zEnd, uBottom.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xPos, yBottom, zStart, uBottom.toDouble(), vEnd.toDouble())
        }
    }

    private fun drawPlaneAlongX(
        tessellator: Tessellator,
        zPos: Double,
        yTop: Double,
        yBottom: Double,
        uBottom: Float,
        uTop: Float,
        vStart: Float,
        vEnd: Float,
        invert: Boolean
    ) {
        val xStart = 0.0
        val xEnd = 1.0
        if (!invert) {
            tessellator.addVertexWithUV(xEnd, yTop, zPos, uTop.toDouble(), vEnd.toDouble())
            tessellator.addVertexWithUV(xStart, yTop, zPos, uTop.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xStart, yBottom, zPos, uBottom.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xEnd, yBottom, zPos, uBottom.toDouble(), vEnd.toDouble())
        } else {
            tessellator.addVertexWithUV(xStart, yTop, zPos, uTop.toDouble(), vEnd.toDouble())
            tessellator.addVertexWithUV(xEnd, yTop, zPos, uTop.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xEnd, yBottom, zPos, uBottom.toDouble(), vStart.toDouble())
            tessellator.addVertexWithUV(xStart, yBottom, zPos, uBottom.toDouble(), vEnd.toDouble())
        }
    }

    private fun determineSide(axis: Axis, front: Direction?): Direction {
        return when (axis) {
            Axis.Z -> when (front) {
                Direction.XN -> Direction.XN
                Direction.ZN -> Direction.XN
                Direction.ZP -> Direction.XP
                Direction.XP -> Direction.XP
                else -> Direction.XP
            }
            Axis.X -> when (front) {
                Direction.ZN -> Direction.ZN
                Direction.XN -> Direction.ZN
                Direction.XP -> Direction.ZP
                Direction.ZP -> Direction.ZP
                else -> Direction.ZP
            }
        }
    }

    private enum class Axis {
        X, Z
    }
}
