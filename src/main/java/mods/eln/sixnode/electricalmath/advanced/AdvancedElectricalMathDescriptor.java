package mods.eln.sixnode.electricalmath.advanced;

import mods.eln.Eln;
import mods.eln.gui.GuiLabel;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import mods.eln.wiki.GuiVerticalExtender;
import mods.eln.wiki.ItemDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;
import static mods.eln.sixnode.electricalmath.advanced.AdvancedElectricalMathElement.nominalU;
import static mods.eln.sixnode.electricalmath.advanced.AdvancedElectricalMathElement.wattsStandBy;
import static mods.eln.sixnode.electricalmath.advanced.AdvancedElectricalMathElement.wattsPerRedstone;
import static mods.eln.sixnode.electricalmath.advanced.AdvancedElectricalMathElement.wattsPerVoltageOutPut;


public class AdvancedElectricalMathDescriptor extends SixNodeDescriptor implements ItemDefault.IPlugIn {

    Obj3D obj;
    Obj3DPart main, door;
    Led[] advanced_leds = new Led[8];
    public float[] pinDistance;
    float alphaOff;

    public AdvancedElectricalMathDescriptor(String name, Obj3D obj) {
        super(name, AdvancedElectricalMathElement.class, AdvancedElectricalMathRender.class);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            door = obj.getPart("door");
            if (door != null) {
                alphaOff = door.getFloat("alphaOff");
            }
            for (int idx = 0; idx < 8; idx++) {
                advanced_leds[idx] = new Led(obj.getPart("led" + idx));
            }

            pinDistance = Utils.getSixNodePinDistance(main);
            pinDistance[2] -= 1;
        }
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    void draw(float open) {
        if (main != null) main.draw();
        if (door != null) door.draw((1f - open) * alphaOff, 0f, 1f, 0f);

        for (Led advancedLed : advanced_leds) {
            advancedLed.draw();
        }
    }

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
    public void renderItem(@NotNull ItemRenderType type, @NotNull ItemStack item, @NotNull Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            GL11.glTranslatef(-0.3f, -0.1f, 0f);
            GL11.glRotatef(90, 1, 0, 0);
            draw(0.7f);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add("Calculates 16 output signal from");
        list.add("32 inputs signal (A0, A1 ... A15, B0, B1 ... B15)");
        list.add("using connections with signal bus cables.");
        list.add("Nominal voltage: " + Utils.plotValue(nominalU) + "~10V");
        list.add(Utils.plotPower("StandBy power:", wattsStandBy));
        list.add(Utils.plotPower("Power per operation:", wattsPerRedstone));
        list.add(Utils.plotPower("Power per output:", wattsPerVoltageOutPut));
    }

    @Override
    public int top(int y, GuiVerticalExtender extender, ItemStack stack) {
        extender.add(new GuiLabel(6, y, tr("Applicable mathematical operators:")));
        y += 9;
        extender.add(new GuiLabel(6, y, "  + - * / > < "));
        y += 9;
        y += 9;
        extender.add(new GuiLabel(6, y, tr("Applicable boolean operators:")));
        y += 9;
        extender.add(new GuiLabel(6, y, "  & | = ^"));
        y += 9;
        y += 9;
        extender.add(new GuiLabel(6, y, tr("Applicable functions:")));
        y += 9;
        extender.add(new GuiLabel(6, y, "  if(condition,then,else)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  min(x,y)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  max(x,y)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  sin(alpha)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  cos(alpha)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  abs(value)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  ramp(periode)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  rs(reset,set)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  integrate(value,resetTrigger)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  integrate(value,minOutput,maxOutput)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  derivate(value)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  batteryCharge(normalizedBatVoltage)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  rc(tao,value)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  pid(target,hit,p,i,d)"));
        y += 9;
        extender.add(new GuiLabel(6, y, "  pid(target,hit,p,i,d,minOut,maxOut)"));
        y += 9;

        y += 9;
        return y;
    }

    @Override
    public int bottom(int y, GuiVerticalExtender extender, ItemStack stack) {
        return y;
    }
}
