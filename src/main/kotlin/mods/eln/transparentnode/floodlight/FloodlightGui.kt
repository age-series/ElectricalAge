package mods.eln.transparentnode.floodlight

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class FloodlightGui(player: EntityPlayer, inventory: IInventory, val render: FloodlightRender) : GuiContainerEln(FloodlightContainer(player, inventory)) {

    companion object {
        const val MIN_HORIZONTAL_ANGLE: Double  = 0.0
        const val MAX_HORIZONTAL_ANGLE: Double = 360.0
        const val MIN_VERTICAL_ANGLE: Double = 0.0
        const val MAX_VERTICAL_ANGLE: Double = 180.0
        const val MIN_SHUTTER_ANGLE: Double = 0.0
        const val MAX_SHUTTER_ANGLE: Double = 180.0
    }

    private lateinit var horizontalAdjust: GuiHorizontalTrackBar
    private lateinit var verticalAdjust: GuiHorizontalTrackBar
    private lateinit var shutterAdjust: GuiHorizontalTrackBar

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }

    override fun initGui() {
        super.initGui()

        horizontalAdjust = newGuiHorizontalTrackBar(7, 8, 162, 12)
        horizontalAdjust.setRange(MIN_HORIZONTAL_ANGLE.toFloat(), MAX_HORIZONTAL_ANGLE.toFloat())
        horizontalAdjust.setStepIdMax(360)
        horizontalAdjust.value = render.swivelAngle.toFloat()

        verticalAdjust = newGuiHorizontalTrackBar(7, 26, 162, 12)
        verticalAdjust.setRange(MIN_VERTICAL_ANGLE.toFloat(), MAX_VERTICAL_ANGLE.toFloat())
        verticalAdjust.setStepIdMax(180)
        verticalAdjust.value = render.headAngle.toFloat()

        shutterAdjust = newGuiHorizontalTrackBar(7, 44, 162, 12)
        shutterAdjust.setRange(MIN_SHUTTER_ANGLE.toFloat(), MAX_SHUTTER_ANGLE.toFloat())
        shutterAdjust.setStepIdMax(180)
        shutterAdjust.value = render.shutterAngle.toFloat()

        if (render.motorized) {
            horizontalAdjust.setEnable(false)
            verticalAdjust.setEnable(false)
            shutterAdjust.setEnable(false)
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (render.motorized) {
            horizontalAdjust.value = render.swivelAngle.toFloat()
            verticalAdjust.value = render.headAngle.toFloat()
            shutterAdjust.value = render.shutterAngle.toFloat()
        }

        horizontalAdjust.setComment(0, I18N.tr("Horizontal angle: ${(horizontalAdjust.value).toInt()}°"))
        verticalAdjust.setComment(0, I18N.tr("Vertical angle: ${(verticalAdjust.value).toInt()}°"))
        shutterAdjust.setComment(0, I18N.tr("Shutter angle: ${(shutterAdjust.value).toInt()}°"))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        when (obj) {
            horizontalAdjust -> render.clientSendDouble(FloodlightElement.HORIZONTAL_ADJUST_EVENT, horizontalAdjust.value.toDouble())
            verticalAdjust -> render.clientSendDouble(FloodlightElement.VERTICAL_ADJUST_EVENT, verticalAdjust.value.toDouble())
            shutterAdjust -> render.clientSendDouble(FloodlightElement.SHUTTER_ADJUST_EVENT, shutterAdjust.value.toDouble())
        }
    }

}