package mods.eln.transparentnode.floodlight

import mods.eln.misc.*
import mods.eln.misc.HybridNodeDirection.*
import mods.eln.node.Synchronizable
import mods.eln.node.transparent.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class FloodlightRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    override val inventory = TransparentNodeElementInventory(2, 64, this)
    private val descriptor = transparentNodeDescriptor as FloodlightDescriptor

    private var rotationAxis = XN
    private var blockFacing = XN
    var motorized = false
    private var powered: Boolean = false
    var swivelAngle = Synchronizable(0f)
    var headAngle = Synchronizable(0f)
    var coneWidth = FloodlightConeWidth.NARROW
    var coneRange = FloodlightConeRange.NEAR
    private var lamp1Stack: ItemStack? = null
    private var lamp2Stack: ItemStack? = null
/*
    private val swivelAnimate = PhysicalInterpolatorNoRebound(1.0f, 2.0f, 2.0f)
    private val headAnimate = PhysicalInterpolatorNoRebound(1.0f, 2.0f, 2.0f)
    private var boot = true
*/
    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            rotationAxis = HybridNodeDirection.fromInt(stream.readInt())!!
            blockFacing = HybridNodeDirection.fromInt(stream.readInt())!!
            motorized = stream.readBoolean()
            powered = stream.readBoolean()
            swivelAngle.value = stream.readFloat()
            headAngle.value = stream.readFloat()
            coneWidth = FloodlightConeWidth.fromInt(stream.readInt())!!
            coneRange = FloodlightConeRange.fromInt(stream.readInt())!!
            lamp1Stack = Utils.unserialiseItemStack(stream)
            lamp2Stack = Utils.unserialiseItemStack(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun draw() {
        glNormalizePlacement(rotationAxis, blockFacing)
        // descriptor.draw(swivelAnimate.get(), headAnimate.get(), lamp1Stack, lamp2Stack, powered)
        descriptor.draw(swivelAngle.value / 360f, headAngle.value / 180f, lamp1Stack, lamp2Stack, powered)
    }
/*
    override fun refresh(deltaT: Float) {
        if (boot) {
            swivelAnimate.setPos(swivelAngle.value / 360f)
            headAnimate.setPos(headAngle.value / 180f)
            boot = false
        }
        else {
            if (swivelAnimate.target != swivelAngle.value / 360f) swivelAnimate.target = swivelAngle.value / 360f
            if (headAnimate.target != headAngle.value / 180f) headAnimate.target = headAngle.value / 180f
        }

        swivelAnimate.step(deltaT)
        headAnimate.step(deltaT)
    }
*/
    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return FloodlightGui(player, inventory, this)
    }

    private fun glNormalizePlacement(axis: HybridNodeDirection, facing: HybridNodeDirection) {
        when (axis) {
            XN -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(180f, -1f, 0f, 0f)
                when (facing) {
                    XN -> TODO("unused - impossible rotation direction")
                    XP -> TODO("unused - impossible rotation direction")
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                    ZP -> GL11.glRotatef(90f, 0f, 1f, 0f)
                }
            }
            XP -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(0f, -1f, 0f, 0f)
                when (facing) {
                    XN -> TODO("unused - impossible rotation direction")
                    XP -> TODO("unused - impossible rotation direction")
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> GL11.glRotatef(90f, 0f, 1f, 0f)
                    ZP -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                }
            }
            YN -> {
                when (facing) {
                    XN -> {
                        GL11.glRotatef(180f, -1f, 0f, 0f)
                        GL11.glRotatef(180f, 0f, 1f, 0f)
                    }
                    XP -> {
                        GL11.glRotatef(180f, 1f, 0f, 0f)
                        GL11.glRotatef(0f, 0f, 1f, 0f)
                    }
                    YN -> TODO("unused - impossible rotation direction")
                    YP -> TODO("unused - impossible rotation direction")
                    ZN -> {
                        GL11.glRotatef(180f, 0f, 0f, -1f)
                        GL11.glRotatef(90f, 0f, 1f, 0f)
                    }
                    ZP -> {
                        GL11.glRotatef(180f, 0f, 0f, 1f)
                        GL11.glRotatef(-90f, 0f, 1f, 0f)
                    }
                }
            }
            YP -> {
                when (facing) {
                    XN -> {
                        GL11.glRotatef(0f, -1f, 0f, 0f)
                        GL11.glRotatef(180f, 0f, 1f, 0f)
                    }
                    XP -> {
                        GL11.glRotatef(0f, 1f, 0f, 0f)
                        GL11.glRotatef(0f, 0f, 1f, 0f)
                    }
                    YN -> TODO("unused - impossible rotation direction")
                    YP -> TODO("unused - impossible rotation direction")
                    ZN -> {
                        GL11.glRotatef(0f, 0f, 0f, -1f)
                        GL11.glRotatef(90f, 0f, 1f, 0f)
                    }
                    ZP -> {
                        GL11.glRotatef(0f, 0f, 0f, 1f)
                        GL11.glRotatef(-90f, 0f, 1f, 0f)
                    }
                }
            }
            ZN -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(90f, -1f, 0f, 0f)
                when (facing) {
                    XN -> GL11.glRotatef(90f, 0f, 1f, 0f)
                    XP -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> TODO("unused - impossible rotation direction")
                    ZP -> TODO("unused - impossible rotation direction")
                }
            }
            ZP -> {
                GL11.glRotatef(-90f, 0f, 0f, 1f)
                GL11.glRotatef(-90f, -1f, 0f, 0f)
                when (facing) {
                    XN -> GL11.glRotatef(-90f, 0f, 1f, 0f)
                    XP -> GL11.glRotatef(90f, 0f, 1f, 0f)
                    YN -> GL11.glRotatef(0f, 0f, 1f, 0f)
                    YP -> GL11.glRotatef(180f, 0f, 1f, 0f)
                    ZN -> TODO("unused - impossible rotation direction")
                    ZP -> TODO("unused - impossible rotation direction")
                }
            }
        }
    }

}