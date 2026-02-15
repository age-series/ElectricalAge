package mods.eln.partnode

import cpw.mods.fml.common.registry.GameRegistry
import mods.eln.integration.fmp.PartNodeMultipartItem
import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.node.NodeManager.Companion.registerUuid
import mods.eln.node.simple.SimpleNodeItem
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntity

object PartNodeRegistry {
    var partNodeBlock: TestPartNodeBlock? = null
    var partNodeItem: Item? = null

    @JvmField
    var descriptor: TestPartNodeDescriptor? = null

    fun registerPartNodes() {
        if (!Eln.instance.isDevelopmentRun) {
            return
        }

        val entityName = I18N.TR_NAME(I18N.Type.TILE, "eln.PartNodeEntity")
        val blockName = I18N.TR_NAME(I18N.Type.TILE, "eln.PartNode")
        val partItemName = I18N.TR_NAME(I18N.Type.ITEM, "eln.PartNodeMultipartItem")
        descriptor = TestPartNodeDescriptor()
        TileEntity.addMapping(TestPartNodeEntity::class.java, entityName)
        registerUuid(TestPartNode.nodeUuidStatic, TestPartNode::class.java)

        val block = TestPartNodeBlock(descriptor!!)
        block.setCreativeTab(Eln.creativeTabCreative)
            .setBlockName(blockName)
            .setBlockTextureName("eln:conduit")
        GameRegistry.registerBlock(block, SimpleNodeItem::class.java, blockName)
        partNodeBlock = block

        val item = PartNodeMultipartItem()
        item.setUnlocalizedName(partItemName)
        item.setTextureName("eln:conduit")
        item.setCreativeTab(Eln.creativeTabCreative)
        GameRegistry.registerItem(item, partItemName)
        partNodeItem = item
    }
}
