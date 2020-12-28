package mods.eln.sixnode.currentsource;

import mods.eln.gui.GuiHelper;
import mods.eln.gui.GuiScreenEln;
import mods.eln.gui.GuiTextFieldEln;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;

import static mods.eln.i18n.I18N.tr;

public class CurrentSourceGui extends GuiScreenEln {

    GuiTextFieldEln current;
    CurrentSourceRender render;

    public CurrentSourceGui(CurrentSourceRender render) {
        this.render = render;
    }

    @Override
    protected GuiHelper newHelper() {
        return new GuiHelper(this, 50 + 12, 12 + 12);
    }

    @Override
    public void initGui() {
        super.initGui();

        current = newGuiTextField(6, 6, 50);
        current.setText((float) render.current);
        current.setObserver(this);
        current.setComment(new String[]{tr("Current sourced")});
    }

    @Override
    public void textFieldNewValue(GuiTextFieldEln textField, String value) {
        float newCurrent;

        try {
            newCurrent = NumberFormat.getInstance().parse(current.getText()).floatValue();
        } catch (ParseException e) {
            return;
        }

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream stream = new DataOutputStream(bos);

            render.preparePacketForServer(stream);
            stream.writeFloat(newCurrent);
            render.sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
