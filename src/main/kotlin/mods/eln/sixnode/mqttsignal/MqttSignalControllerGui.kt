package mods.eln.sixnode.mqttsignal

import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import mods.eln.mqtt.SignalPort
import mods.eln.mqtt.SignalPortMode
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory

class MqttSignalControllerGui(
    player: EntityPlayer,
    inventory: IInventory,
    private val render: MqttSignalControllerRender
) : GuiContainerEln(MqttSignalControllerContainer(player, inventory)) {

    private lateinit var nameField: GuiTextFieldEln
    private lateinit var serverField: GuiTextFieldEln
    private lateinit var idField: GuiTextFieldEln
    private val portButtons = LinkedHashMap<SignalPort, GuiButtonEln>()
    private val signalPorts = SignalPort.values()

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 176, 238, 8, 156)
    }

    override fun initGui() {
        super.initGui()
        var x = 6
        var y = 6

        nameField = newGuiTextField(x, y + 4, 160).apply {
            text = render.controllerName
            setGuiObserver(this@MqttSignalControllerGui)
            setComment(0, tr("Signal Controller Name"))
        }
        y += 24

        serverField = newGuiTextField(x, y + 4, 160).apply {
            text = render.serverName
            setGuiObserver(this@MqttSignalControllerGui)
            setComment(0, tr("MQTT server"))
        }
        y += 24

        idField = newGuiTextField(x, y + 4, 160).apply {
            text = render.controllerId
            setEnabled(false)
            setComment(0, tr("Controller ID (Read Only)"))
        }
        y += 24

        signalPorts.forEachIndexed { index, port ->
            val buttonX = x + (index % 2) * 82
            val buttonY = y + (index / 2) * 28
            val button = newGuiButton(buttonX, buttonY, 80, "")
            button.setComment(0, tooltipForMode(render.getPortMode(port)))
            portButtons[port] = button
        }
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        when (obj) {
            nameField -> render.clientSetString(MqttSignalControllerElement.CLIENT_SET_NAME, nameField.text)
            serverField -> render.clientSetString(MqttSignalControllerElement.CLIENT_SET_SERVER, serverField.text)
            else -> {
                portButtons.entries.firstOrNull { it.value == obj }?.let { entry ->
                    val next = render.getPortMode(entry.key).next()
                    render.clientSetInt(
                        MqttSignalControllerElement.CLIENT_SET_PORT_MODE,
                        packPortMode(entry.key, next)
                    )
                }
            }
        }
    }

    override fun preDraw(f: Float, mouseX: Int, mouseY: Int) {
        super.preDraw(f, mouseX, mouseY)
        if (!nameField.isFocused) {
            nameField.text = render.controllerName
        }
        if (!serverField.isFocused) {
            serverField.text = render.serverName
        }
        idField.text = render.controllerId
        serverField.setTextColor(if (render.isServerKnown()) 0x00FF00 else 0x00FF0000)

        portButtons.forEach { (port, button) ->
            val mode = render.getPortMode(port)
            val colorPrefix = colorFor(port)
            button.displayString = "$colorPrefix${port.label}: ${modeLabel(mode)}"
            button.setComment(0, tooltipForMode(mode))
        }
    }

    private fun tooltipForMode(mode: SignalPortMode): String {
        return when (mode) {
            SignalPortMode.DISABLED -> tr("Disabled - ignored by MQTT")
            SignalPortMode.READ -> tr("Read - publish voltage only")
            SignalPortMode.WRITE -> tr("Write - driven by MQTT")
        }
    }

    private fun modeLabel(mode: SignalPortMode): String = when (mode) {
        SignalPortMode.DISABLED -> tr("Disabled")
        SignalPortMode.READ -> tr("Read")
        SignalPortMode.WRITE -> tr("Write")
    }

    private fun colorFor(port: SignalPort): String = when (port) {
        SignalPort.A -> "\u00A7c"
        SignalPort.B -> "\u00A7a"
        SignalPort.C -> "\u00A79"
        SignalPort.D -> ""
    }

    private fun packPortMode(port: SignalPort, mode: SignalPortMode): Int {
        return (mode.ordinal shl 8) or port.ordinal
    }
}
