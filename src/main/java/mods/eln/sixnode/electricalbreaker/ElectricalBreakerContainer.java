package mods.eln.sixnode.electricalbreaker;

import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class ElectricalBreakerContainer extends BasicContainer {

    public ElectricalBreakerContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new net.minecraft.inventory.Slot[]{});
    }
}
