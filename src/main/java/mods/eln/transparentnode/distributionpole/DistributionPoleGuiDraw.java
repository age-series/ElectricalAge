package mods.eln.transparentnode.distributionpole;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;

public class DistributionPoleGuiDraw extends GuiContainerEln {

    public DistributionPoleGuiDraw(EntityPlayer player, IInventory inventory, DistributionPoleRender render) {
        super(new DistributionPoleContainer(player, inventory));
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, 176, 194 - 33 + 20, 8, 84 + 194 - 166 - 33 + 20, "transformer.png");
    }
}
