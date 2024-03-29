package mods.eln.sixnode.groundcable;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class GroundCableDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main;

    public GroundCableDescriptor(String name, Obj3D obj) {
        super(name, GroundCableElement.class, GroundCableRender.class);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Provides a zero volt reference."));
        Collections.addAll(list, tr("Can be used to set a point of an\nelectrical network to 0V potential.\nFor example to ground negative battery contacts.").split("\n"));
        list.add(tr("Internal resistance: %1$\u2126", Utils.plotValue(Eln.getSmallRs())));
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        list.add(tr("Acts as a ground reference."));
        list.add(tr("Has a small resistance inline"));
        return RealisticEnum.REALISTIC;
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull EntityPlayer player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
