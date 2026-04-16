package mods.eln.sixnode.lampsocket

import mods.eln.i18n.I18N
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.misc.RealisticEnum
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.sixnode.lampsocket.objrender.ILampSocketObjRender
import mods.eln.sixnode.lampsocket.objrender.LampSocketSuspendedObjRender
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11
import java.util.Collections
import kotlin.text.split

class LampSocketDescriptor(itemName: String, val renderType: ILampSocketObjRender, val range: Int, val acceptedLampTypes: Array<BoilerplateLampData>) :
    SixNodeDescriptor(itemName, LampSocketElement::class.java, LampSocketRender::class.java) {

    var paintable: Boolean = false
    var enableProjectionRotation: Boolean = false
    var initialRenderAngleOffset = 0.0
    var renderSideCables = true
    var extendedRenderBounds = false

    private var acceptedLampTypesString: String = ""

    init {
        voltageLevelColor = VoltageLevelColor.Neutral

        for (lampData in acceptedLampTypes) {
            acceptedLampTypesString += lampData.lampType
            if (acceptedLampTypes.indexOf(lampData) < (acceptedLampTypes.size - 1)) acceptedLampTypesString += ", "
        }
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack())
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
            GL11.glRotated(initialRenderAngleOffset, 1.0, 0.0, 0.0)
            GL11.glScaled(1.25, 1.25, 1.25)
            if (this.hasGhostGroup()) GL11.glRotated(90.0, 0.0, 0.0, 1.0)
            else if (this.renderType is LampSocketSuspendedObjRender) GL11.glRotated(-90.0, 0.0, 0.0, 1.0)
            renderType.draw(this, type, 0.0)
        }
    }

    override fun hasVolume(): Boolean {
        return hasGhostGroup()
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        if (range != 0) list.add(I18N.tr("Spot range: %1$ blocks", range))
        if (enableProjectionRotation) list.add(I18N.tr("Angle: %1$\u00B0 to %2$\u00B0", LampSocketGui.MIN_ROTATION_ANGLE.toInt(), LampSocketGui.MAX_ROTATION_ANGLE.toInt()))
        list.add(I18N.tr("Accepted lamp types: %1$", I18N.tr(acceptedLampTypesString)))
    }

    override fun addRealismContext(list: MutableList<String>): RealisticEnum {
        super.addRealismContext(list)

        Collections.addAll(list, *I18N.tr("Wireless mode of lights is intended \nto pretend wires are in the walls.")!!
            .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

        return RealisticEnum.REALISTIC
    }

}