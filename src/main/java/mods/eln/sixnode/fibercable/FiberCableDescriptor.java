package mods.eln.sixnode.fibercable;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class FiberCableDescriptor extends SixNodeDescriptor {

    protected CableRenderDescriptor render;

    public FiberCableDescriptor(String name, CableRenderDescriptor render) {
        super(name, FiberCableElement.class, FiberCableRender.class);
        this.render = render;
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("I'm a fiber optic cable!"));
    }
}
