package mods.eln.integration.fmp;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TMultiPart;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.partnode.PartNodeRegistry;
import mods.eln.partnode.TestPartNode;
import mods.eln.partnode.TestPartNodeEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PartNodeFmpFactory implements MultiPartRegistry.IPartFactory, MultiPartRegistry.IPartConverter {

    @Override
    public TMultiPart createPart(String name, boolean client) {
        if (PartNodeFmpPart.TYPE.equals(name)) {
            return new PartNodeFmpPart();
        }
        return null;
    }

    @Override
    public Iterable<Block> blockTypes() {
        final Block block = PartNodeRegistry.INSTANCE.getPartNodeBlock();
        if (block == null) {
            return Collections.emptyList();
        }
        final List<Block> blocks = new ArrayList<Block>(1);
        blocks.add(block);
        return blocks;
    }

    @Override
    public TMultiPart convert(World world, BlockCoord pos) {
        final TileEntity tileEntity = world.getTileEntity(pos.x, pos.y, pos.z);
        if (!(tileEntity instanceof TestPartNodeEntity)) {
            return null;
        }

        final SimpleNode nodeBase = ((TestPartNodeEntity) tileEntity).getNode();
        if (!(nodeBase instanceof TestPartNode)) {
            return null;
        }
        final TestPartNode node = (TestPartNode) nodeBase;

        final Direction partSide = node.getFront() == null ? Direction.YP : node.getFront();
        return new PartNodeFmpPart((byte) partSide.toSideValue());
    }
}
