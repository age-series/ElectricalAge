package mods.eln.sixnode.electricalhumiditysensor

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11

class ElectricalHumiditySensorDescriptor(name: String, private val obj: Obj3D?) : SixNodeDescriptor(
    name,
    ElectricalHumiditySensorElement::class.java,
    ElectricalHumiditySensorRender::class.java
) {
    private val main = obj?.getPart("main")
    val pinDistance: FloatArray? = main?.let { Utils.getSixNodePinDistance(it) }

    init {
        voltageLevelColor = VoltageLevelColor.SignalVoltage
    }

    fun draw() {
        UtilsClient.disableCulling()
        main?.draw()
        UtilsClient.enableCulling()
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addSignal(newItemStack())
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.addAll(tr("Provides an electrical signal\nfor biome relative humidity.").split("\n"))
        list.addAll(tr("Humidity follows day/night\nand rises during rain or snow.").split("\n"))
        list.add(tr("0%% RH: %1\$V", 0))
        list.add(tr("100%% RH: %1\$V", Utils.plotValue(Eln.SVU)))
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean = true

    override fun shouldUseRenderHelper(
        type: IItemRenderer.ItemRenderType,
        item: ItemStack,
        helper: IItemRenderer.ItemRendererHelper
    ): Boolean = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun shouldUseRenderHelperEln(
        type: IItemRenderer.ItemRenderType?,
        item: ItemStack?,
        helper: IItemRenderer.ItemRendererHelper?
    ): Boolean = type != IItemRenderer.ItemRenderType.INVENTORY

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            GL11.glScalef(2f, 2f, 2f)
            draw()
        }
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU {
        return super.getFrontFromPlace(side, player)!!.right()
    }
}
