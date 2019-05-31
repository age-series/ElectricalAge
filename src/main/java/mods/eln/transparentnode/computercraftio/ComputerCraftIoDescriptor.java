package mods.eln.transparentnode.computercraftio;

import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.item.ItemStack;

public class ComputerCraftIoDescriptor extends TransparentNodeDescriptor {

    public ComputerCraftIoDescriptor(String name, Obj3D obj) {
        super(name, ComputerCraftIoElement.class, ComputerCraftIoRender.class);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        draw(0, 1f);
    }

    void draw(int eggStackSize, float powerFactor) {
    }
}
