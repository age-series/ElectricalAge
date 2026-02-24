package mods.eln.item

import mods.eln.environment.BiomeClimateService
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.misc.Utils
import mods.eln.node.NodeBlock
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class ThermometerDescriptor(name: String) : GenericItemUsingDamageDescriptor(name) {
    override fun onItemUse(
        stack: ItemStack?,
        player: EntityPlayer?,
        world: World?,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        if (player == null || world == null || world.isRemote) {
            return false
        }

        val block = world.getBlock(x, y, z)
        if (block is NodeBlock) {
            return false
        }

        val climate = BiomeClimateService.sample(world, x, y, z)
        val tempC = climate.temperatureCelsius
        val tempF = tempC * 9.0 / 5.0 + 32.0
        val message = buildString {
            append("Biome T: ")
            append(Utils.plotCelsius("", tempC))
            append("(")
            append(Utils.plotValue(tempF, "°F "))
            append(")")
            append(" ")
            append("RH:")
            append(String.format("%.0f%%", climate.relativeHumidityPercent))
        }
        Utils.addChatMessage(player, message)
        return false
    }
}
