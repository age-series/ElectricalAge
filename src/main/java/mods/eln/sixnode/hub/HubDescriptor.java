package mods.eln.sixnode.hub;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Allows crossing cables\non one single block.").split("\n"));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return LRDU.Up;
    }
}
