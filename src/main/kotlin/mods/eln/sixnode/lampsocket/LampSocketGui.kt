package mods.eln.sixnode.lampsocket

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.entity.player.EntityPlayer

class LampSocketGui(player: EntityPlayer, val render: LampSocketRender) :
    GuiContainerEln(LampSocketContainer(player, render.inventory, render.descriptor)) {

    companion object {
        const val TOGGLE_GROUNDED_EVENT: Byte = 0
        const val TOGGLE_POWER_SOURCE_EVENT: Byte = 1
        const val UPDATE_LAMP_SUPPLY_CHANNEL_EVENT: Byte = 2
        const val ADJUST_ROTATION_ANGLE_EVENT: Byte = 3

        const val MIN_ROTATION_ANGLE = -90.0
        const val MAX_ROTATION_ANGLE = 90.0
    }

    private lateinit var buttonGrounded: GuiButtonEln
    private lateinit var buttonPowerSource: GuiButtonEln
    private lateinit var textboxLampSupplyChannel: GuiTextFieldEln
    private lateinit var trackbarRotationAngle: GuiVerticalTrackBar

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }

    override fun initGui() {
        super.initGui()

        buttonPowerSource = newGuiButton(16, 7, 144, "")
        buttonGrounded = newGuiButton(16, 33, 144, "")

        textboxLampSupplyChannel = newGuiTextField(16+1, 36+1, 144-2)
        textboxLampSupplyChannel.setComment(0, I18N.tr("Specify the lamp supply channel"))
        textboxLampSupplyChannel.setText(render.lampSupplyChannel)

        trackbarRotationAngle = newGuiHorizontalTrackBar(43,61,90, 14)
        trackbarRotationAngle.setRange(MIN_ROTATION_ANGLE.toFloat(), MAX_ROTATION_ANGLE.toFloat())
        trackbarRotationAngle.setStepIdMax(180)
        trackbarRotationAngle.value = render.projectionRotationAngle.toFloat()
        trackbarRotationAngle.visible = render.descriptor.enableProjectionRotation
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (render.grounded) buttonGrounded.displayString = I18N.tr("Grounded")
        else buttonGrounded.displayString = I18N.tr("Ungrounded")

        if (render.poweredByLampSupply) {
            textboxLampSupplyChannel.visible = true
            buttonGrounded.visible = false
            buttonPowerSource.displayString = I18N.tr("Powered by lamp supply")

            val lampStack = render.inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
            val cableStack = render.inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID)

            when {
                cableStack == null -> textboxLampSupplyChannel.setComment(1, "§4" + I18N.tr("Cable slot empty"))
                lampStack == null -> textboxLampSupplyChannel.setComment(1, "§4" + I18N.tr("Lamp slot empty"))
                render.activeLampSupplyConnection -> textboxLampSupplyChannel.setComment(1, "§2" + I18N.tr("Connected to %1$", render.lampSupplyChannel))
                else -> textboxLampSupplyChannel.setComment(1, "§4" + I18N.tr("%1$ is not in range!", render.lampSupplyChannel))
            }
        } else {
            textboxLampSupplyChannel.visible = false
            buttonGrounded.visible = true
            buttonPowerSource.displayString = I18N.tr("Powered by cable")
        }

        trackbarRotationAngle.setComment(0, I18N.tr("Beam orientation: %1$\u00B0", trackbarRotationAngle.value.toInt()))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        when (obj) {
            buttonGrounded -> render.clientSend(TOGGLE_GROUNDED_EVENT.toInt())
            buttonPowerSource -> render.clientSend(TOGGLE_POWER_SOURCE_EVENT.toInt())
            textboxLampSupplyChannel -> render.clientSetString(UPDATE_LAMP_SUPPLY_CHANNEL_EVENT, textboxLampSupplyChannel.text)
            trackbarRotationAngle -> render.clientSetDouble(ADJUST_ROTATION_ANGLE_EVENT, trackbarRotationAngle.value.toDouble())
        }
    }
}