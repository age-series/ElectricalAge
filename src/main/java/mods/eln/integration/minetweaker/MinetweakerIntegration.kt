package mods.eln.integration.minetweaker

import minetweaker.MineTweakerAPI;

object MinetweakerIntegration {
    fun initialize() {
        MineTweakerAPI.registerClass(Compressor::class.java)
        MineTweakerAPI.registerClass(Macerator::class.java)
        MineTweakerAPI.registerClass(Magnetizer::class.java)
        MineTweakerAPI.registerClass(PlateMachine::class.java)
    }
}
