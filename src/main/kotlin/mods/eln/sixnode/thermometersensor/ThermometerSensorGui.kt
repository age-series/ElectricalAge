package mods.eln.sixnode.thermometersensor

import mods.eln.gui.GuiContainerEln
import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.HelperStdContainer
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import net.minecraft.client.gui.GuiButton
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import java.text.NumberFormat

class ThermometerSensorGui(player: EntityPlayer, inventory: IInventory, private val render: ThermometerSensorRender) :
    GuiContainerEln(ThermometerSensorContainer(player, inventory)) {

    private lateinit var validate: GuiButton
    private lateinit var lowValue: GuiTextFieldEln
    private lateinit var highValue: GuiTextFieldEln

    override fun initGui() {
        super.initGui()

        val x = 0
        val y = 0
        validate = newGuiButton(x + 8 + 50 + 4 + 50 + 4 - 26, y + (166 - 84) / 2 - 8, 50, tr("Validate"))

        lowValue = newGuiTextField(x + 8 + 50 + 4 - 26, y + (166 - 84) / 2 + 3, 50)
        lowValue.setText(render.lowValue)
        lowValue.setComment(tr("Measured temperature\ncorresponding\nto 0% output").split("\n").toTypedArray())

        highValue = newGuiTextField(x + 8 + 50 + 4 - 26, y + (166 - 84) / 2 - 12, 50)
        highValue.setText(render.highValue)
        highValue.setComment(tr("Measured temperature\ncorresponding\nto 100% output").split("\n").toTypedArray())
    }

    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
        if (`object` == validate) {
            try {
                val low = NumberFormat.getInstance().parse(lowValue.text).toFloat()
                val high = NumberFormat.getInstance().parse(highValue.text).toFloat()
                render.clientSetFloat(ThermometerSensorElement.setValueId.toInt(), low, high)
            } catch (_: Exception) {
            }
        }
    }

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }
}
