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

    private lateinit var horizontalAdjust: GuiVerticalTrackBar
    private lateinit var verticalAdjust: GuiVerticalTrackBar

    private lateinit var beamAngleSelect0: GuiButton
    private lateinit var beamAngleSelect30: GuiButton
    private lateinit var beamAngleSelect60: GuiButton
    private lateinit var beamAngleSelect90: GuiButton
    private lateinit var beamAngleSelect120: GuiButton
    private lateinit var beamAngleSelect150: GuiButton
    private lateinit var beamAngleSelect180: GuiButton

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
            horizontalAdjust.setVisible(false)
            verticalAdjust.setVisible(false)

            beamAngleSelect0 =   newGuiButton(((176 / 2) - (162 / 2) + (0 * 39) + (0 * 2)), 7, 39, "beamAngle_0")
            beamAngleSelect30 =  newGuiButton(((176 / 2) - (162 / 2) + (1 * 39) + (1 * 2)), 7, 39, "beamAngle_30")
            beamAngleSelect60 =  newGuiButton(((176 / 2) - (162 / 2) + (2 * 39) + (2 * 2)), 7, 39, "beamAngle_60")
            beamAngleSelect90 =  newGuiButton(((176 / 2) - (162 / 2) + (3 * 39) + (3 * 2)), 7, 39, "beamAngle_90")
            beamAngleSelect120 = newGuiButton(((176 / 2) - (162 / 2) + (0 * 52) + (0 * 3)), (7 + 20 + 4), 52, "beamAngle_120")
            beamAngleSelect150 = newGuiButton(((176 / 2) - (162 / 2) + (1 * 52) + (1 * 3)), (7 + 20 + 4), 52, "beamAngle_150")
            beamAngleSelect180 = newGuiButton(((176 / 2) - (162 / 2) + (2 * 52) + (2 * 3)), (7 + 20 + 4), 52, "beamAngle_180")
        }
        else {
            beamAngleSelect0 =   newGuiButton(((176 / 2) - (126 / 2) + (0 * 30) + (0 * 2)), 7, 30, "beamAngle_0")
            beamAngleSelect30 =  newGuiButton(((176 / 2) - (126 / 2) + (1 * 30) + (1 * 2)), 7, 30, "beamAngle_30")
            beamAngleSelect60 =  newGuiButton(((176 / 2) - (126 / 2) + (2 * 30) + (2 * 2)), 7, 30, "beamAngle_60")
            beamAngleSelect90 =  newGuiButton(((176 / 2) - (126 / 2) + (3 * 30) + (3 * 2)), 7, 30, "beamAngle_90")
            beamAngleSelect120 = newGuiButton(((176 / 2) - (126 / 2) + (0 * 40) + (0 * 3)), (7 + 20 + 4), 40, "beamAngle_120")
            beamAngleSelect150 = newGuiButton(((176 / 2) - (126 / 2) + (1 * 40) + (1 * 3)), (7 + 20 + 4), 40, "beamAngle_150")
            beamAngleSelect180 = newGuiButton(((176 / 2) - (126 / 2) + (2 * 40) + (2 * 3)), (7 + 20 + 4), 40, "beamAngle_180")
        }

        selectGuiButton(render.beamAngle.value)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        horizontalAdjust.setComment(0, I18N.tr("Horizontal orientation: ${(horizontalAdjust.value).toInt()}°"))
        verticalAdjust.setComment(0, I18N.tr("Vertical orientation: ${(verticalAdjust.value).toInt()}°"))

        beamAngleSelect0.displayString = I18N.tr("0°")
        beamAngleSelect30.displayString = I18N.tr("30°")
        beamAngleSelect60.displayString = I18N.tr("60°")
        beamAngleSelect90.displayString = I18N.tr("90°")
        beamAngleSelect120.displayString = I18N.tr("120°")
        beamAngleSelect150.displayString = I18N.tr("150°")
        beamAngleSelect180.displayString = I18N.tr("180°")
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        if (obj === horizontalAdjust) render.clientSendFloat(FloodlightElement.HORIZONTAL_ADJUST_EVENT, horizontalAdjust.value)
        else if (obj === verticalAdjust) render.clientSendFloat(FloodlightElement.VERTICAL_ADJUST_EVENT, verticalAdjust.value)

        else if (obj === beamAngleSelect0) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 0f)
            selectGuiButton(obj)
        }
        else if (obj === beamAngleSelect30) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 30f)
            selectGuiButton(obj)
        }
        else if (obj === beamAngleSelect60) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 60f)
            selectGuiButton(obj)
        }
        else if (obj === beamAngleSelect90) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 90f)
            selectGuiButton(obj)
        }
        else if (obj === beamAngleSelect120) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 120f)
            selectGuiButton(obj)
        }
        else if (obj === beamAngleSelect150) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 150f)
            selectGuiButton(obj)
        }
        else if (obj === beamAngleSelect180) {
            render.clientSendFloat(FloodlightElement.BEAM_ANGLE_EVENT, 180f)
            selectGuiButton(obj)
        }
    }

    private fun selectGuiButton(beamAngle: Float) {
        if (beamAngle.toInt() == 0) beamAngleSelect0.enabled = false
        else if (beamAngle.toInt() == 30) beamAngleSelect30.enabled = false
        else if (beamAngle.toInt() == 60) beamAngleSelect60.enabled = false
        else if (beamAngle.toInt() == 90) beamAngleSelect90.enabled = false
        else if (beamAngle.toInt() == 120) beamAngleSelect120.enabled = false
        else if (beamAngle.toInt() == 150) beamAngleSelect150.enabled = false
        else if (beamAngle.toInt() == 180) beamAngleSelect180.enabled = false
    }

    private fun selectGuiButton(obj: GuiButton) {
        beamAngleSelect0.enabled = true
        beamAngleSelect30.enabled = true
        beamAngleSelect60.enabled = true
        beamAngleSelect90.enabled = true
        beamAngleSelect120.enabled = true
        beamAngleSelect150.enabled = true
        beamAngleSelect180.enabled = true

        obj.enabled = false
    }

}