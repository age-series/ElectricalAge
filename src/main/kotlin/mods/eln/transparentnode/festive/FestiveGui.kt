package mods.eln.transparentnode.festive

import mods.eln.gui.*
import mods.eln.sixnode.lampsupply.PowerChannelTextboxHelper

class FestiveGui(val render: IFestiveElementRender) : GuiScreenEln() {

    companion object {
        const val UPDATE_LAMP_SUPPLY_CHANNEL_EVENT: Byte = 0
    }

    private lateinit var textboxLampSupplyChannel: GuiTextFieldEln

    override fun newHelper(): GuiHelper {
        return GuiHelperContainer(this, 154, 30, 0, 0)
    }

    override fun initGui() {
        super.initGui()
        textboxLampSupplyChannel = newGuiTextField(8, 8, 138)
        PowerChannelTextboxHelper.initPowerChannelTextbox(textboxLampSupplyChannel, render.lampSupplyChannel)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        PowerChannelTextboxHelper.updatePowerChannelTextboxTooltip(
            textboxLampSupplyChannel,
            render.lampSupplyChannel,
            render.activeLampSupplyConnection
        )
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)
        if (obj === textboxLampSupplyChannel) {
            render.clientSetString(UPDATE_LAMP_SUPPLY_CHANNEL_EVENT, textboxLampSupplyChannel.text)
        }
    }

}