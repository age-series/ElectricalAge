package mods.eln.sixnode.mqttmeter

import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.IGuiObject
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import mods.eln.i18n.I18N.tr

class MqttEnergyMeterGui(
    player: EntityPlayer,
    inventory: IInventory,
    private val render: MqttEnergyMeterRender
) : GuiContainerEln(MqttEnergyMeterContainer(player, inventory)) {

    private lateinit var meterNameField: GuiTextFieldEln
    private lateinit var serverNameField: GuiTextFieldEln
    private lateinit var meterIdField: GuiTextFieldEln
    private lateinit var toggleButton: GuiButtonEln

    protected override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 238, 8, 156)
    }

    override fun initGui() {
        super.initGui()
        var x = 6
        var y = 6

        meterNameField = newGuiTextField(x, y + 4, 160).apply {
            text = render.meterName
            setGuiObserver(this@MqttEnergyMeterGui)
            setComment(0, tr("Meter Name"))
        }
        y += 24

        serverNameField = newGuiTextField(x, y + 4, 160).apply {
            text = render.serverName
            setGuiObserver(this@MqttEnergyMeterGui)
            setComment(0, tr("MQTT server"))
        }
        y += 24

        meterIdField = newGuiTextField(x, y + 4, 160).apply {
            text = render.meterId
            setEnabled(false)
            setComment(0, tr("Meter ID (Read Only)"))
        }
        y += 24

        toggleButton = newGuiButton(x, y, 120, "")
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        super.guiObjectEvent(obj)
        when (obj) {
            toggleButton -> render.clientSend(MqttEnergyMeterElement.CLIENT_TOGGLE_STATE.toInt())
            meterNameField -> render.clientSetString(MqttEnergyMeterElement.CLIENT_SET_NAME, meterNameField.text)
            serverNameField -> render.clientSetString(MqttEnergyMeterElement.CLIENT_SET_SERVER, serverNameField.text)
        }
    }

    override fun preDraw(f: Float, mouseX: Int, mouseY: Int) {
        super.preDraw(f, mouseX, mouseY)
        toggleButton.displayString = if (render.meterEnabled) tr("Turn off") else tr("Turn on")
        if (!meterNameField.isFocused) {
            meterNameField.text = render.meterName
        }
        if (!serverNameField.isFocused) {
            serverNameField.text = render.serverName
        }
        meterIdField.text = render.meterId
        serverNameField.setTextColor(if (render.isServerKnown()) 0x00FF00 else 0x00FF0000)
    }
}
