package mods.eln.sixnode.electricaldatalogger;

import mods.eln.gui.GuiContainerEln;
import mods.eln.gui.GuiHelperContainer;
import mods.eln.gui.GuiTextFieldEln;
import mods.eln.gui.GuiTextFieldEln.GuiTextFieldElnObserver;
import mods.eln.gui.IGuiObject;
import mods.eln.misc.FC;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.text.ParseException;

import static mods.eln.i18n.I18N.tr;

public class ElectricalDataLoggerGui extends GuiContainerEln implements GuiTextFieldElnObserver {

    GuiButton resetBt, voltageType, energyType, currentType, powerType, celsiusType, temperatureType, humidityType, percentType, noType, zeroLineToggle, config, printBt, pause;
    GuiTextFieldEln samplingPeriod, maxValue, minValue, yCursorValue;
    ElectricalDataLoggerRender render;
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 286;
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 204;
    private static final int GRAPH_TOP = 53;
    private static final int PRINT_BUTTON_Y = 179;
    private static final int CONFIG_FIELDS_Y = 157;
    private static final float TEMPERATURE_F_DEFAULT_MIN = -40f;
    private static final float TEMPERATURE_F_DEFAULT_MAX = 122f;

    enum State {display, config}

    State state = State.display;

    public ElectricalDataLoggerGui(EntityPlayer player, IInventory inventory, ElectricalDataLoggerRender render) {
        super(new ElectricalDataLoggerContainer(player, inventory));
        this.render = render;
    }

    void displayEntry() {
        config.displayString = tr("Configuration");
        config.visible = true;
        pause.visible = true;
        resetBt.visible = true;
        voltageType.visible = false;
        energyType.visible = false;
        percentType.visible = false;
        noType.visible = false;
        currentType.visible = false;
        powerType.visible = false;
        celsiusType.visible = false;
        temperatureType.visible = false;
        humidityType.visible = false;
        zeroLineToggle.visible = false;
        samplingPeriod.setVisible(false);
        maxValue.setVisible(false);
        minValue.setVisible(false);
        printBt.visible = true;
        state = State.display;
    }

    void configEntry() {
        pause.visible = false;
        config.visible = true;
        config.displayString = tr("Back to display");
        resetBt.visible = false;
        printBt.visible = false;
        voltageType.visible = true;
        energyType.visible = true;
        percentType.visible = true;
        noType.visible = true;
        currentType.visible = true;
        powerType.visible = true;
        celsiusType.visible = true;
        temperatureType.visible = true;
        humidityType.visible = true;
        zeroLineToggle.visible = true;
        samplingPeriod.setVisible(true);
        maxValue.setVisible(true);
        minValue.setVisible(true);
        state = State.config;
    }

    @Override
    public void initGui() {
        super.initGui();

        config = newGuiButton(GUI_WIDTH / 2 - 50, 8 - 2, 100, "");

        //@devs: Do not translate the following elements. Please.
        voltageType = newGuiButton(GUI_WIDTH / 2 - 75 - 2, 8 + 20 + 2 - 2, 75, tr("Voltage [V]"));
        currentType = newGuiButton(GUI_WIDTH / 2 + 2, 8 + 20 + 2 - 2, 75, tr("Current [A]"));
        powerType = newGuiButton(GUI_WIDTH / 2 - 75 - 2, 8 + 40 + 4 - 2, 75, tr("Power [W]"));
        celsiusType = newGuiButton(GUI_WIDTH / 2 + 2, 8 + 40 + 4 - 2, 75, tr("Temp [C]"));
        percentType = newGuiButton(GUI_WIDTH / 2 - 75 - 2, 8 + 60 + 6 - 2, 75, tr("Percent [-]%"));
        energyType = newGuiButton(GUI_WIDTH / 2 + 2, 8 + 60 + 6 - 2, 75, tr("Energy [J]"));
        temperatureType = newGuiButton(GUI_WIDTH / 2 - 75 - 2, 8 + 80 + 8 - 2, 75, tr("Temp [F]"));
        humidityType = newGuiButton(GUI_WIDTH / 2 + 2, 8 + 80 + 8 - 2, 75, tr("Humidity [%%]"));
        noType = newGuiButton(GUI_WIDTH / 2 - 75 - 2, 8 + 100 + 10 - 2, 75, tr("Unit"));
        zeroLineToggle = newGuiButton(GUI_WIDTH / 2 + 2, 8 + 100 + 10 - 2, 75, tr("0 Line"));

        resetBt = newGuiButton(GUI_WIDTH / 2 - 50, 8 + 20 + 2 - 2, 48, tr("Reset"));
        pause = newGuiButton(GUI_WIDTH / 2 + 2, 8 + 20 + 2 - 2, 48, "");

        printBt = newGuiButton(GUI_WIDTH / 2 - 48 / 2, PRINT_BUTTON_Y, 48, tr("Print"));

        samplingPeriod = newGuiTextField(30, CONFIG_FIELDS_Y, 50);
        samplingPeriod.setText(render.log.samplingPeriod);
        samplingPeriod.setComment(new String[]{tr("Sampling period")});

        maxValue = newGuiTextField(GUI_WIDTH - 50 - 30, CONFIG_FIELDS_Y - 5, 50);
        maxValue.setText(render.log.maxValue);
        maxValue.setComment(new String[]{tr("Y-axis max")});

        minValue = newGuiTextField(GUI_WIDTH - 50 - 30, CONFIG_FIELDS_Y + 10, 50);
        minValue.setText(render.log.minValue);
        minValue.setComment(new String[]{tr("Y-axis min")});

        displayEntry();
    }

