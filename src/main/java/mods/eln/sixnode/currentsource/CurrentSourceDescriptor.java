package mods.eln.sixnode.currentsource;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class CurrentSourceDescriptor extends SixNodeDescriptor {

    private Obj3DPart main;

    public CurrentSourceDescriptor(String name, Obj3D obj) {
        super(name, CurrentSourceElement.class, CurrentSourceRender.class);
        if (obj != null) {
            main = obj.getPart("main");
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Provides an ideal current source\nwithout energy or power limitation.").split("\\\n"));
        list.add("");
        list.add(tr("Internal resistance: %1$\u2126", Utils.plotValue(Eln.instance.lowVoltageCableDescriptor.electricalRs)));
        list.add("");
        list.add(tr("Creative block."));
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        switch (type) {
            case ENTITY:
                draw();
                break;

            case EQUIPPED:
            case EQUIPPED_FIRST_PERSON:
                GL11.glPushMatrix();
                GL11.glTranslatef(0.8f, 0.3f, 0.2f);
                GL11.glRotatef(150, 0, 0, 1);
                draw();
                GL11.glPopMatrix();
                break;

            case INVENTORY:
            case FIRST_PERSON_MAP:
                super.renderItem(type, item, data);
                break;
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player);
    }
}
