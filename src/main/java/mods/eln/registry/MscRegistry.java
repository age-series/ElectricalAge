package mods.eln.registry;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Eln;
import mods.eln.Other;
import mods.eln.debug.DebugType;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.i18n.I18N;
import mods.eln.node.NodeManager;
import mods.eln.node.simple.SimpleNodeItem;
import mods.eln.ore.OreDescriptor;
import mods.eln.simplenode.computerprobe.ComputerProbeBlock;
import mods.eln.simplenode.computerprobe.ComputerProbeEntity;
import mods.eln.simplenode.computerprobe.ComputerProbeNode;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import static mods.eln.i18n.I18N.TR_NAME;
import static mods.eln.registry.RegistryUtils.findItemStack;

public class MscRegistry {

    public static void thingRegistration() {
        registerEnergyConverter();
        registerComputer();
        registerOre();
    }

    private static void registerOre() {
        int id;
        String name;
        {
            id = 1;
            name = TR_NAME(I18N.Type.NONE, "Copper Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                30 * (Eln.genCopper ? 1 : 0), 6, 10, 0, 80
            );
            Eln.oreCopper = desc;
            Eln.oreItem.addDescriptor(id, desc);
            RegistryUtils.addToOre("oreCopper", desc.newItemStack());
        }
        {
            id = 4;
            name = TR_NAME(I18N.Type.NONE, "Lead Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                8 * (Eln.genLead ? 1 : 0), 3, 9, 0, 24
            );
            Eln.oreItem.addDescriptor(id, desc);
            RegistryUtils.addToOre("oreLead", desc.newItemStack());
        }
        {
            id = 5;
            name = TR_NAME(I18N.Type.NONE, "Tungsten Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                6 * (Eln.genTungsten ? 1 : 0), 3, 9, 0, 32
            );
            Eln.oreItem.addDescriptor(id, desc);
            RegistryUtils.addToOre(Eln.dictTungstenOre, desc.newItemStack());
        }
        {
            id = 6;
            name = TR_NAME(I18N.Type.NONE, "Cinnabar Ore");
            OreDescriptor desc = new OreDescriptor(name, id,
                3 * (Eln.genCinnabar ? 1 : 0), 3, 9, 0, 32
            );
            Eln.oreItem.addDescriptor(id, desc);
            RegistryUtils.addToOre("oreCinnabar", desc.newItemStack());
        }
    }

