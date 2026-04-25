package mods.eln.sixnode.lampsupply;

import mods.eln.cable.CableItemSlot;
import mods.eln.i18n.I18N;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class LampSupplyContainer extends BasicContainer {

    public static final int cableSlotId = 0;

    public LampSupplyContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{new CableItemSlot(inventory, cableSlotId, 184, 144, 64, false, I18N.tr(
                "Cable slot\nBase range is 32 blocks.\nEach additional cable\nincreases range by one.").split("\n"))});
    }
}
