package mods.eln.registration;

import cpw.mods.fml.common.registry.GameRegistry;
import mods.eln.Vars;
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
import net.minecraft.tileentity.TileEntity;

import static mods.eln.i18n.I18N.TR_NAME;

public class BlockRegistration {

    public BlockRegistration() {

    }

    public void registerBlocks() {
        registerEnergyConverter();
        registerComputer();
        registerOre();
    }

    private void registerOre() {
        int id;
        String name;

        {
            id = 1;

            name = TR_NAME(I18N.Type.NONE, "Copper Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                30 * (Vars.genCopper ? 1 : 0), 6, 10, 0, 80 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            Vars.oreCopper = desc;
            Vars.oreItem.addDescriptor(id, desc);
            Vars.addToOre("Vars.oreCopper", desc.newItemStack());
        }

        {
            id = 4;

            name = TR_NAME(I18N.Type.NONE, "Lead Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                8 * (Vars.genLead ? 1 : 0), 3, 9, 0, 24 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            Vars.oreItem.addDescriptor(id, desc);
            Vars.addToOre("oreLead", desc.newItemStack());
        }
        {
            id = 5;

            name = TR_NAME(I18N.Type.NONE, "Tungsten Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                6 * (Vars.genTungsten ? 1 : 0), 3, 9, 0, 32 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            Vars.oreItem.addDescriptor(id, desc);
            Vars.addToOre(Vars.dictTungstenOre, desc.newItemStack());
        }
        {
            id = 6;

            name = TR_NAME(I18N.Type.NONE, "Cinnabar Ore");

            OreDescriptor desc = new OreDescriptor(name, id, // int itemIconId,
                // String
                // name,int
                // metadata,
                3 * (Vars.genCinnabar ? 1 : 0), 3, 9, 0, 32 // int spawnRate,int
                // spawnSizeMin,int
                // spawnSizeMax,int spawnHeightMin,int
                // spawnHeightMax
            );
            Vars.oreItem.addDescriptor(id, desc);
            Vars.addToOre("oreCinnabar", desc.newItemStack());
        }

    }

    private void registerComputer() {
        if (Vars.ComputerProbeEnable) {
            String entityName = TR_NAME(I18N.Type.TILE, "eln.ElnProbe");

            TileEntity.addMapping(ComputerProbeEntity.class, entityName);
            NodeManager.registerUuid(ComputerProbeNode.getNodeUuidStatic(), ComputerProbeNode.class);


            Vars.computerProbeBlock = new ComputerProbeBlock();
            Vars.computerProbeBlock.setCreativeTab(Vars.creativeTab).setBlockName(entityName);
            GameRegistry.registerBlock(Vars.computerProbeBlock, SimpleNodeItem.class, entityName);
        }

    }

    private void registerEnergyConverter() {
        if (Vars.ElnToOtherEnergyConverterEnable) {
            String entityName = "eln.EnergyConverterElnToOtherEntity";

            TileEntity.addMapping(EnergyConverterElnToOtherEntity.class, entityName);
            NodeManager.registerUuid(EnergyConverterElnToOtherNode.getNodeUuidStatic(), EnergyConverterElnToOtherNode.class);

            {
                String blockName = TR_NAME(I18N.Type.TILE, "eln.EnergyConverterElnToOtherLVUBlock");
                EnergyConverterElnToOtherDescriptor.ElnDescriptor elnDesc = new EnergyConverterElnToOtherDescriptor.ElnDescriptor(Vars.LVU, Vars.LVP);
                EnergyConverterElnToOtherDescriptor.Ic2Descriptor ic2Desc = new EnergyConverterElnToOtherDescriptor.Ic2Descriptor(32, 1);
                EnergyConverterElnToOtherDescriptor.OcDescriptor ocDesc = new EnergyConverterElnToOtherDescriptor.OcDescriptor(ic2Desc.outMax * Vars.getElnToOcConversionRatio() / Vars.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherLVU", elnDesc, ic2Desc, ocDesc);
                Vars.elnToOtherBlockLvu = new EnergyConverterElnToOtherBlock(desc);
                Vars.elnToOtherBlockLvu.setCreativeTab(Vars.creativeTab).setBlockName(blockName);
                GameRegistry.registerBlock(Vars.elnToOtherBlockLvu, SimpleNodeItem.class, blockName);
            }
            {
                String blockName = TR_NAME(I18N.Type.TILE, "eln.EnergyConverterElnToOtherMVUBlock");
                EnergyConverterElnToOtherDescriptor.ElnDescriptor elnDesc = new EnergyConverterElnToOtherDescriptor.ElnDescriptor(Vars.MVU, Vars.MVP);
                EnergyConverterElnToOtherDescriptor.Ic2Descriptor ic2Desc = new EnergyConverterElnToOtherDescriptor.Ic2Descriptor(128, 2);
                EnergyConverterElnToOtherDescriptor.OcDescriptor ocDesc = new EnergyConverterElnToOtherDescriptor.OcDescriptor(ic2Desc.outMax * Vars.getElnToOcConversionRatio() / Vars.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherMVU", elnDesc, ic2Desc, ocDesc);
                Vars.elnToOtherBlockMvu = new EnergyConverterElnToOtherBlock(desc);
                Vars.elnToOtherBlockMvu.setCreativeTab(Vars.creativeTab).setBlockName(blockName);
                GameRegistry.registerBlock(Vars.elnToOtherBlockMvu, SimpleNodeItem.class, blockName);
            }
            {
                String blockName = TR_NAME(I18N.Type.TILE, "eln.EnergyConverterElnToOtherHVUBlock");
                EnergyConverterElnToOtherDescriptor.ElnDescriptor elnDesc = new EnergyConverterElnToOtherDescriptor.ElnDescriptor(Vars.HVU, Vars.HVP);
                EnergyConverterElnToOtherDescriptor.Ic2Descriptor ic2Desc = new EnergyConverterElnToOtherDescriptor.Ic2Descriptor(512, 3);
                EnergyConverterElnToOtherDescriptor.OcDescriptor ocDesc = new EnergyConverterElnToOtherDescriptor.OcDescriptor(ic2Desc.outMax * Vars.getElnToOcConversionRatio() / Vars.getElnToIc2ConversionRatio());
                EnergyConverterElnToOtherDescriptor desc =
                    new EnergyConverterElnToOtherDescriptor("EnergyConverterElnToOtherHVU", elnDesc, ic2Desc, ocDesc);
                Vars.elnToOtherBlockHvu = new EnergyConverterElnToOtherBlock(desc);
                Vars.elnToOtherBlockHvu.setCreativeTab(Vars.creativeTab).setBlockName(blockName);
                GameRegistry.registerBlock(Vars.elnToOtherBlockHvu, SimpleNodeItem.class, blockName);
            }
        }
    }
}
