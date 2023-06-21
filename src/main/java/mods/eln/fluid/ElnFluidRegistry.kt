package mods.eln.fluid

import net.minecraft.block.material.Material


@Suppress("EnumEntryName")
enum class ElnFluidRegistry(
    val material: Material,
    val color: Int,
    val density: Int,
    val viscosity: Int,
    val luminosity: Int,
    val temperature: Int,
    val isGaseous: Boolean,
    val isBucketable: Boolean
) {
    //name(Material,Color,Density,Viscosity, luminosity, isGaseous, isBucktable),
    hot_water(Material.water,4644607, 1000, 1000, 0, 333, false, true),
    cold_water(Material.water,4644607, 1000, 1000, 0, 288, false, true)
}
