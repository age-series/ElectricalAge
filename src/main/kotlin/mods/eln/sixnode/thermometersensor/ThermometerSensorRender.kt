package mods.eln.sixnode.thermometersensor

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import java.io.DataInputStream

class ThermometerSensorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {

    private val thermometerDescriptor = descriptor as ThermometerSensorDescriptor
    private val clientInventory = SixNodeElementInventory(0, 64, this)
    var lowValue = -40f
    var highValue = 50f

    override val inventory = clientInventory

    override fun draw() {
        super.draw()
        drawSignalPin(front!!.right(), thermometerDescriptor.pinDistance)
        thermometerDescriptor.draw()
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)
        lowValue = stream.readFloat()
        highValue = stream.readFloat()
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return ThermometerSensorGui(player, clientInventory, this)
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor {
        return Eln.instance.signalCableDescriptor.render
    }
}
