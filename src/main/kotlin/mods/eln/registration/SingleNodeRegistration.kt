package mods.eln.registration

import cpw.mods.fml.common.registry.GameRegistry
import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.node.NodeManager.Companion.registerUuid
import mods.eln.node.simple.SimpleNodeItem
import mods.eln.simplenode.ConduitBlock
import mods.eln.simplenode.ConduitEntity
import mods.eln.simplenode.ConduitNode
import mods.eln.simplenode.ConduitNode.Companion.getNodeUuidStatic
import mods.eln.simplenode.computerprobe.ComputerProbeBlock
import mods.eln.simplenode.computerprobe.ComputerProbeEntity
import mods.eln.simplenode.computerprobe.ComputerProbeNode
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherDescriptor
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode.Companion.nodeUuidStatic
import net.minecraft.tileentity.TileEntity

object SingleNodeRegistration {

    fun registerSingle() {
        if (Eln.instance.isDevelopmentRun) {
            registerConduitSingles()
        }
        registerEnergyConverter()
        registerComputer()
    }


    private fun registerConduitSingles() {
        run {
            val entityName = I18N.TR_NAME(I18N.Type.TILE, "eln.Conduit")
            TileEntity.addMapping(ConduitEntity::class.java, entityName)
            registerUuid(getNodeUuidStatic(), ConduitNode::class.java)


            val conduitBlock = ConduitBlock()
            conduitBlock.setCreativeTab(Eln.creativeTab).setBlockName(entityName)
            GameRegistry.registerBlock(conduitBlock, SimpleNodeItem::class.java, entityName)
        }
    }

    private fun registerEnergyConverter() {
        if (Eln.instance.ElnToOtherEnergyConverterEnable) {
            val entityName = "eln.EnergyConverterElnToOtherEntity"

            TileEntity.addMapping(EnergyConverterElnToOtherEntity::class.java, entityName)
            registerUuid(
                nodeUuidStatic,
                EnergyConverterElnToOtherNode::class.java
            )

            run {
                val blockName =
                    I18N.TR_NAME(I18N.Type.TILE, "eln.EnergyConverter")
                val desc = EnergyConverterElnToOtherDescriptor(
                    "EnergyConverterElnToOtherLVU", Eln.instance.ELN_CONVERTER_MAX_POWER
                )
                Eln.instance.elnToOtherBlockConverter = EnergyConverterElnToOtherBlock(desc)
                Eln.instance.elnToOtherBlockConverter.setCreativeTab(Eln.creativeTab).setBlockName(blockName)
                GameRegistry.registerBlock(Eln.instance.elnToOtherBlockConverter, SimpleNodeItem::class.java, blockName)
            }
        }
    }

    private fun registerComputer() {
        if (Eln.instance.ComputerProbeEnable) {
            val entityName = I18N.TR_NAME(I18N.Type.TILE, "eln.ElnProbe")

            TileEntity.addMapping(ComputerProbeEntity::class.java, entityName)
            registerUuid(ComputerProbeNode.getNodeUuidStatic(), ComputerProbeNode::class.java)


            Eln.instance.computerProbeBlock = ComputerProbeBlock()
            Eln.instance.computerProbeBlock.setCreativeTab(Eln.creativeTab).setBlockName(entityName)
            GameRegistry.registerBlock(Eln.instance.computerProbeBlock, SimpleNodeItem::class.java, entityName)
        }
        /*
        if (ComputerProbeEnable) {
            String name = TR_NAME(Type.TILE, "eln.ElnDeviceProbe");
            TileEntity.addMapping(DeviceProbeEntity.class, name);
            NodeManager.registerUuid(DeviceProbeNode.Companion.getNodeUuidStatic(), DeviceProbeNode.class);
            DeviceProbeBlock deviceProbeBlock = new DeviceProbeBlock();
            deviceProbeBlock.setCreativeTab(creativeTab).setBlockName(name);
            GameRegistry.registerBlock(deviceProbeBlock, SimpleNodeItem.class, name);
        }
        */
    }

}