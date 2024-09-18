package mods.eln.transparentnode.floodlight

import mods.eln.misc.*
import mods.eln.node.Synchronizable
import mods.eln.node.transparent.*
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import java.io.DataInputStream
import java.io.IOException

class FloodlightRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    override val inventory = TransparentNodeElementInventory(2, 64, this)
    private val descriptor = transparentNodeDescriptor as FloodlightDescriptor

    private var rotationAxis = HybridNodeDirection.XN
    private var blockFacing = HybridNodeDirection.XN
    var motorized = false
    private var powered: Boolean = false
    var swivelAngle = Synchronizable(0f)
    var headAngle = Synchronizable(0f)
    var coneWidth = FloodlightConeWidth.NARROW
    var coneRange = FloodlightConeRange.NEAR
    private var lamp1Stack: ItemStack? = null
    private var lamp2Stack: ItemStack? = null

    private val swivelAnimate = PhysicalInterpolatorNoRebound(1.0f, 2.0f, 2.0f)
    private val headAnimate = PhysicalInterpolatorNoRebound(1.0f, 2.0f, 2.0f)
    private var boot = true

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
        blockFacing.glNormalizePlacement(rotationAxis)
        descriptor.draw(swivelAnimate.get(), headAnimate.get(), lamp1Stack, lamp2Stack, powered)
    }

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

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return FloodlightGui(player, inventory, this)
    }

}