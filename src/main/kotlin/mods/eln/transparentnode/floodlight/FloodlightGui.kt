package mods.eln.transparentnode.floodlight

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class FloodlightGui(player: EntityPlayer, inventory: IInventory, val render: FloodlightRender) : GuiContainerEln(FloodlightContainer(player, inventory)) {

    companion object {
        const val MIN_HORIZONTAL_ANGLE: Float  = 0f
        const val MAX_HORIZONTAL_ANGLE: Float = 360f
        const val MIN_VERTICAL_ANGLE: Float = 0f
        const val MAX_VERTICAL_ANGLE: Float = 180f
    }

    private lateinit var coneWidthSelect: GuiButton
    private lateinit var coneRangeSelect: GuiButton
    private lateinit var horizontalAdjust: GuiVerticalTrackBar
    private lateinit var verticalAdjust: GuiVerticalTrackBar

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }

    override fun initGui() {
        super.initGui()

        horizontalAdjust = newGuiVerticalTrackBar(7, 7 +(2), 14, 20 + 4 + 20 + 4 + 20 + 4 -(4))
        horizontalAdjust.setRange(MIN_HORIZONTAL_ANGLE, MAX_HORIZONTAL_ANGLE)
        horizontalAdjust.setStepIdMax(360)
        horizontalAdjust.value = render.swivelAngle.value

        verticalAdjust = newGuiVerticalTrackBar(7 + 14 + 4 + 126 + 4, 7 +(2), 14, 20 + 4 + 20 + 4 + 20 + 4 -(4))
        verticalAdjust.setRange(MIN_VERTICAL_ANGLE, MAX_VERTICAL_ANGLE)
        verticalAdjust.setStepIdMax(180)
        verticalAdjust.value = render.headAngle.value

        if (render.motorized) {
            coneWidthSelect = newGuiButton(176 / 2 - 162 / 2, 7, 162, "coneWidth")
            coneRangeSelect = newGuiButton(176 / 2 - 162 / 2, 7 + 20 + 4, 162, "coneRange")
            horizontalAdjust.setVisible(false)
            verticalAdjust.setVisible(false)
        }
        else {
            coneWidthSelect = newGuiButton(176 / 2 - 126 / 2, 7, 126, "coneWidth")
            coneRangeSelect = newGuiButton(176 / 2 - 126 / 2, 7 + 20 + 4, 126, "coneRange")
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        /*
        when (render.coneWidth) {
            FloodlightConeWidth.NARROW -> coneWidthSelect.displayString = I18N.tr("Cone width: narrow")
            FloodlightConeWidth.MEDIUM -> coneWidthSelect.displayString = I18N.tr("Cone width: medium")
            FloodlightConeWidth.WIDE -> coneWidthSelect.displayString = I18N.tr("Cone width: wide")
        }

        when (render.coneRange) {
            FloodlightConeRange.NEAR -> coneRangeSelect.displayString = I18N.tr("Cone range: near")
            FloodlightConeRange.MIDDLE -> coneRangeSelect.displayString = I18N.tr("Cone range: middle")
            FloodlightConeRange.FAR -> coneRangeSelect.displayString = I18N.tr("Cone range: far")
        }
        */

        coneWidthSelect.displayString = I18N.tr("Cone angle: ${render.coneWidth.int}°")
        coneRangeSelect.displayString = I18N.tr("Cone range: ${render.coneRange.int} blocks")

        horizontalAdjust.setComment(0, I18N.tr("Horizontal orientation: ${(horizontalAdjust.value).toInt()}°"))
        verticalAdjust.setComment(0, I18N.tr("Vertical orientation: ${(verticalAdjust.value).toInt()}°"))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)
        if (obj === coneWidthSelect) render.clientSendId(FloodlightElement.CONE_WIDTH_SELECT_EVENT)
        else if (obj === coneRangeSelect) render.clientSendId(FloodlightElement.CONE_RANGE_SELECT_EVENT)
        else if (obj === horizontalAdjust) render.clientSendFloat(FloodlightElement.HORIZONTAL_ADJUST_EVENT, horizontalAdjust.value)
        else if (obj === verticalAdjust) render.clientSendFloat(FloodlightElement.VERTICAL_ADJUST_EVENT, verticalAdjust.value)
    }

}