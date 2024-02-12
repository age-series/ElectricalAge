package mods.eln.railroad

import mods.eln.Eln
import mods.eln.sim.IProcess

class RailroadResistorSlowProcess(val rpi: RailroadPowerInterface, val cart: EntityElectricMinecart, var timeLeft: Double): IProcess {

    override fun process(time: Double) {
        if (timeLeft - time < 0.0) {
            Eln.logger.warn("Automatically unregistered minecart after timeout")
            rpi.deregisterCart(cart)
        }
        timeLeft -= time
    }
}
