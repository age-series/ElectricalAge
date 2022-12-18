package mods.eln.simplenode.energyconverter

import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.GuiVerticalTrackBar
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import kotlin.math.log
import kotlin.math.pow

class EnergyConverterElnToOtherGui(var render: EnergyConverterElnToOtherEntity) : GuiScreenEln() {

    var resistanceSelector: GuiVerticalTrackBar? = null
    var ic2tier1: GuiButtonEln? = null
    var ic2tier2: GuiButtonEln? = null
    var ic2tier3: GuiButtonEln? = null
    var ic2tier4: GuiButtonEln? = null
    var powerEntry: GuiTextFieldEln? = null

    private val maxResistance = 100_000.0

    var ic2tierList: List<GuiButtonEln>? = null

    override fun initGui() {
        super.initGui()
        resistanceSelector = newGuiVerticalTrackBar(6, 6 + 2, 20, 82)
        resistanceSelector!!.setStepIdMax(100)
        resistanceSelector!!.setEnable(true)
        resistanceSelector!!.setRange(0f, 1f)
        ic2tier1 = newGuiButton(30, 6, 47,"IC2 T1")
        ic2tier2 = newGuiButton(30, 28, 47,"IC2 T2")
        ic2tier3 = newGuiButton(30, 50, 47,"IC2 T3")
        ic2tier4 = newGuiButton(30, 72, 47,"IC2 T4")
        ic2tierList = listOfNotNull(ic2tier1, ic2tier2, ic2tier3, ic2tier4)
        ic2tier1!!.enabled = true
        ic2tier1!!.setComment(0, "Set output to 32 EU/t max")
        ic2tier2!!.enabled = true
        ic2tier2!!.setComment(0, "Set output to 128 EU/t max")
        ic2tier3!!.enabled = true
        ic2tier3!!.setComment(0, "Set output to 512 EU/t max")
        ic2tier4!!.enabled = true
        ic2tier4!!.setComment(0, "Set output to 2048 EU/t max")
        powerEntry = newGuiTextField(6, 96, 70)
        powerEntry!!.enabled
        powerEntry!!.setComment(listOf("Select the resistance").toTypedArray())
        syncVoltage()
    }

    fun syncVoltage() {
        resistanceSelector!!.value = selectedResistanceToSlider(render.selectedResistance)
        powerEntry!!.text = render.selectedResistance.toInt().toString()
        ic2tierList!!.forEachIndexed {
            index, button ->
            val tier = index + 1
            button.displayString = if (tier == render.ic2tier) {
                "[IC2 T$tier]"
            } else {
                "IC2 T$tier"
            }
        }
        render.hasChanges = false
    }

    fun selectedResistanceToSlider(selectedResistance: Double): Float {
        return log(selectedResistance / 100 + 1, (maxResistance / 100) + 1).toFloat()
    }

    fun sliderToSelectedResistance(slider: Float): Double {
        return 100 * (((maxResistance / 100) + 1).pow(slider.toDouble()) - 1)
    }

    override fun guiObjectEvent(guiObject: IGuiObject) {
        super.guiObjectEvent(guiObject)
        when {
            guiObject === resistanceSelector -> {
                render.selectedResistance = sliderToSelectedResistance(resistanceSelector!!.value)
                render.sender.clientSendDouble(NetworkType.SET_OHMS.id, render.selectedResistance)
            }
            guiObject === powerEntry -> {
                render.selectedResistance = powerEntry!!.text.toDoubleOrNull()?: render.selectedResistance
                render.sender.clientSendDouble(NetworkType.SET_OHMS.id, render.selectedResistance)
            }
            guiObject === ic2tier1 ->
                render.sender.clientSendInt(NetworkType.SET_IC2_TIER.id, 1)
            guiObject === ic2tier2 ->
                render.sender.clientSendInt(NetworkType.SET_IC2_TIER.id, 2)
            guiObject === ic2tier3 ->
                render.sender.clientSendInt(NetworkType.SET_IC2_TIER.id, 3)
            guiObject === ic2tier4 ->
                render.sender.clientSendInt(NetworkType.SET_IC2_TIER.id, 4)
        }
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        if (render.hasChanges) syncVoltage()
        resistanceSelector!!.setComment(0, Utils.plotOhm(render.selectedResistance))
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 82, 115)
    }
}
