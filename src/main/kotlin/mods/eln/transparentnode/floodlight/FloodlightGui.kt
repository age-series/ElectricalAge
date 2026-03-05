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
        const val MIN_BEAM_WIDTH: Double = 0.0
        const val MAX_BEAM_WIDTH: Double = 45.0
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
        horizontalAdjust.setStepIdMax(360)
        horizontalAdjust.value = render.swivelAngle.toFloat()

        verticalAdjust = newGuiHorizontalTrackBar(7, 26, 162, 12)
        verticalAdjust.setRange(MIN_VERTICAL_ANGLE.toFloat(), MAX_VERTICAL_ANGLE.toFloat())
        verticalAdjust.setStepIdMax(180)
        verticalAdjust.value = render.headAngle.toFloat()

        beamAdjust = newGuiHorizontalTrackBar(7, 44, 162, 12)
        beamAdjust.setRange(MIN_BEAM_WIDTH.toFloat(), MAX_BEAM_WIDTH.toFloat())
        beamAdjust.setStepIdMax(180)
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

        horizontalAdjust.setComment(0, I18N.tr("Horizontal angle: ${(horizontalAdjust.value).toInt()}°"))
        verticalAdjust.setComment(0, I18N.tr("Vertical angle: ${(verticalAdjust.value).toInt()}°"))
        beamAdjust.setComment(0, I18N.tr("Beam width: ${(beamAdjust.value).toInt()}°"))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        when (obj) {
            horizontalAdjust -> render.clientSendDouble(FloodlightElement.HORIZONTAL_ADJUST_EVENT, horizontalAdjust.value.toDouble())
            verticalAdjust -> render.clientSendDouble(FloodlightElement.VERTICAL_ADJUST_EVENT, verticalAdjust.value.toDouble())
            beamAdjust -> render.clientSendDouble(FloodlightElement.BEAM_ADJUST_EVENT, beamAdjust.value.toDouble())
        }
    }

}