package mods.eln.sixnode.electricalsensor;

import mods.eln.Eln;
import mods.eln.Vars;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalSensorDescriptor extends SixNodeDescriptor {

    boolean voltageOnly;
    Obj3DPart main;

    public ElectricalSensorDescriptor(
        String name, String modelName,
        boolean voltageOnly) {
        super(name, ElectricalSensorElement.class, ElectricalSensorRender.class);
        this.voltageOnly = voltageOnly;
        main = Vars.obj.getPart(modelName, "main");

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    /*
	
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}
	
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		draw();
	}*/

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (voltageOnly) {
            list.add(tr("Measures voltage on cables."));
            list.add(tr("Has a signal output."));
        } else {
            list.add(tr("Measures electrical values on cables."));
            list.add(tr("Can measure Voltage/Power/Current"));
            list.add(tr("Has a signal output."));
        }
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
