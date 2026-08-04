package mods.eln.integration.fmp;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.JItemMultiPart;
import codechicken.multipart.TMultiPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PartNodeMultipartItem extends JItemMultiPart {

    private final String partType;

    public PartNodeMultipartItem() {
        this(PartNodeFmpPart.TYPE);
    }

    public PartNodeMultipartItem(String partType) {
        this.partType = partType;
    }

    @Override
    public TMultiPart newPart(ItemStack item, EntityPlayer player, World world, BlockCoord pos, int side, Vector3 hit) {
        if (side < 0 || side > 5) {
            return null;
        }
        if (PartNodeFmpPart.CREATIVE_RESISTOR_TYPE.equals(partType)) {
            final byte rotation = (byte) (((int) Math.floor(player.rotationYaw * 4.0f / 360.0f + 0.5) + 1) & 3);
            // FMP supplies top/bottom as the occupied face, but side faces as
            // the face of the block that was clicked.
            final byte mountSide = (byte) (side < 2 ? side : side ^ 1);
            return new CreativeResistorFmpPart(mountSide, mountSide < 2 ? rotation : 0);
        }
        return new PartNodeFmpPart((byte) side);
    }
}
