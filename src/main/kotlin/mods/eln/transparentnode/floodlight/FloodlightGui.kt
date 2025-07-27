package mods.eln.transparentnode.floodlight

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class FloodlightGui(player: EntityPlayer, inventory: IInventory, val render: FloodlightRender) : GuiContainerEln(FloodlightContainer(player, inventory)) {

    companion object {
        const val MIN_HORIZONTAL_ANGLE: Float  = 0f
        const val MAX_HORIZONTAL_ANGLE: Float = 360f
        const val MIN_VERTICAL_ANGLE: Float = 0f
        const val MAX_VERTICAL_ANGLE: Float = 180f
        const val MIN_SHUTTER_ANGLE: Float = 0f
        const val MAX_SHUTTER_ANGLE: Float = 180f
    }

    private lateinit var horizontalAdjust: GuiVerticalTrackBar
    private lateinit var verticalAdjust: GuiVerticalTrackBar
    private lateinit var shutterAdjust: GuiVerticalTrackBar

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }

    override fun initGui() {
        super.initGui()

        horizontalAdjust = newGuiVerticalTrackBar(7, 7+2, 162, 14-4)
        horizontalAdjust.setRange(MIN_HORIZONTAL_ANGLE, MAX_HORIZONTAL_ANGLE)
        horizontalAdjust.setStepIdMax(360)
        horizontalAdjust.value = render.swivelAngle

        verticalAdjust = newGuiVerticalTrackBar(7, 25+2, 162, 14-4)
        verticalAdjust.setRange(MIN_VERTICAL_ANGLE, MAX_VERTICAL_ANGLE)
        verticalAdjust.setStepIdMax(180)
        verticalAdjust.value = render.headAngle

        shutterAdjust = newGuiVerticalTrackBar(7, 43+2, 162, 14-4)
        shutterAdjust.setRange(MIN_SHUTTER_ANGLE, MAX_SHUTTER_ANGLE)
        shutterAdjust.setStepIdMax(180)
        shutterAdjust.value = render.shutterAngle

        if (render.motorized) {
            horizontalAdjust.setEnable(false)
            verticalAdjust.setEnable(false)
            shutterAdjust.setEnable(false)
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (render.motorized) {
            horizontalAdjust.value = render.swivelAngle
            verticalAdjust.value = render.headAngle
            shutterAdjust.value = render.shutterAngle
        }

        horizontalAdjust.setComment(0, I18N.tr("Horizontal angle: ${(horizontalAdjust.value).toInt()}°"))
        verticalAdjust.setComment(0, I18N.tr("Vertical angle: ${(verticalAdjust.value).toInt()}°"))
        shutterAdjust.setComment(0, I18N.tr("Shutter angle: ${(shutterAdjust.value).toInt()}°"))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        when (obj) {
            horizontalAdjust -> render.clientSendFloat(FloodlightElement.HORIZONTAL_ADJUST_EVENT, horizontalAdjust.value)
            verticalAdjust -> render.clientSendFloat(FloodlightElement.VERTICAL_ADJUST_EVENT, verticalAdjust.value)
            shutterAdjust -> render.clientSendFloat(FloodlightElement.SHUTTER_ADJUST_EVENT, shutterAdjust.value)
        }
    }

}