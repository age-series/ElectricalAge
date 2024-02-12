package mods.eln.railroad

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
