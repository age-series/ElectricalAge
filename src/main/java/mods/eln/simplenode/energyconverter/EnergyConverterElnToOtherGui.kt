package mods.eln.simplenode.energyconverter

import mods.eln.Eln
import mods.eln.gui.GuiButtonEln
import mods.eln.gui.GuiHelper
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiTextFieldEln
import mods.eln.gui.GuiVerticalTrackBar
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N
import kotlin.math.log
import kotlin.math.pow

class EnergyConverterElnToOtherGui(var render: EnergyConverterElnToOtherEntity) : GuiScreenEln() {

    var powerSelector: GuiVerticalTrackBar? = null
    var ic2tier1: GuiButtonEln? = null
    var ic2tier2: GuiButtonEln? = null
    var ic2tier3: GuiButtonEln? = null
    var ic2tier4: GuiButtonEln? = null
    var powerEntry: GuiTextFieldEln? = null

    private val maxPower = Eln.instance.ELN_CONVERTER_MAX_POWER

    var ic2tierList: List<GuiButtonEln>? = null

    override fun initGui() {
        super.initGui()
        powerSelector = newGuiVerticalTrackBar(6, 6 + 2, 20, 82)
        powerSelector!!.setStepIdMax(100)
        powerSelector!!.setEnable(true)
        powerSelector!!.setRange(0f, 1f)
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
        powerEntry!!.setComment(listOf("Select the maximum export power").toTypedArray())
        syncVoltage()
    }

    fun syncVoltage() {
        powerSelector!!.value = selectedPowerToSlider(render.selectorPower)
        powerEntry!!.text = render.selectorPower.toInt().toString()
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

    fun selectedPowerToSlider(selectedPower: Double): Float {
        return log(selectedPower / 100 + 1, (maxPower / 100) + 1).toFloat()
    }

    fun sliderToSelectedPower(slider: Float): Double {
        return 100 * (((maxPower / 100) + 1).pow(slider.toDouble()) - 1)
    }

    override fun guiObjectEvent(guiObject: IGuiObject) {
        super.guiObjectEvent(guiObject)
        when {
            guiObject === powerSelector -> {
                render.selectorPower = sliderToSelectedPower(powerSelector!!.value)
                render.sender.clientSendDouble(NetworkType.SET_POWER.id, render.selectorPower)
            }
            guiObject === powerEntry -> {
                render.selectorPower = powerEntry!!.text.toDoubleOrNull()?: render.selectorPower
                render.sender.clientSendDouble(NetworkType.SET_POWER.id, render.selectorPower)
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
        powerSelector!!.setComment(0, I18N.tr("Input power is limited to %1\$W", render.selectorPower))
    }

    override fun newHelper(): GuiHelper {
        return GuiHelper(this, 82, 115)
    }
}
