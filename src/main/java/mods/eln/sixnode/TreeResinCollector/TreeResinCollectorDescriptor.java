package mods.eln.sixnode.TreeResinCollector;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TreeResinCollectorDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main, fill;

    float emptyS, emptyT;

    public TreeResinCollectorDescriptor(String name, Obj3D obj) {
        super(name, TreeResinCollectorElement.class, TreeResinCollectorRender.class);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            fill = obj.getPart("fill");
            if (fill != null) {
                emptyT = fill.getFloat("emptyT");
                emptyS = fill.getFloat("emptyS");
            }
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    void draw(float factor) {
        if (main != null) main.draw();
        if (fill != null) {
            if (factor > 1f) factor = 1f;
            factor = (1f - factor);
            GL11.glTranslatef(0f, 0f, factor * emptyT);
            GL11.glScalef(1f - factor * (1f - emptyS), 1f - factor * (1f - emptyS), 1f);
            fill.draw();
        }
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
            draw(0.0f);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Produces Tree Resin over\ntime when put on a tree.").split("\n"));
    }

    public static boolean isWood(Block b) {
        for (ItemStack s : OreDictionary.getOres("treeWood")) {
            if (s.getItem() == Item.getItemFromBlock(b)) return true;
        }
        for (ItemStack s : OreDictionary.getOres("logWood")) {
            if (s.getItem() == Item.getItemFromBlock(b)) return true;
        }

        return false;
    }

    public static boolean isLeaf(Block b) {
        for (ItemStack s : OreDictionary.getOres("treeLeaves")) {
            if (s.getItem() == Item.getItemFromBlock(b)) return true;
        }
        return false;
    }

    @Override
    public boolean canBePlacedOnSide(EntityPlayer player, Coordonate c, Direction side) {
        Block b = c.getBlock();
        if (!isWood(b) || side.isY()) {
            Utils.addChatMessage(player, tr("This block can only be placed on the side of a tree!"));
            return false;
        }
        return true;
    }
}
