package mods.eln.transparentnode.battery

import mods.eln.gui.GuiHelperContainer
import mods.eln.gui.GuiScreenEln
import mods.eln.gui.GuiVerticalProgressBar
import mods.eln.gui.HelperStdContainer
import mods.eln.gui.IGuiObject
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils

class BatteryGuiDraw(var render: BatteryRender) : GuiScreenEln() {
    var energyBar: GuiVerticalProgressBar? = null

    override fun initGui() {
        super.initGui()
        super.helper.ySize = 50
        energyBar = newGuiVerticalProgressBar(167 - 16, 8, 16, 35)
        energyBar!!.setColor(0.2f, 0.5f, 0.8f)
    }

    override fun guiObjectEvent(`object`: IGuiObject) {
        super.guiObjectEvent(`object`)
    }

    override fun preDraw(f: Float, x: Int, y: Int) {
        super.preDraw(f, x, y)
        energyBar!!.setValue((render.energy / (render.descriptor.electricalStdEnergy * render.life)).toFloat())
        energyBar!!.setComment(0, Utils.plotPercent(tr("Energy: "), energyBar!!.value).replace(" ", ""))
    }

    override fun postDraw(f: Float, x: Int, y: Int) {
        super.postDraw(f, x, y)
        val str1: String
        var str2: String? = ""
        val p = render.power.toDouble()
        val energyMiss = render.descriptor.electricalStdEnergy * render.life - render.energy
        when {
            Math.abs(p) < 5 -> {
                str1 = tr("No charge")
            }
            p > 0 -> {
                str1 = tr("Discharge")
                str2 = Utils.plotTime("", render.energy / p)
            }
            energyMiss > 0 -> {
                str1 = tr("Charge")
                str2 = Utils.plotTime("", -energyMiss / p)
            }
            else -> {
                str1 = tr("Charged")
            }
        }
        val xDelta = 70
        if (render.descriptor.lifeEnable) {
            drawString(8, 8, tr("Life:"))
            drawString(xDelta, 8, Utils.plotPercent("", render.life.toDouble()))
        }
        drawString(8, 17, tr("Energy:"))
        drawString(xDelta, 17,
            Utils.plotValue(render.energy.toDouble(), "J/") + Utils.plotValue(render.descriptor.electricalStdEnergy * render.life, "J"))
        if (render.power >= 0) drawString(8, 26, tr("Power out:")) else drawString(8, 26, tr("Power in:"))
        drawString(xDelta, 26, Utils.plotValue(Math.abs(render.power).toDouble(), "W/") + Utils.plotValue(render.descriptor.electricalStdP, "W"))
        drawString(8, 35, str1)
        drawString(xDelta, 35, str2)
    }

    override fun newHelper(): GuiHelperContainer {
        return HelperStdContainer(this)
    }
}
