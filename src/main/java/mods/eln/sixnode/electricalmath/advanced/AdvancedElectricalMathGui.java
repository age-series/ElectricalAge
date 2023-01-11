package mods.eln.sixnode.electricalmath.advanced;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.BasicContainer;
import mods.eln.misc.Utils;
import mods.eln.sixnode.electricalcable.ElectricalSignalBusCableElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.EnumChatFormatting;

import static mods.eln.i18n.I18N.tr;

public class AdvancedElectricalMathGui extends GuiContainerEln {
    private static final int xSize = 200*2 + 32;
    private static final int ySize = 8*26 + 30;

    GuiTextFieldEln[] expression = new GuiTextFieldEln[16];
    AdvancedElectricalMathRender render;
    int errorTick = 0;

    public AdvancedElectricalMathGui(EntityPlayer player, AdvancedElectricalMathRender render) {
        super(new BasicContainer(player, null, new Slot[]{}));
        this.render = render;
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, xSize, ySize, 8, 8 + 18*8 + 4);
    }

    @Override
    public void initGui() {
        super.initGui();

        for (int i = 0; i < 16; i++) {
            int xOffset = (i > 7 ? 16 + 200 : 0);
            int yOffset = (i > 7 ? 18*(i-8) : 18 * i);
            GuiTextFieldEln expression = newGuiTextField(8 + xOffset, 8 + yOffset, 200);
            this.expression[i] = expression;

            String text = "";
            for (Gate.GateInfo info : render.info) {
                if (info.index == i){
                    text = info.expression;
                    break;
                }
            }

            expression.setText(text);
            expression.setObserver(this);
            expression.setComment(new String[]{
                tr("Output voltage formula"),
                ElectricalSignalBusCableElement.Companion.getWool_to_chat()[15 - i] + "ColorCode: " + i,
                tr("Inputs are") + " §4A0, A1...A15 §2B0, B1...B15"
            });
        }
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        byte idx = 0;
        for (GuiTextFieldEln textField : expression) {
            if (object == textField){
                render.clientSetString(idx, textField.getText());
            }
            idx++;
        }
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        super.postDraw(f, x, y);
        if (!render.isPowered) {
            for (GuiTextFieldEln textFieldEln : expression) {
                textFieldEln.setText("");
            }
            return;
        }

        StringBuilder invalid = new StringBuilder("Invalid equation (");
        boolean error = false;
        boolean isWriting = false;

        for (int i = 0; i < 16; i++) {
            Gate.GateInfo info = null;
            for (Gate.GateInfo gateInfo : render.info) {
                if (gateInfo.index == i){
                    info = gateInfo;
                    break;
                }
            }

            if (!expression[i].getText().equals("")) {
                if (!expression[i].getText().equals(info == null ? "" : info.expression)){
                    isWriting = true;
                    continue;
                }
                if (!(info == null || info.isValid)) {
                    invalid.append(i).append(", ");
                    expression[i].setTextColor(0xFFFF5555);
                    error = true;
                } else {
                    expression[i].setTextColor(0xFFFFFFFF);
                }
            }
        }
        invalid.replace(invalid.length()-2, invalid.length(), ")");

        if (error){
            helper.drawString(8 + 216, 8 + 18*8 + 4, 0xFFFF0000, invalid.toString());
        } else {
            if (render.isOverORUnderVoltage == 0)
                helper.drawString(8 + 216, 8 + 18*8 + 4, 0xFF108F00, "No error");
            else {
                errorTick += 1;
                int c = errorTick > 90 ? 0xFFFFFF55 : 0xFFFFAA00;
                if (errorTick > 180)
                    errorTick = 0;

                if (render.isOverORUnderVoltage == -1)
                    helper.drawString(8 + 216, 8 + 18*8 + 4, c, "Under voltage detected! ");
                else if (render.isOverORUnderVoltage == 1)
                    helper.drawString(8 + 216, 8 + 18*8 + 4, c, "Over voltage detected!");
            }
        }
        if (isWriting){
            helper.drawString(8 + 216, 8 + 18*8 + 4 + 16, 0xFFFFFF55, "Waiting for completion...");
        } else {
            helper.drawString(224, 156 + 16, 0xFF108F00, "Power required: " + Utils.plotPower(render.powerNeeded));
            helper.drawString(224, 156 + 32, 0xFF108F00, "->Outputs: " + Utils.plotPower(render.info.size() * AdvancedElectricalMathElement.wattsPerVoltageOutPut));
            helper.drawString(224, 156 + 48, 0xFF108F00, "->Operations: " + Utils.plotPower(render.powerNeeded - AdvancedElectricalMathElement.wattsStandBy - render.info.size() * AdvancedElectricalMathElement.wattsPerVoltageOutPut));
        }
    }
}