    @Override
    public void guiObjectEvent(IGuiObject object) {
        super.guiObjectEvent(object);
        try {
            if (object == resetBt) {
                render.clientSend(ElectricalDataLoggerElement.resetId);
            } else if (object == pause) {
                render.clientSend(ElectricalDataLoggerElement.tooglePauseId);
            } else if (object == printBt) {
                render.clientSend(ElectricalDataLoggerElement.printId);
            } else if (object == currentType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.currentType);
            } else if (object == voltageType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.voltageType);
            } else if (object == energyType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.energyType);
            } else if (object == percentType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.percentType);
            } else if(object == noType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.noType);
            } else if (object == powerType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.powerType);
            } else if (object == celsiusType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.celsiusType);
            } else if (object == temperatureType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.temperatureType);
                render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, TEMPERATURE_F_DEFAULT_MIN);
                render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, TEMPERATURE_F_DEFAULT_MAX);
                render.log.minValue = TEMPERATURE_F_DEFAULT_MIN;
                render.log.maxValue = TEMPERATURE_F_DEFAULT_MAX;
                minValue.setText(TEMPERATURE_F_DEFAULT_MIN);
                maxValue.setText(TEMPERATURE_F_DEFAULT_MAX);
            } else if (object == humidityType) {
                render.clientSetByte(ElectricalDataLoggerElement.setUnitId, DataLogs.humidityType);
            } else if (object == zeroLineToggle) {
                render.log.showZeroLine = !render.log.showZeroLine;
                render.clientSetByte(ElectricalDataLoggerElement.setShowZeroLineId, (byte) (render.log.showZeroLine ? 1 : 0));
            } else if (object == config) {
                switch (state) {
                    case config:
                        displayEntry();
                        break;
                    case display:
                        configEntry();
                        break;
                    default:
                        break;
                }
            } else if (object == maxValue) {
                render.clientSetFloat(ElectricalDataLoggerElement.setMaxValue, NumberFormat.getInstance().parse(maxValue.getText()).floatValue());
            } else if (object == minValue) {
                render.clientSetFloat(ElectricalDataLoggerElement.setMinValue, NumberFormat.getInstance().parse(minValue.getText()).floatValue());
            } else if (object == samplingPeriod) {
                float value = NumberFormat.getInstance().parse(samplingPeriod.getText()).floatValue();
                if (value < 0.05f) value = 0.05f;
                samplingPeriod.setText(value);

                render.clientSetFloat(ElectricalDataLoggerElement.setSamplingPeriodeId, value);
            }
        } catch (ParseException e) {
        }
    }

    @Override
    protected void preDraw(float f, int x, int y) {
        super.preDraw(f, x, y);
        powerType.enabled = true;
        currentType.enabled = true;
        voltageType.enabled = true;
        celsiusType.enabled = true;
        percentType.enabled = true;
        energyType.enabled = true;
        temperatureType.enabled = true;
        humidityType.enabled = true;

        switch (render.log.unitType) {
            case DataLogs.currentType:
                currentType.enabled = false;
                break;
            case DataLogs.voltageType:
                voltageType.enabled = false;
                break;
            case DataLogs.powerType:
                powerType.enabled = false;
                break;
            case DataLogs.celsiusType:
                celsiusType.enabled = false;
                break;
            case DataLogs.percentType:
                percentType.enabled = false;
                break;
            case DataLogs.energyType:
                energyType.enabled = false;
                break;
            case DataLogs.noType:
                noType.enabled = false;
                break;
            case DataLogs.temperatureType:
                temperatureType.enabled = false;
                break;
            case DataLogs.humidityType:
                humidityType.enabled = false;
                break;
        }

        if (render.pause)
            pause.displayString = FC.DARK_YELLOW + "Paused";
        else
            pause.displayString = FC.BRIGHT_GREEN + "Running";
        zeroLineToggle.displayString = render.log.showZeroLine ? tr("0 Line On") : tr("0 Line Off");

        boolean a = inventorySlots.getSlot(ElectricalDataLoggerContainer.paperSlotId).getStack() != null;
        boolean b = inventorySlots.getSlot(ElectricalDataLoggerContainer.printSlotId).getStack() == null;
        printBt.enabled = a && b;
    }

    @Override
    protected void postDraw(float f, int x, int y) {
        super.postDraw(f, x, y);
        final float bckrndMargin = 0.05f;

        if (state == State.display) {

            GL11.glPushMatrix();
            GL11.glTranslatef(guiLeft + 8, guiTop + GRAPH_TOP, 0);
            GL11.glScalef(50, 50, 1f);

            GL11.glColor4f(0.15f, 0.15f, 0.15f, 1.0f);
            UtilsClient.disableTexture();
            UtilsClient.disableCulling();
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(-bckrndMargin, -bckrndMargin);
            GL11.glVertex2f(3.2f + bckrndMargin, -bckrndMargin);
            GL11.glVertex2f(3.2f + bckrndMargin, 1.6f + 3 * bckrndMargin);
            GL11.glVertex2f(-bckrndMargin, 1.6f + 3 * bckrndMargin);
            GL11.glEnd();
            UtilsClient.enableCulling();
            UtilsClient.enableTexture();

            GL11.glColor4f(render.descriptor.cr, render.descriptor.cg, render.descriptor.cb, 1);
            render.log.draw(2.9f, 1.6f, render.descriptor.textColor);
            GL11.glPopMatrix();
        }
    }

    @Override
    protected GuiHelperContainer newHelper() {
        return new GuiHelperContainer(this, GUI_WIDTH, GUI_HEIGHT, PLAYER_INV_X, PLAYER_INV_Y);
    }
}
