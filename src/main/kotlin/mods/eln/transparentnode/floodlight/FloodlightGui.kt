package mods.eln.transparentnode.floodlight

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.entity.player.EntityPlayer

class FloodlightGui(player: EntityPlayer, val render: FloodlightRender) :
    GuiContainerEln(FloodlightContainer(player, render.inventory, render.descriptor)) {

    companion object {
        const val ADJUST_HORIZONTAL_ANGLE_EVENT: Byte = 0
        const val ADJUST_VERTICAL_ANGLE_EVENT: Byte = 1
        const val ADJUST_BEAM_WIDTH_EVENT: Byte = 2

        const val MIN_HORIZONTAL_ANGLE  = 0.0
        const val MAX_HORIZONTAL_ANGLE = 360.0
        const val MIN_VERTICAL_ANGLE = 0.0
        const val MAX_VERTICAL_ANGLE = 180.0
        const val MIN_BEAM_WIDTH = 0.0
        const val MAX_BEAM_WIDTH = 45.0
    }

    private lateinit var horizontalAdjust: GuiHorizontalTrackBar
    private lateinit var verticalAdjust: GuiHorizontalTrackBar
    private lateinit var beamAdjust: GuiHorizontalTrackBar

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }

    override fun initGui() {
        super.initGui()

        horizontalAdjust = newGuiHorizontalTrackBar(7, 8, 162, 12)
        horizontalAdjust.setRange(MIN_HORIZONTAL_ANGLE.toFloat(), MAX_HORIZONTAL_ANGLE.toFloat())
        horizontalAdjust.setStepIdMax((MAX_HORIZONTAL_ANGLE - MIN_HORIZONTAL_ANGLE).toInt())
        horizontalAdjust.value = render.swivelAngle.toFloat()

        verticalAdjust = newGuiHorizontalTrackBar(7, 26, 162, 12)
        verticalAdjust.setRange(MIN_VERTICAL_ANGLE.toFloat(), MAX_VERTICAL_ANGLE.toFloat())
        verticalAdjust.setStepIdMax((MAX_VERTICAL_ANGLE - MIN_VERTICAL_ANGLE).toInt())
        verticalAdjust.value = render.headAngle.toFloat()

        beamAdjust = newGuiHorizontalTrackBar(7, 44, 162, 12)
        beamAdjust.setRange(MIN_BEAM_WIDTH.toFloat(), MAX_BEAM_WIDTH.toFloat())
        beamAdjust.setStepIdMax((MAX_BEAM_WIDTH - MIN_BEAM_WIDTH).toInt())
        beamAdjust.value = render.beamWidth.toFloat()

        if (render.motorized) {
            horizontalAdjust.setEnable(false)
            verticalAdjust.setEnable(false)
            beamAdjust.setEnable(false)
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (render.motorized) {
            horizontalAdjust.value = render.swivelAngle.toFloat()
            verticalAdjust.value = render.headAngle.toFloat()
            beamAdjust.value = render.beamWidth.toFloat()
        }

        horizontalAdjust.setComment(0, I18N.tr("Horizontal angle: %1$\u00B0", horizontalAdjust.value.toInt()))
        verticalAdjust.setComment(0, I18N.tr("Vertical angle: %1$\u00B0", verticalAdjust.value.toInt()))
        beamAdjust.setComment(0, I18N.tr("Beam width: %1$\u00B0", beamAdjust.value.toInt()))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        when (obj) {
            horizontalAdjust -> render.clientSendDouble(ADJUST_HORIZONTAL_ANGLE_EVENT, horizontalAdjust.value.toDouble())
            verticalAdjust -> render.clientSendDouble(ADJUST_VERTICAL_ANGLE_EVENT, verticalAdjust.value.toDouble())
            beamAdjust -> render.clientSendDouble(ADJUST_BEAM_WIDTH_EVENT, beamAdjust.value.toDouble())
        }
    }

}