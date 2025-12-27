package mods.eln.railroad

import mods.eln.Eln
import mods.eln.node.transparent.TransparentNodeElement
import mods.eln.sim.IProcess
import mods.eln.sim.mna.misc.MnaConst

object PoweredMinecartSimulationSingleton {
    val poweredMinecartSimulationData: MutableList<PoweredMinecartSimulationData> = mutableListOf()
    val minecartEnergyCache: MutableMap<EntityElectricMinecart, Double> = mutableMapOf()

    init {
        val energyMover = IProcess { time ->
            poweredMinecartSimulationData.forEach {
                moveEnergy(it.minecart, it.resistor.power * time)
            }
        }
        Eln.simulator.addSlowPostProcess(energyMover)
    }

    /**
     * powerCart - set the power demands of the cart
     * @param cart electric minecart entity instance
     * @param resistance the resistance of the motor
     * @param time time in seconds to request this power for the cart
     */
    fun powerCart(cart: EntityElectricMinecart, resistance: Double, time: Double) {
        val search = poweredMinecartSimulationData.filter { it.minecart == cart }
        if (search.isEmpty()) return
        val cartData = search.first()
        if (cartData.resistor.resistance != resistance)
            if (cartData.owningElement is TransparentNodeElement)
                cartData.owningElement.needPublish()
        // Don't draw power from the overhead lines if the voltage is too low
        if (cartData.resistorElectricalLoad.voltage < 700.0) {
            cartData.resistor.resistance = MnaConst.highImpedance
        } else {
            cartData.resistor.resistance = resistance
        }
        cartData.slowProcess.timeLeft = time
    }

    /**
     * cartCollectEnergy - collect the joules from the wire for the cart
     * @param cart electric minecart entity instance
     * @return the joules of energy from the cable
     */
    fun cartCollectEnergy(cart: EntityElectricMinecart): Double {
        return if (cart in minecartEnergyCache) {
            val currentEnergy = minecartEnergyCache[cart]!!
            minecartEnergyCache.remove(cart)
            currentEnergy
        } else {
            0.0
        }
    }

    /**
     * moveEnergy - move energy to the cart's energy cache for collection by the entity
     * @param cart electric minecart entity instance
     * @param joules the quantity of energy in joules
     */
    private fun moveEnergy(cart: EntityElectricMinecart, joules: Double) {
        minecartEnergyCache[cart] = if (cart in minecartEnergyCache) {
            minecartEnergyCache[cart]!! + joules
        } else {
            joules
        }
    }
}
