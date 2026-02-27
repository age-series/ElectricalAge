package mods.eln.integration.fmp;

import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.JCuboidPart;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.TMultiPart;
import mods.eln.partnode.PartNodeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;

import java.util.Collections;
import java.util.List;

public class PartNodeFmpPart extends JCuboidPart implements JNormalOcclusion {

    public static final String TYPE = "eln:part_node";
    private byte side = 1;

    public PartNodeFmpPart() {
    }

    public PartNodeFmpPart(byte side) {
        this.side = side;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Cuboid6 getBounds() {
        final double t = 0.25;
        switch (side) {
            case 0:
                return new Cuboid6(0, 0, 0, 1, t, 1);
            case 1:
                return new Cuboid6(0, 1 - t, 0, 1, 1, 1);
            case 2:
                return new Cuboid6(0, 0, 0, 1, 1, t);
            case 3:
                return new Cuboid6(0, 0, 1 - t, 1, 1, 1);
            case 4:
                return new Cuboid6(0, 0, 0, t, 1, 1);
            case 5:
            default:
                return new Cuboid6(1 - t, 0, 0, 1, 1, 1);
        }
    }

    @Override
    public Iterable<Cuboid6> getOcclusionBoxes() {
        return Collections.singletonList(getBounds());
    }

    @Override
    public boolean occlusionTest(TMultiPart npart) {
        return NormalOcclusionTest.apply(this, npart);
    }

    @Override
    public void save(NBTTagCompound tag) {
        super.save(tag);
        tag.setByte("side", side);
    }

    @Override
    public void load(NBTTagCompound tag) {
        super.load(tag);
        side = tag.getByte("side");
    }

    @Override
    public ItemStack pickItem(MovingObjectPosition hit) {
        if (PartNodeRegistry.INSTANCE.getPartNodeItem() == null) {
            return null;
        }
        return new ItemStack(PartNodeRegistry.INSTANCE.getPartNodeItem(), 1, 0);
    }

    @Override
    public Iterable<ItemStack> getDrops() {
        if (PartNodeRegistry.INSTANCE.getPartNodeItem() == null) {
            return Collections.emptyList();
        }
        final List<ItemStack> stack =
            Collections.singletonList(new ItemStack(PartNodeRegistry.INSTANCE.getPartNodeItem(), 1, 0));
        return stack;
    }
}
