package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart main;

    int range;

    public WirelessSignalTxDescriptor(String name,
                                      Obj3D obj,
                                      int range) {
        super(name, WirelessSignalTxElement.class, WirelessSignalTxRender.class);
        this.range = range;
        this.obj = obj;
        if (obj != null) main = obj.getPart("main");

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    public void draw() {
        if (main != null) main.draw();
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            if (type == ItemRenderType.ENTITY) {
                //	GL11.glTranslatef(1.0f, 0f, 0f);
                GL11.glScalef(2.8f, 2.8f, 2.8f);
            }
            draw();
        }
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Sends signal voltage on selected wireless signal channel"));
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        list.add(tr("It should require power to transmit realistically"));
        return RealisticEnum.IDEAL;
    }
}
