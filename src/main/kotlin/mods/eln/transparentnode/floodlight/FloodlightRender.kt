package mods.eln.transparentnode.floodlight

import mods.eln.misc.*
import mods.eln.misc.HybridNodeDirection.*
import mods.eln.node.transparent.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class FloodlightRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor) :
    TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    override val inventory = TransparentNodeElementInventory(2, 64, this)
    private val descriptor = transparentNodeDescriptor as FloodlightDescriptor

    private var rotationAxis = XN
    private var blockFacing = XN
    var motorized = false
    private var powered = false
    var swivelAngle = 0.0
    var headAngle = 0.0
    var beamWidth = 0.0
    private var lamp1Stack: ItemStack? = null
    private var lamp2Stack: ItemStack? = null

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            rotationAxis = HybridNodeDirection.fromInt(stream.readInt())!!
            blockFacing = HybridNodeDirection.fromInt(stream.readInt())!!
            motorized = stream.readBoolean()
            powered = stream.readBoolean()
            swivelAngle = stream.readDouble()
            headAngle = stream.readDouble()
            beamWidth = stream.readDouble()
            lamp1Stack = Utils.unserialiseItemStack(stream)
            lamp2Stack = Utils.unserialiseItemStack(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun draw() {
        glNormalizePlacement(rotationAxis, blockFacing)
        descriptor.draw(swivelAngle, headAngle, lamp1Stack, lamp2Stack, powered)
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return FloodlightGui(player, this)
    }

    private fun glNormalizePlacement(axis: HybridNodeDirection, facing: HybridNodeDirection) {
        when (axis) {
            XN -> {
                GL11.glRotated(-90.0, 0.0, 0.0, 1.0)
                GL11.glRotated(180.0, -1.0, 0.0, 0.0)

                when (facing) {
                    XN -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    XP -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    YN -> GL11.glRotated(0.0, 0.0, 1.0, 0.0)
                    YP -> GL11.glRotated(180.0, 0.0, 1.0, 0.0)
                    ZN -> GL11.glRotated(-90.0, 0.0, 1.0, 0.0)
                    ZP -> GL11.glRotated(90.0, 0.0, 1.0, 0.0)
                }
            }
            XP -> {
                GL11.glRotated(-90.0, 0.0, 0.0, 1.0)
                GL11.glRotated(0.0, -1.0, 0.0, 0.0)

                when (facing) {
                    XN -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    XP -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    YN -> GL11.glRotated(0.0, 0.0, 1.0, 0.0)
                    YP -> GL11.glRotated(180.0, 0.0, 1.0, 0.0)
                    ZN -> GL11.glRotated(90.0, 0.0, 1.0, 0.0)
                    ZP -> GL11.glRotated(-90.0, 0.0, 1.0, 0.0)
                }
            }
            YN -> {
                when (facing) {
                    XN -> {
                        GL11.glRotated(180.0, -1.0, 0.0, 0.0)
                        GL11.glRotated(180.0, 0.0, 1.0, 0.0)
                    }
                    XP -> {
                        GL11.glRotated(180.0, 1.0, 0.0, 0.0)
                        GL11.glRotated(0.0, 0.0, 1.0, 0.0)
                    }
                    YN -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    YP -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    ZN -> {
                        GL11.glRotated(180.0, 0.0, 0.0, -1.0)
                        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
                    }
                    ZP -> {
                        GL11.glRotated(180.0, 0.0, 0.0, 1.0)
                        GL11.glRotated(-90.0, 0.0, 1.0, 0.0)
                    }
                }
            }
            YP -> {
                when (facing) {
                    XN -> {
                        GL11.glRotated(0.0, -1.0, 0.0, 0.0)
                        GL11.glRotated(180.0, 0.0, 1.0, 0.0)
                    }
                    XP -> {
                        GL11.glRotated(0.0, 1.0, 0.0, 0.0)
                        GL11.glRotated(0.0, 0.0, 1.0, 0.0)
                    }
                    YN -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    YP -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    ZN -> {
                        GL11.glRotated(0.0, 0.0, 0.0, -1.0)
                        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
                    }
                    ZP -> {
                        GL11.glRotated(0.0, 0.0, 0.0, 1.0)
                        GL11.glRotated(-90.0, 0.0, 1.0, 0.0)
                    }
                }
            }
            ZN -> {
                GL11.glRotated(-90.0, 0.0, 0.0, 1.0)
                GL11.glRotated(90.0, -1.0, 0.0, 0.0)

                when (facing) {
                    XN -> GL11.glRotated(90.0, 0.0, 1.0, 0.0)
                    XP -> GL11.glRotated(-90.0, 0.0, 1.0, 0.0)
                    YN -> GL11.glRotated(0.0, 0.0, 1.0, 0.0)
                    YP -> GL11.glRotated(180.0, 0.0, 1.0, 0.0)
                    ZN -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    ZP -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                }
            }
            ZP -> {
                GL11.glRotated(-90.0, 0.0, 0.0, 1.0)
                GL11.glRotated(-90.0, -1.0, 0.0, 0.0)

                when (facing) {
                    XN -> GL11.glRotated(-90.0, 0.0, 1.0, 0.0)
                    XP -> GL11.glRotated(90.0, 0.0, 1.0, 0.0)
                    YN -> GL11.glRotated(0.0, 0.0, 1.0, 0.0)
                    YP -> GL11.glRotated(180.0, 0.0, 1.0, 0.0)
                    ZN -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                    ZP -> TODO("Unused - impossible rotation direction. If you get this message there's a bug in the code.")
                }
            }
        }
    }

}