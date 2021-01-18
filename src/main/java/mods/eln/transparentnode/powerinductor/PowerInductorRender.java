package mods.eln.transparentnode.powerinductor;

import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;

public class PowerInductorRender extends TransparentNodeElementRender {

    public PowerInductorDescriptor descriptor;
    private CableRenderType renderPreProcess;

    public PowerInductorRender(TransparentNodeEntity tileEntity,
                               TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (PowerInductorDescriptor) descriptor;

    }


    @Override
    public void draw() {


        descriptor.draw();

    }

    @Override
    public void refresh(float deltaT) {

    }


    @Override
    public void networkUnserialize(DataInputStream stream) {

        super.networkUnserialize(stream);


	/*	try {


		} catch (IOException e) {
			
			e.printStackTrace();
		}*/

    }

    TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);

    @Nullable
    @Override
    public GuiScreen newGuiDraw(@NotNull Direction side, @NotNull EntityPlayer player) {

        return new PowerInductorGui(player, inventory, this);
    }


}
