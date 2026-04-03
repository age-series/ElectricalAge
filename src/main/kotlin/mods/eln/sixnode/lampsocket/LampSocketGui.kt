package mods.eln.sixnode.lampsocket

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.EntityPlayer

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
class LampSocketGui(player: EntityPlayer, val render: LampSocketRender) :
    GuiContainerEln(LampSocketContainer(player, render.inventory, render.descriptor)) {

    companion object {
        const val TOGGLE_GROUNDED_EVENT: Byte = 0
        const val TOGGLE_POWER_SOURCE_EVENT: Byte = 1
        const val UPDATE_LAMP_SUPPLY_CHANNEL_EVENT: Byte = 2
        const val ADJUST_ALPHA_Z_EVENT: Byte = 3
    }

    private lateinit var buttonGrounded: GuiButton
    private lateinit var buttonPowerSource: GuiButton
    private lateinit var textboxLampSupplyChannel: GuiTextFieldEln
    private lateinit var trackbarAlphaZ: GuiVerticalTrackBar

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }

    override fun initGui() {
        super.initGui()

        buttonGrounded = newGuiButton(7, 56, 54, "")
        buttonGrounded.visible = false // TODO: Whenever grounding is actually implemented, remove this.

        if (render.descriptor.alphaZMax == render.descriptor.alphaZMin) {
            buttonPowerSource = newGuiButton(16, 7, 144, "")
            textboxLampSupplyChannel = newGuiTextField(16+1, 35+1, 144-2)
        } else {
            buttonPowerSource = newGuiButton(7, 7, 135, "")
            textboxLampSupplyChannel = newGuiTextField(7+1, 35+1, 135-2)
        }

        textboxLampSupplyChannel.setComment(0, I18N.tr("Specify the lamp supply channel"))
        textboxLampSupplyChannel.setText(render.lampSupplyChannel)

        trackbarAlphaZ = newGuiVerticalTrackBar(151, 7+2, 18, 68-4)
        trackbarAlphaZ.setRange(render.descriptor.alphaZMin.toFloat(), render.descriptor.alphaZMax.toFloat())
        trackbarAlphaZ.setStepIdMax(180)
        trackbarAlphaZ.value = render.alphaZ.toFloat()

        if (render.descriptor.alphaZMax == render.descriptor.alphaZMin) trackbarAlphaZ.visible = false
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)

        if (render.grounded) buttonGrounded.displayString = I18N.tr("Parallel")
        else buttonGrounded.displayString = I18N.tr("Serial")

        if (render.poweredByLampSupply) {
            textboxLampSupplyChannel.visible = true
            buttonPowerSource.displayString = I18N.tr("Powered by lamp supply")

            val lampStack = render.inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
            val cableStack = render.inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID)

            if (cableStack == null) {
                textboxLampSupplyChannel.setComment(1, "§4" + I18N.tr("Cable slot empty"))
            } else if (lampStack == null) {
                textboxLampSupplyChannel.setComment(1, "§4" + I18N.tr("Lamp slot empty"))
            } else if (render.activeLampSupplyConnection) {
                textboxLampSupplyChannel.setComment(1, "§2" + I18N.tr("Connected to ${render.lampSupplyChannel}"))
            } else {
                textboxLampSupplyChannel.setComment(1, "§4" + I18N.tr("${render.lampSupplyChannel} is not in range!"))
            }
        } else {
            textboxLampSupplyChannel.visible = false
            buttonPowerSource.displayString = I18N.tr("Powered by cable")
        }

        trackbarAlphaZ.setComment(0, I18N.tr("Orientation: ${trackbarAlphaZ.value.toInt()}°"))
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)

        when (obj) {
            buttonGrounded -> render.clientSend(TOGGLE_GROUNDED_EVENT.toInt())
            buttonPowerSource -> render.clientSend(TOGGLE_POWER_SOURCE_EVENT.toInt())
            textboxLampSupplyChannel -> render.clientSetString(UPDATE_LAMP_SUPPLY_CHANNEL_EVENT, textboxLampSupplyChannel.text)
            trackbarAlphaZ -> render.clientSetDouble(ADJUST_ALPHA_Z_EVENT, trackbarAlphaZ.value.toDouble())
        }
    }
}