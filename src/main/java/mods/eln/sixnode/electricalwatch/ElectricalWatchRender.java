package mods.eln.sixnode.electricalwatch;

import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public class ElectricalWatchRender extends SixNodeElementRender {

    ElectricalWatchDescriptor descriptor;

    boolean upToDate = false;
    long oldDate = 1379;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public ElectricalWatchRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (ElectricalWatchDescriptor) descriptor;
    }

    @Override
    public void draw() {
        super.draw();
        long time;
        if (upToDate)
            time = getTileEntity().getWorldObj().getWorldTime();
        else
            time = oldDate;
        time += 6000;
        time %= 24000;

        front.glRotateOnX();

        descriptor.draw(time / 12000f, (time % 1000) / 1000f, upToDate);
    }

    @Override
    public void publishUnserialize(DataInputStream stream) {
        super.publishUnserialize(stream);
        try {
            upToDate = stream.readBoolean();
            oldDate = stream.readLong();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {
        return new ElectricalWatchGui(player, inventory, this);
    }
}
