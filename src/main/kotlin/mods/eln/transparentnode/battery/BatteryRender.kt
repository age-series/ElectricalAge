package mods.eln.transparentnode.battery

import mods.eln.misc.Direction
import mods.eln.misc.LRDUMask
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeElementRender
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import java.io.DataInputStream
import java.io.IOException

class BatteryRender(tileEntity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : TransparentNodeElementRender(tileEntity, descriptor) {
    var energy = 0f
    var life = 0f
    var descriptor: BatteryDescriptor = descriptor as BatteryDescriptor
    var plus = false
    var minus = false
    var lrdu = LRDUMask()
    var power = 0f

    override fun draw() {
        front?.glRotateXnRef()
        descriptor.draw(plus, minus)
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            power = stream.readFloat()
            energy = stream.readFloat()
            life = stream.readShort() / 1000.0f
            lrdu.deserialize(stream)
            plus = true
            minus = true
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return BatteryGuiDraw(this)
    }
}
