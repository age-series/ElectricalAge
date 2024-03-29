package mods.eln.sixnode.hub;

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

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class HubDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main;
    Obj3DPart[] connection = new Obj3DPart[6];

    public HubDescriptor(String name, Obj3D obj) {
        super(name, HubElement.class, HubRender.class);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            for (int idx = 0; idx < 6; idx++) {
                connection[idx] = obj.getPart("con" + idx);
            }
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    void draw(boolean[] connectionGrid) {
        if (main != null) main.draw();
        for (int idx = 0; idx < 6; idx++) {
            if (connectionGrid[idx])
                GL11.glColor3f(40 / 255f, 40 / 255f, 40 / 255f);
            else
                GL11.glColor3f(150 / 255f, 150 / 255f, 150 / 255f);

            if (connection[idx] != null) connection[idx].draw();
        }
        GL11.glColor3f(1, 1, 1);
    }

/*	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		return true;
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		return true;
	}*/

	/*@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		//GL11.glTranslatef(-0.3f, -0.1f, 0f);
		draw(new boolean[]{true, true, true, true, true, true});
	}*/

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addWiring(newItemStack());
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Allows crossing cables\non one single block.").split("\n"));
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        list.add(tr("A bit contrived, as the wires could just cross over each other. Realism depends on the wires used."));
        return RealisticEnum.IDEAL;
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull EntityPlayer player) {
        return LRDU.Up;
    }
}
