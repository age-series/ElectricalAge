package mods.eln.sixnode.mqttsignal

import mods.eln.misc.Obj3D
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

class MqttSignalControllerDescriptor(name: String?, private val obj: Obj3D?) :
    SixNodeDescriptor(name, MqttSignalControllerElement::class.java, MqttSignalControllerRender::class.java) {

    private val main: Obj3DPart?
    private val door: Obj3DPart?
    private val leds: Array<Obj3DPart?>
    private val alphaOff: Float
    val pinDistance: FloatArray?

    init {
        var doorAlpha = 0f
        val ledParts = Array(8) { obj?.getPart("led$it") }
        if (obj != null) {
            main = obj.getPart("main")
            door = obj.getPart("door")
            if (door != null) {
                doorAlpha = door.getFloat("alphaOff")
            }
        } else {
            main = null
            door = null
        }
        leds = ledParts
        alphaOff = doorAlpha
        pinDistance = main?.let { Utils.getSixNodePinDistance(it) }
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addSignal(newItemStack())
    }

    fun draw(open: Float, ledState: BooleanArray) {
        main?.draw()
        door?.draw((1f - open) * alphaOff, 0f, 1f, 0f)
        leds.forEachIndexed { index, led ->
            led ?: return@forEachIndexed
            if (index < ledState.size && ledState[index]) {
                if ((index and 3) == 0) {
                    GL11.glColor3f(0.8f, 0f, 0f)
                } else {
                    GL11.glColor3f(0f, 0.8f, 0f)
                }
                UtilsClient.drawLight(led)
            } else {
                GL11.glColor3f(0.3f, 0.3f, 0.3f)
                led.draw()
            }
        }
        GL11.glColor3f(1f, 1f, 1f)
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return true
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun shouldUseRenderHelperEln(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            GL11.glTranslatef(-0.3f, -0.1f, 0f)
            GL11.glRotatef(90f, 1f, 0f, 0f)
            draw(0.7f, DEFAULT_LED_PATTERN)
        }
    }

    companion object {
        private val DEFAULT_LED_PATTERN = booleanArrayOf(true, false, true, false, true, true, true, false)
    }
}
