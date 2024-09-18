package mods.eln.transparentnode.floodlight

import mods.eln.sim.IProcess
import kotlin.math.abs

class FloodlightProcess(var element: FloodlightElement) : IProcess {

    override fun process(time: Double) {
        if (element.motorized) {
            element.swivelAngle = (element.swivelControl.normalized).toFloat() * 360f
            element.headAngle = (element.headControl.normalized).toFloat() * 180f
        }

        val lamp1Stack = element.inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_1_ID)
        val lamp2Stack = element.inventory.getStackInSlot(FloodlightContainer.LAMP_SLOT_2_ID)

        if (lamp1Stack != null || lamp2Stack != null) element.node!!.lightValue = (((abs(element.electricalLoad.voltage) - 150) / 3.3333).toInt()).coerceIn(0, 15)
        else element.node!!.lightValue = 0

        element.powered = element.node!!.lightValue > 8
        if ((lamp1Stack != null && lamp2Stack == null) || (lamp1Stack == null && lamp2Stack != null)) element.node!!.lightValue /= 2
    }

}