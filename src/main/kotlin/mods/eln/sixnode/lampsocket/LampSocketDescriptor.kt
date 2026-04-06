package mods.eln.sixnode.lampsocket

import mods.eln.i18n.I18N
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.misc.RealisticEnum
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.sixnode.lampsocket.objrender.ILampSocketObjRender
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11
import java.util.Collections
import kotlin.text.split

// TODO: Revisit integration of this file with the rest of the six-node lamp socket code.
class LampSocketDescriptor(val itemName: String, val renderType: ILampSocketObjRender, val paintable: Boolean,
                           val range: Int, val rotatable: Boolean, val acceptedLampTypes: Array<BoilerplateLampData>) :
    SixNodeDescriptor(itemName, LampSocketElement::class.java, LampSocketRender::class.java) {

    companion object {
        const val MIN_LIGHT_ON_VALUE = 8
    }

    var acceptedLampTypesString: String = ""

    val minRotationAngle = if (rotatable) -90.0 else 0.0
    val maxRotationAngle = if (rotatable) 90.0 else 0.0

    var initialRotateDeg = 0.0
    var renderIconInHand = false

    var renderSideCables = true

    var cameraOpt = true
    var extendedRenderBounds = false

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
        return !renderIconInHand && type != ItemRenderType.INVENTORY
    }

    override fun shouldUseRenderHelperEln(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return !renderIconInHand && type != ItemRenderType.INVENTORY
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == ItemRenderType.INVENTORY || renderIconInHand) {
            super.renderItem(type, item, *data)
        } else {
            GL11.glScaled(1.25, 1.25, 1.25)
            renderType.draw(this, type, 0.0)
        }
    }

    override fun hasVolume(): Boolean {
        return hasGhostGroup()
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        if (range != 0) list.add(I18N.tr("Spot range: $range blocks"))
        if (rotatable) list.add(I18N.tr("Angle: ${minRotationAngle.toInt()}° to ${maxRotationAngle.toInt()}°"))
        list.add(I18N.tr("Accepted lamp types: $acceptedLampTypesString"))
    }

    override fun addRealismContext(list: MutableList<String>): RealisticEnum {
        super.addRealismContext(list)

        Collections.addAll(list, *I18N.tr("Wireless mode of lights is intended \nto pretend wires are in the walls.")!!
            .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

        return RealisticEnum.REALISTIC
    }

}