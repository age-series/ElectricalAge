package mods.eln.sixnode.powersocket

import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.IGuiObject
import mods.eln.sixnode.lampsupply.PowerChannelTextboxHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class PowerSocketGui(
    private val render: PowerSocketRender,
    @Suppress("UNUSED_PARAMETER") player: EntityPlayer?,
    @Suppress("UNUSED_PARAMETER") inventory: IInventory?
) : GuiScreenEln() {

    private lateinit var device: GuiTextFieldEln

    override fun initGui() {
        super.initGui()
        device = newGuiTextField(8, 8, 138)
        PowerChannelTextboxHelper.initPowerChannelTextbox(device, render.channel)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        PowerChannelTextboxHelper.updatePowerChannelTextboxTooltip(device, render.channel, render.activeLampSupplyConnection)
    }

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 154, 30, 0, 0)
    }

    override fun guiObjectEvent(`object`: IGuiObject) {
        if (`object` === device) {
            render.clientSetString(PowerSocketElement.SET_CHANNEL_EVENT, device.text?: "")
        }
        super.guiObjectEvent(`object`)
    }
}
