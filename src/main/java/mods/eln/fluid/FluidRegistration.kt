package mods.eln.fluid

import cpw.mods.fml.common.registry.GameRegistry
import mods.eln.Eln.*
import net.minecraft.block.Block
import net.minecraft.init.Items
import net.minecraft.item.ItemBucket
import net.minecraft.item.ItemStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidRegistry

fun registerElnFluids() {
    ElnFluidRegistry.values().forEach {
        val fluid = Fluid(it.name).setDensity(it.density).setViscosity(it.viscosity).setLuminosity(it.luminosity)
            .setTemperature(it.temperature).setGaseous(it.isGaseous)
        FluidRegistry.registerFluid(fluid)
        val fluidBlock: Block
        if (!fluid.canBePlacedInWorld()) {
            fluidBlock = BlockElnFluid(it.name, fluid, it.material, it.color)
            fluid.setBlock(fluidBlock)
            fluid.setUnlocalizedName(fluidBlock.unlocalizedName.substring(5))
            fluids[ElnFluidRegistry.valueOf(it.name)] = fluid
            fluidBlocks[ElnFluidRegistry.valueOf(it.name)] = fluidBlock
            if (it.isBucketable) {
                val fb = ItemBucket(fluidBlock)
                val bucketName = "${it.name}_bucket"
                val bucketTextureName = "$MODID:${it.name}_bucket"
                fb.setUnlocalizedName(bucketName).setContainerItem(Items.bucket)
                fb.setTextureName(bucketTextureName)
                GameRegistry.registerItem(fb, bucketName)
                FluidContainerRegistry.registerFluidContainer(fluid, ItemStack(fb), ItemStack(Items.bucket))
                BucketHandler.buckets[fluidBlock] = fb
                MinecraftForge.EVENT_BUS.register(BucketHandler)
            }
        }
    }
}