    private static void registerEnergyConverter() {
        if (Eln.ElnToOtherEnergyConverterEnable) {
            String entityName = "eln.EnergyConverterElnToOtherEntity";

            TileEntity.addMapping(EnergyConverterElnToOtherEntity.class, entityName);
            NodeManager.registerUuid(EnergyConverterElnToOtherNode.getNodeUuidStatic(), EnergyConverterElnToOtherNode.class);

            {
                String blockName = TR_NAME(I18N.Type.TILE, "eln.EnergyConverterElnToOtherLVUBlock");
                EnergyConverterElnToOtherDescriptor.ElnDescriptor elnDesc = new EnergyConverterElnToOtherDescriptor.ElnDescriptor(Eln.LVU, Eln.LVP());
                EnergyConverterElnToOtherDescriptor.Ic2Descriptor ic2Desc = new EnergyConverterElnToOtherDescriptor.Ic2Descriptor(32, 1);
                EnergyConverterElnToOtherDescriptor.OcDescriptor ocDesc = new EnergyConverterElnToOtherDescriptor.OcDescriptor(ic2Desc.outMax * Other.getElnToOcConversionRatio() / Other.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherLVU", elnDesc, ic2Desc, ocDesc);
                Eln.elnToOtherBlockLvu = new EnergyConverterElnToOtherBlock(desc);
                Eln.elnToOtherBlockLvu.setCreativeTab(Eln.creativeTab).setBlockName(blockName);
                GameRegistry.registerBlock(Eln.elnToOtherBlockLvu, SimpleNodeItem.class, blockName);
            }
            {
                String blockName = TR_NAME(I18N.Type.TILE, "eln.EnergyConverterElnToOtherMVUBlock");
                EnergyConverterElnToOtherDescriptor.ElnDescriptor elnDesc = new EnergyConverterElnToOtherDescriptor.ElnDescriptor(Eln.MVU, Eln.MVP());
                EnergyConverterElnToOtherDescriptor.Ic2Descriptor ic2Desc = new EnergyConverterElnToOtherDescriptor.Ic2Descriptor(128, 2);
                EnergyConverterElnToOtherDescriptor.OcDescriptor ocDesc = new EnergyConverterElnToOtherDescriptor.OcDescriptor(ic2Desc.outMax * Other.getElnToOcConversionRatio() / Other.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherMVU", elnDesc, ic2Desc, ocDesc);
                Eln.elnToOtherBlockMvu = new EnergyConverterElnToOtherBlock(desc);
                Eln.elnToOtherBlockMvu.setCreativeTab(Eln.creativeTab).setBlockName(blockName);
                GameRegistry.registerBlock(Eln.elnToOtherBlockMvu, SimpleNodeItem.class, blockName);
            }
            {
                String blockName = TR_NAME(I18N.Type.TILE, "eln.EnergyConverterElnToOtherHVUBlock");
                EnergyConverterElnToOtherDescriptor.ElnDescriptor elnDesc = new EnergyConverterElnToOtherDescriptor.ElnDescriptor(Eln.HVU, Eln.HVP());
                EnergyConverterElnToOtherDescriptor.Ic2Descriptor ic2Desc = new EnergyConverterElnToOtherDescriptor.Ic2Descriptor(512, 3);
                EnergyConverterElnToOtherDescriptor.OcDescriptor ocDesc = new EnergyConverterElnToOtherDescriptor.OcDescriptor(ic2Desc.outMax * Other.getElnToOcConversionRatio() / Other.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherHVU", elnDesc, ic2Desc, ocDesc);
                Eln.elnToOtherBlockHvu = new EnergyConverterElnToOtherBlock(desc);
                Eln.elnToOtherBlockHvu.setCreativeTab(Eln.creativeTab).setBlockName(blockName);
                GameRegistry.registerBlock(Eln.elnToOtherBlockHvu, SimpleNodeItem.class, blockName);
            }
        }
    }

    private static void registerComputer() {
        if (Eln.ComputerProbeEnable) {
            String entityName = TR_NAME(I18N.Type.TILE, "eln.ElnProbe");

            TileEntity.addMapping(ComputerProbeEntity.class, entityName);
            NodeManager.registerUuid(ComputerProbeNode.getNodeUuidStatic(), ComputerProbeNode.class);


            Eln.computerProbeBlock = new ComputerProbeBlock();
            Eln.computerProbeBlock.setCreativeTab(Eln.creativeTab).setBlockName(entityName);
            GameRegistry.registerBlock(Eln.computerProbeBlock, SimpleNodeItem.class, entityName);
        }

    }

    // TODO: This may need to be moved to load(), since it was originally there. Not sure if it's important that it's there or not. :/
    private void registerReplicator() {
        int redColor = (255 << 16);
        int orangeColor = (255 << 16) + (200 << 8);
        if (Eln.replicatorRegistrationId == -1)
            Eln.replicatorRegistrationId = EntityRegistry.findGlobalUniqueEntityId();
        Eln.dp.println(DebugType.OTHER, "Replicator registred at" + Eln.replicatorRegistrationId);
        // Register mob
        EntityRegistry.registerGlobalEntityID(ReplicatorEntity.class, TR_NAME(I18N.Type.ENTITY, "EAReplicator"), Eln.replicatorRegistrationId, redColor, orangeColor);
        ReplicatorEntity.dropList.add(findItemStack("Iron Dust", 1));
        ReplicatorEntity.dropList.add(findItemStack("Copper Dust", 1));
        ReplicatorEntity.dropList.add(findItemStack("Gold Dust", 1));
        ReplicatorEntity.dropList.add(new ItemStack(Items.redstone));
        ReplicatorEntity.dropList.add(new ItemStack(Items.glowstone_dust));
        // Add mob spawn
        // EntityRegistry.addSpawn(ReplicatorEntity.class, 1, 1, 2, EnumCreatureType.monster, BiomeGenBase.plains);
    }
}
