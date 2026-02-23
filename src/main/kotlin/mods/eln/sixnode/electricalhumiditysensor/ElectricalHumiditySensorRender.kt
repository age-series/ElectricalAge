package mods.eln.sixnode.electricalhumiditysensor

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity

class ElectricalHumiditySensorRender(tileEntity: SixNodeEntity, side: Direction, descriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, descriptor) {

    private val humidityDescriptor = descriptor as ElectricalHumiditySensorDescriptor

    override fun draw() {
        super.draw()
        drawSignalPin(front!!.right(), humidityDescriptor.pinDistance)
        humidityDescriptor.draw()
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor {
        return Eln.instance.signalCableDescriptor.render
    }
}
