package mods.eln.transparentnode.railroad

import mods.eln.entity.carts.EntityElectricMinecart

interface RailroadPowerInterface {
    /**
     * registerCart - register a cart against a power element
     * @param cart electric minecart entity instance
     */
    fun registerCart(cart: EntityElectricMinecart)

    /**
     * deregisterCart - de-register a cart against a power element
     * @param cart electric minecart entity instance
     */
    fun deregisterCart(cart: EntityElectricMinecart)
}
