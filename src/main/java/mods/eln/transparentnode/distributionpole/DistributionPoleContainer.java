package mods.eln.transparentnode.distributionpole;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.ItemStackFilter;
import mods.eln.gui.SlotFilter;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class DistributionPoleContainer extends BasicContainer {
    static final int braceSlot = 0;
    static final int transformerSlot = 1;
    static final int breakerSlot = 2;
    static final int fuseSlot = 3;

    public DistributionPoleContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
            new SlotFilter(inventory, braceSlot, 10, 10, 1,
                new ItemStackFilter[]{new ItemStackFilter(Blocks.wooden_slab)},
                SlotSkin.medium,
                new String[]{tr("Crossbar Slot"), tr("(Adds capacity for high voltage lines)")}),
            new SlotFilter(inventory, transformerSlot, 30, 10, 1,
                new ItemStackFilter[]{new ItemStackFilter(Items.iron_ingot)},
                SlotSkin.medium,
                new String[]{tr("Transformer Slot"), tr("Adds a transformer to the pole")}),
            new SlotFilter(inventory, breakerSlot, 50, 10, 1,
                new ItemStackFilter[]{new ItemStackFilter(Items.item_frame)},
                SlotSkin.medium,
                new String[]{tr("Breaker Slot"), tr("Adds a breaker to your pole!")}),
            new SlotFilter(inventory, fuseSlot, 70, 10, 1,
                new ItemStackFilter[]{new ItemStackFilter(Items.gold_ingot)},
                SlotSkin.medium,
                new String[]{tr("Fuse Slot"), tr("Adds a fuse to the breaker")})
        });
    }
}
