package mods.eln.gui;

public class GuiHorizontalTrackBar extends GuiVerticalTrackBar {

    public GuiHorizontalTrackBar(int xPosition, int yPosition, int width, int height, GuiHelper helper) {
        super(xPosition, yPosition, width, height, helper);
    }

    @Override
    public void setValue(float value) {
        if (!drag) {
            this.stepId = (int) ((value - max) / (min - max) * stepIdMax + 0.5);
            stepLimit();
        }
    }

    @Override
    public float getValue() {
        return max + (min - max) * stepId / stepIdMax;
    }

    @Override
    public void mouseMove(int x, int y) {
        if (drag) {
            stepId = (int) ((1.0 - (double) (x - (xPosition + 2)) / (width - 4) + 1.0 / stepIdMax / 2.0) * stepIdMax);

            stepLimit();
        }
    }

    @Override
    public int getCursorPosition() {
        return (int) ((xPosition + 2) + (width - 4) - 1.0 * stepId / stepIdMax * (width - 4));
    }

    @Override
    public void drawBase(float par1, int x, int y) {
        if (!visible) return;

        drawRect(xPosition, yPosition, xPosition + width, yPosition + height, 0xFF404040);
        drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, 0xFF606060);
        drawRect(xPosition + 2, yPosition + 2, xPosition + width - 2, yPosition + height - 2, 0xFF808080);
    }

    @Override
    public void drawBare(float par1, int x, int y) {
        if (!visible) return;
        if (!sliderDrawEnable) return;

        drawRect(getCursorPosition() + 2, yPosition + height + 2, getCursorPosition() - 2, yPosition - 2, 0xFF202020);
        drawRect(getCursorPosition() + 1, yPosition + height + 1, getCursorPosition() - 1, yPosition - 1, 0xFF606060);
    }

}
