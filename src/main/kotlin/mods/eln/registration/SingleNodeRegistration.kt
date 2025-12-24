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
import mods.eln.railroad.ThirdRailBlock
import mods.eln.railroad.ThirdRailItem
import mods.eln.railroad.ThirdRailNode
import mods.eln.railroad.ThirdRailTileEntity
import net.minecraft.tileentity.TileEntity

object SingleNodeRegistration {

    fun registerSingle() {
        if (Eln.instance.isDevelopmentRun) {
            registerConduitSingles()
        }
        registerEnergyConverter()
        registerComputer()
        registerThirdRail()
    }


    private fun registerConduitSingles() {
        run {
            val entityName = I18N.TR_NAME(I18N.Type.TILE, "eln.Conduit")
            TileEntity.addMapping(ConduitEntity::class.java, entityName)
            registerUuid(getNodeUuidStatic(), ConduitNode::class.java)


            val conduitBlock = ConduitBlock()
            conduitBlock.setCreativeTab(null).setBlockName(entityName)
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
                Eln.instance.elnToOtherBlockConverter.setCreativeTab(Eln.creativeTabPowerElectronics).setBlockName(blockName)
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
            Eln.instance.computerProbeBlock.setCreativeTab(Eln.creativeTabSignalProcessing).setBlockName(entityName)
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

    private fun registerThirdRail() {
        val entityName = I18N.TR_NAME(I18N.Type.TILE, "eln.thirdRail")
        TileEntity.addMapping(ThirdRailTileEntity::class.java, "eln.ThirdRail")
        registerUuid(ThirdRailNode.NODE_UUID, ThirdRailNode::class.java)

        Eln.instance.thirdRailBlock = ThirdRailBlock()
        Eln.instance.thirdRailBlock
            .setCreativeTab(Eln.creativeTabPowerElectronics)
            .setBlockName(entityName)
        GameRegistry.registerBlock(Eln.instance.thirdRailBlock, ThirdRailItem::class.java, entityName)
    }
}
