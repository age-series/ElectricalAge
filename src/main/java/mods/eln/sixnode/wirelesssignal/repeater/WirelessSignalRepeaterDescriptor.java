package mods.eln.sixnode.wirelesssignal.repeater;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class WirelessSignalRepeaterDescriptor extends SixNodeDescriptor {

    public int range;
    private Obj3D obj;
    Obj3DPart main, led;

    public WirelessSignalRepeaterDescriptor(String name,
                                            Obj3D obj,
                                            int range) {
        super(name, WirelessSignalRepeaterElement.class, WirelessSignalRepeaterRender.class);
        this.range = range;
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            led = obj.getPart("led");
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
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
                GL11.glScalef(2.8f, 2.8f, 2.8f);
            }
            draw();
        }
    }

    public void draw() {
        if (main != null) main.draw();

		/*if (led != null) {
			UtilsClient.ledOnOffColor(connection);
			UtilsClient.drawLight(led);
		}*/
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
