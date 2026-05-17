package mods.eln.sixnode.lampsupply

import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import java.util.*

class LampSupplyDescriptor(name: String, val obj: Obj3D, @JvmField val range: Int, val nominalVoltage: Double) :
    SixNodeDescriptor(name, LampSupplyElement::class.java, LampSupplyRender::class.java) {

    private val base = obj.getPart("base")
    private val window = obj.getPart("window")

    companion object {
        const val CHANNEL_COUNT = 3
    }

    init {
        setDefaultIcon("lampsupply")
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack(1))
    }

    fun draw(openFactor: Float) {
        if (base != null) base.draw()
        UtilsClient.disableCulling()
        //UtilsClient.disableDepthTest()
        UtilsClient.enableBlend()
        obj.bindTexture("Glass.png")
        val rotYaw = Minecraft.getMinecraft().thePlayer.rotationYaw / 360f
        val rotPitch = Minecraft.getMinecraft().thePlayer.rotationPitch / 180f
        val pos = (Minecraft.getMinecraft().thePlayer.posX + Minecraft.getMinecraft().thePlayer.posZ).toFloat() / 64f
        if (window != null) {
            val windowOpenAngle = window.getFloat("windowOpenAngle")
            window.draw((1f - openFactor) * windowOpenAngle, 0f, 0f, 1f, rotYaw + pos + (openFactor * 0.5f), rotPitch * 0.65f)
        }
        UtilsClient.disableBlend()
        //UtilsClient.enableDepthTest()
        UtilsClient.enableCulling()
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun shouldUseRenderHelperEln(type: IItemRenderer.ItemRenderType?, item: ItemStack?, helper: IItemRenderer.ItemRendererHelper?): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) super.renderItem(type, item, *data)
        else draw(1f)
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(I18N.tr("Supplies power to nearby lamps."))
        list.add(I18N.tr($$"Nominal voltage: %1$V", Utils.plotValue(nominalVoltage)))
        list.add(I18N.tr("Capable of operating 3 light channels."))
        Collections.addAll(
            list,
            *I18N.tr("Supports control from a wireless signal\nchannel for each lighting channel.").split(
                "\n".toRegex()
            ).dropLastWhile { it.isEmpty() }.toTypedArray()
        )
    }

    override fun addRealismContext(list: MutableList<String>): RealisticEnum {
        super.addRealismContext(list)
        list.add(I18N.tr("Most homes have a circuit breaker panel for lights."))
        list.add(I18N.tr("The wireless power aspect is pretending there are wires in the walls."))
        list.add(I18N.tr("Wireless control signals are totally possible."))
        return RealisticEnum.REALISTIC
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU? {
        return super.getFrontFromPlace(side, player)?.inverse()
    }

}