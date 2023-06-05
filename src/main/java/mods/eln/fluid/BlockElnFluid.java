package mods.eln.fluid;
//shamelessly lifted from IC2

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

import mods.eln.Eln;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraft.item.ItemBlock;

public class BlockElnFluid extends BlockFluidClassic
{
    @SideOnly(Side.CLIENT)
    protected IIcon[] fluidIcon;
    protected Fluid fluid;
    private final int color;

    public BlockElnFluid(String internalName, Fluid fluid, Material material, int color)
    {
        super(fluid, material);

        setBlockName(fluidName);
        GameRegistry.registerBlock(this, ItemBlock.class, internalName);
        this.fluid = fluid;
        this.color = color;

        if (this.density <= FluidRegistry.WATER.getDensity()) {
            this.displacements.put(Blocks.water, Boolean.valueOf(false));
            this.displacements.put(Blocks.flowing_water, Boolean.valueOf(false));
        }

        if (this.density <= FluidRegistry.LAVA.getDensity()) {
            this.displacements.put(Blocks.lava, Boolean.valueOf(false));
            this.displacements.put(Blocks.flowing_lava, Boolean.valueOf(false));
        }


    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister)
    {
        String name = this.fluidName; if(name.startsWith("eln")){name = name.substring("eln".length());}
        String domain = Eln.MODID.toLowerCase(Locale.ENGLISH) + ":fluids/";
        this.fluidIcon = new IIcon[] {
            iconRegister.registerIcon(domain + name + "_still"),
            iconRegister.registerIcon(domain + name + "_flow") };
    }

    /*public void updateTick(World world, int x, int y, int z, Random random)
    {
        super.updateTick(world, x, y, z, random);

            if (this.fluid.equals(BlocksItems.getFluid(InternalName.fluidHotWater)))
                if ((isSourceBlock(world, x, y, z)) && (world.getBlock(x, y - 2, z) != Blocks.flowing_lava) && (world.getBlock(x, y - 1, z) != this) && (world.rand.nextInt(60) == 0)) {
                    world.setBlock(x, y, z, Blocks.flowing_water, 0, 3);
                }
                else
                    world.scheduleBlockUpdate(x, y, z, this, tickRate(world));
    }

    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        super.onNeighborBlockChange(world, x, y, z, block);

        hardenFromNeighbors(world, x, y, z);
    }

    public void onBlockAdded(World world, int x, int y, int z)
    {
        super.onBlockAdded(world, x, y, z);

        hardenFromNeighbors(world, x, y, z);
    }

    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityliving, ItemStack itemStack)
    {
        /*if (!IC2.platform.isSimulating()) return;

        if (this.fluid.equals(BlocksItems.getFluid(InternalName.fluidBiogas)))
            world.setBlock(x, y, z, Blocks.air, 0, 7);
    }

    public void onEntityCollidedWithBlock(World world, int i, int j, int k, Entity entity)
    {
        /*
        if ((entity instanceof EntityPlayer))
        {
            if ((this.fluid.equals(BlocksItems.getFluid(InternalName.fluidConstructionFoam))) &&
                (!((EntityPlayer)entity).isPotionActive(Potion.moveSlowdown.id))) {
                ((EntityPlayer)entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 300, 2, true));
            }}
        }

        if (((entity instanceof EntityLivingBase)) &&
            (this.fluid.equals(BlocksItems.getFluid(InternalName.fluidHotWater))))
        {
            int id;
            if (((EntityLivingBase)entity).isEntityUndead()) {
                id = Potion.wither.id;
            }
            else {
                id = Potion.regeneration.id;
            }
            if (!((EntityLivingBase)entity).isPotionActive(id))
                ((EntityLivingBase)entity).addPotionEffect(new PotionEffect(id, 100, IC2.random.nextInt(2), true));
        }
    }*/

    public IIcon getIcon(int side, int meta)
    {
        return (side != 0) && (side != 1) ? this.fluidIcon[1] : this.fluidIcon[0];
    }

    //public String getUnlocalizedName()
    //{
    //    return super.getUnlocalizedName().substring(5);
    //}

    public int getColor() {
        return this.color;
    }

    /*private boolean hardenFromNeighbors(World world, int x, int y, int z) {
        if (!IC2.platform.isSimulating()) return false;

        if (this.fluid.equals(BlocksItems.getFluid(InternalName.fluidPahoehoeLava))) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                LiquidUtil.LiquidData data = LiquidUtil.getLiquid(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);

                if ((data != null) &&
                    (data.liquid
                        .getTemperature() <= this.fluid.getTemperature() / 4)) {
                    if (isSourceBlock(world, x, y, z))
                        world.setBlock(x, y, z, StackUtil.getBlock(Ic2Items.basaltBlock));
                    else {
                        world.setBlockToAir(x, y, z);
                    }

                    return true;
                }
            }
        }

        return false;
    }*/
}
