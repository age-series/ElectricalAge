package mods.eln.sixnode.electricalalarm;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalAlarmDescriptor extends SixNodeDescriptor {

    public float[] pinDistance;

    int light;
    Obj3D obj;
    Obj3DPart main, rot, lightPart;

    ResourceLocation onTexture, offTexture;
    String soundName;
    double soundTime;
    float soundLevel;
    public float rotSpeed = 0f;

    public ElectricalAlarmDescriptor(String name, Obj3D obj, int light, String soundName, double soundTime, float soundLevel) {
        super(name, ElectricalAlarmElement.class, ElectricalAlarmRender.class);
        this.obj = obj;
        this.soundName = soundName;
        this.soundTime = soundTime;
        this.soundLevel = soundLevel;
        this.light = light;

        if (obj != null) {
            main = obj.getPart("main");
            rot = obj.getPart("rot");
            lightPart = obj.getPart("light");

            onTexture = obj.getModelResourceLocation(obj.getString("onTexture"));
            offTexture = obj.getModelResourceLocation(obj.getString("offTexture"));
            if (rot != null)
                rotSpeed = rot.getFloat("speed");
            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
        setDefaultIcon("electricalalarm");
    }

    void draw(boolean warm, float rotAlpha) {
        if (warm) UtilsClient.bindTexture(onTexture);
        else UtilsClient.bindTexture(offTexture);
        if (main != null) main.drawNoBind();
        if (rot != null) {
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glColor3f(1.0f, 1.0f, 1.0f);
            if (warm) UtilsClient.disableLight();
            else GL11.glDisable(GL11.GL_LIGHTING);
            rot.drawNoBind(rotAlpha, 1f, 0f, 0f);
            if (warm) UtilsClient.enableLight();
            else GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_CULL_FACE);
        }
        if (lightPart != null) {
            UtilsClient.drawLightNoBind(lightPart);
        }
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
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        }
        draw(true, 0.0f);
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Emits an acoustic alarm if\nthe input signal is high").split("\n"));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
