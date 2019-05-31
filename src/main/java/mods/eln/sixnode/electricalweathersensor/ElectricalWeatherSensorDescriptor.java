package mods.eln.sixnode.electricalweathersensor;

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

public class ElectricalWeatherSensorDescriptor extends SixNodeDescriptor {

    private Obj3DPart main;
    public float[] pinDistance;

    Obj3D obj;

    public ElectricalWeatherSensorDescriptor(String name, Obj3D obj) {
        super(name, ElectricalWeatherSensorElement.class, ElectricalWeatherSensorRender.class);
        this.obj = obj;

        if (obj != null) {
            main = obj.getPart("main");

            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw() {
        UtilsClient.disableCulling();
        if (main != null) main.draw();
        UtilsClient.enableCulling();
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Provides an electrical signal\ndepending the actual weather.").split("\n"));
        list.add(tr("Clear: %1$V", 0));
        list.add(tr("Rain: %1$V", Utils.plotValue(Eln.SVU / 2)));
        list.add(tr("Storm: %1$V", Utils.plotValue(Eln.SVU)));
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
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            GL11.glScalef(2f, 2f, 2f);
            draw();
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
