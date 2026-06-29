package mods.eln.transparentnode.distributionpole;

import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import java.io.DataInputStream;
import java.io.IOException;

public class DistributionPoleRender extends TransparentNodeElementRender {
    private final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(4, 64, this);
    private final RcInterpolator interpolator;

    private DistributionPoleDescriptor descriptor;

    private boolean hasCrossbar, hasTransformer, hasFuseHolder, hasFuse, fuseEngaged;

    public DistributionPoleRender(TransparentNodeEntity entity, TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
        this.descriptor = (DistributionPoleDescriptor) descriptor;
        interpolator = new RcInterpolator(1f);
    }

    @Override
    public void draw() {
        descriptor.draw(this.front, hasCrossbar, hasTransformer, hasFuseHolder, hasFuse, interpolator.get());
    }

    @Override
    public boolean cameraDrawOptimisation() {
        return false;
    }

    @Override
    public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
        return new DistributionPoleGuiDraw(player, inventory, this);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            hasCrossbar = stream.readBoolean();
            hasTransformer = stream.readBoolean();
            hasFuseHolder = stream.readBoolean();
            hasFuse = stream.readBoolean();
            fuseEngaged = stream.readBoolean();
        } catch (IOException e) {

        }
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        if (hasFuse) {
            if (fuseEngaged) {
                interpolator.setTarget(1f);
            } else {
                interpolator.setTarget(0f);
            }
            interpolator.step(deltaT);
            if (interpolator.get() < 0.1f) {
                fuseEngaged = true;
            }
            if (interpolator.get() > 0.9f) {
                fuseEngaged = false;
            }
        }
    }
}
