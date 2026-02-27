package mods.eln.integration.fmp;

import codechicken.lib.vec.BlockCoord;
import codechicken.lib.vec.Vector3;
import codechicken.multipart.JItemMultiPart;
import codechicken.multipart.TMultiPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PartNodeMultipartItem extends JItemMultiPart {

    @Override
    public TMultiPart newPart(ItemStack item, EntityPlayer player, World world, BlockCoord pos, int side, Vector3 hit) {
        if (side < 0 || side > 5) {
            return null;
        }
        return new PartNodeFmpPart((byte) side);
    }
}
