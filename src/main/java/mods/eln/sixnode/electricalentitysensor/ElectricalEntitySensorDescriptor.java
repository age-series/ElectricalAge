package mods.eln.sixnode.electricalentitysensor;

import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalEntitySensorDescriptor extends SixNodeDescriptor {

    boolean useEntitySpeed = true;
    double speedFactor = 1 / 0.10;
    private Obj3DPart detector, haloMask;
    double maxRange;
    public float[] pinDistance;
    Obj3D obj;

    public ElectricalEntitySensorDescriptor(String name, Obj3D obj, double maxRange) {
        super(name, ElectricalEntitySensorElement.class, ElectricalEntitySensorRender.class);
        this.obj = obj;
        this.maxRange = maxRange;
        if (obj != null) {
            detector = obj.getPart("Detector");
            haloMask = obj.getPart("HaloMask");

            pinDistance = Utils.getSixNodePinDistance(detector);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw(boolean state, EntitySensorFilterDescriptor filter) {
        if (detector != null) detector.draw();
        if (state) {
            if (filter == null) {
                GL11.glColor3f(1f, 1f, 0f);
            } else {
                filter.glColor();
            }
            UtilsClient.drawLight(haloMask);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Output voltage increases\nif entities are moving around.").split("\n"));
        list.add(tr("Range: %1$ blocks", (int) maxRange));
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
            draw(false, null);
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
