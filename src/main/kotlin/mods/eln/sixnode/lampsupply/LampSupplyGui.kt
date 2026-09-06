package mods.eln.sixnode.lampsupply

import mods.eln.gui.*
import mods.eln.i18n.I18N
import net.minecraft.entity.player.EntityPlayer
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class LampSupplyGui(player: EntityPlayer, val render: LampSupplyRender) :
    GuiContainerEln(LampSupplyContainer(player, render.inventory)) {

    inner class GuiAggregatorButton(x: Int, y: Int, width: Int, height: Int, str: String, val channel: Int, val buttonID: Int) :
        GuiButtonEln(x, y, width, height, str) {

        override fun onMouseClicked() {
            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)
                render.preparePacketForServer(stream)
                stream.writeByte(SET_SELECTED_AGGREGATOR_EVENT.toInt())
                stream.writeInt(channel)
                stream.writeInt(buttonID)
                render.sendPacketToServer(bos)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            super.onMouseClicked()
        }

        override fun idraw(x: Int, y: Int, f: Float) {
            enabled = render.localEntries[channel].aggregator != buttonID
            super.idraw(x, y, f)
        }

    }

    companion object {
        const val SET_POWER_NAME_EVENT: Byte = 0
        const val SET_WIRELESS_NAME_EVENT: Byte = 1
        const val SET_SELECTED_AGGREGATOR_EVENT: Byte = 2
    }

    private val powerChannelMap = mutableMapOf<GuiTextFieldEln, Int>()
    private val wirelessChannelMap = mutableMapOf<GuiTextFieldEln, Int>()

    override fun newHelper(): GuiHelperContainer {
        return GuiHelperContainer(this, 220, 205, 8, 125)
    }

    override fun initGui() {
        super.initGui()

        val w = 68
        val h = 20

        var x = 6
        var y = 6

        // TODO: Tweak positions of all GUI elements and clean up this for() loop a little
        for (idx in 0..<LampSupplyDescriptor.CHANNEL_COUNT) {
            val powerChannelField = newGuiTextField(x, y, 101)
            x += (powerChannelField.width + 12)
            powerChannelField.setText(render.localEntries[idx].powerChannel)
            powerChannelField.setComment(0, I18N.tr("Power channel name"))
            powerChannelMap[powerChannelField] = idx

            val wirelessChannelField = newGuiTextField(x, y, 101)
            x += (wirelessChannelField.width + 12)
            wirelessChannelField.setText(render.localEntries[idx].wirelessChannel)
            wirelessChannelField.setComment(0, I18N.tr("Wireless channel name"))
            wirelessChannelMap[wirelessChannelField] = idx

            x = 6
            y += (wirelessChannelField.height + 2)

            val buttonBigger = GuiAggregatorButton(x, y, w, h, I18N.tr("Biggest"), idx, 0)
            x += (w + 2)
            val buttonSmaller = GuiAggregatorButton(x, y, w, h, I18N.tr("Smallest"), idx, 1)
            x += (w + 2)
            val buttonToggle = GuiAggregatorButton(x, y, w, h, I18N.tr("Toggle"), idx, 2)
            x += (w + 2)

            add(buttonBigger)
            add(buttonSmaller)
            add(buttonToggle)

            buttonBigger.setHelper(helper)
            buttonSmaller.setHelper(helper)
            buttonToggle.setHelper(helper)

            val comment1 = I18N.tr("Uses the biggest\nvalue on the channel.")
                .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val comment2 = I18N.tr("Uses the smallest\nvalue on the channel.")
                .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val comment3 = I18N.tr("Toggles the output each time\nan emitter's value rises.\nUseful to allow multiple buttons\nto control the same light.")
                .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            var lineNumber = 0
            for (line in comment1) buttonBigger.setComment(lineNumber++, line)
            lineNumber = 0
            for (line in comment2) buttonSmaller.setComment(lineNumber++, line)
            lineNumber = 0
            for (line in comment3) buttonToggle.setComment(lineNumber++, line)

            x = 6
            y += (buttonToggle.height + 6)
        }
    }

    override fun guiObjectEvent(obj: IGuiObject) {
        if (powerChannelMap.containsKey(obj)) {
            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)
                render.preparePacketForServer(stream)
                stream.writeByte(SET_POWER_NAME_EVENT.toInt())
                stream.writeInt(powerChannelMap[obj]!!)
                stream.writeUTF((obj as GuiTextFieldEln).text)
                render.sendPacketToServer(bos)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (wirelessChannelMap.containsKey(obj)) {
            try {
                val bos = ByteArrayOutputStream()
                val stream = DataOutputStream(bos)
                render.preparePacketForServer(stream)
                stream.writeByte(SET_WIRELESS_NAME_EVENT.toInt())
                stream.writeInt(wirelessChannelMap[obj]!!)
                stream.writeUTF((obj as GuiTextFieldEln).text)
                render.sendPacketToServer(bos)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        super.guiObjectEvent(obj)
    }

}