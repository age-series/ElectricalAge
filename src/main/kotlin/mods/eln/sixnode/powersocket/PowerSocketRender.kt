package mods.eln.sixnode.powersocket

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.IOException

class PowerSocketRender(tileEntity: SixNodeEntity?, side: Direction?, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(
        tileEntity!!, side!!, descriptor
    ) {
    var descriptor: PowerSocketDescriptor
    var coord: Coordinate
    var channel: String? = null
    var cableRender: CableRenderDescriptor? = null
    override var inventory = SixNodeElementInventory(1, 64, this)
    var paintColor = 15

    init {
        this.descriptor = descriptor as PowerSocketDescriptor
        coord = Coordinate(tileEntity!!)
    }

    override fun drawCables() {
        setGlColorFromDye(paintColor, 1.0f)
        super.drawCables()
        GL11.glColor3f(1f, 1f, 1f)
    }

    override fun draw() {
        super.draw()
        descriptor.draw(paintColor)
    }

    override fun refresh(deltaT: Float) {}
    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return Eln.instance.lowCurrentCableRender
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return PowerSocketGui(this, player, inventory)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        try {
            channel = stream.readUTF()
            paintColor = stream.readInt()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
