package mods.eln.sixnode.electricalbreaker;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalBreakerDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart lever;
    private Obj3DPart led;

    float alphaOff, alphaOn, speed;
    public double currentLimit;
    public float[] pinDistance;

    public ElectricalBreakerDescriptor(String name, Obj3D obj) {
        this(name, obj, Double.POSITIVE_INFINITY);
    }

    public ElectricalBreakerDescriptor(String name, Obj3D obj, double currentLimit) {
        super(name, ElectricalBreakerElement.class, ElectricalBreakerRender.class);
        this.obj = obj;
        this.currentLimit = currentLimit;
        if (obj != null) {
            main = obj.getPart("case");
            lever = obj.getPart("lever");

            if (lever != null) {
                speed = lever.getFloat("speed");
                alphaOff = lever.getFloat("alphaOff");
                alphaOn = lever.getFloat("alphaOn");
            }
            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) /*GL11.glScalef(1.8f, 1.8f, 1.8f);*/ {
            super.renderItem(type, item, data);
            renderCurrentLimitOverlay();
        } else
            draw(0f, 0f);
    }

    private void renderCurrentLimitOverlay() {
        if (!Double.isFinite(currentLimit)) return;

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        String overlay = currentLimit == Math.rint(currentLimit)
            ? Integer.toString((int) currentLimit)
            : Double.toString(currentLimit);
        float scale = 0.5f;
        float scaledWidth = font.getStringWidth(overlay) * scale;
        int x = (int) ((16f - scaledWidth) / scale);
        int y = 2;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1f);
        font.drawStringWithShadow(overlay, x, y, 0xFFFFFF);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    public void draw(float on, float distance) {
        if (main != null) main.draw();
        if (lever != null) {
            lever.draw(on * (alphaOn - alphaOff) + alphaOff, 0, 1, 0);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, (tr("Protects electrical components\nOpens contact if:\n  - Voltage exceeds a certain level\n  - Current exceeds the cable limit").split("\n")));
        if (Double.isFinite(currentLimit)) {
            list.add(tr("Breaker rating: %1$A", mods.eln.misc.Utils.plotValue(currentLimit)));
        }
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
