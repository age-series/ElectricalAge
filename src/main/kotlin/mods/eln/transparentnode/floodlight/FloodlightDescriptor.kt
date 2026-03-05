package mods.eln.transparentnode.floodlight

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.misc.Utils.entityLivingHorizontalViewDirection
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.util.*

class FloodlightDescriptor(val name: String, val obj: Obj3D, val motorized: Boolean) : TransparentNodeDescriptor(name, FloodlightElement::class.java, FloodlightRender::class.java) {

    private val base: Obj3D.Obj3DPart = obj.getPart("Lamp_Base_Cube.008")
    private val swivel: Obj3D.Obj3DPart = obj.getPart("Lamp_Swivel_Cube.014")
    private val head: Obj3D.Obj3DPart = obj.getPart("Lamp_Head_Cylinder.004")
    private val bulb1off: Obj3D.Obj3DPart = obj.getPart("Lamp1_OFF_Cylinder.003")
    private val bulb2off: Obj3D.Obj3DPart = obj.getPart("Lamp2_OFF_Cylinder.002")
    private val bulb1on: Obj3D.Obj3DPart = obj.getPart("Lamp1_ON_Cylinder.000")
    private val bulb2on: Obj3D.Obj3DPart = obj.getPart("Lamp2_ON_Cylinder.001")

    var placementSide: Direction = Direction.XN

    init {
        voltageLevelColor = VoltageLevelColor.Neutral
    }

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        Collections.addAll(list, *tr("A powerful lamp that specializes in\nthe illumination of large spaces.")!!
            .split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    override fun addRealismContext(list: MutableList<String>?): RealisticEnum {
        return RealisticEnum.REALISTIC
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack())
    }

    override fun shouldUseRenderHelper(type: IItemRenderer.ItemRenderType, item: ItemStack, helper: IItemRenderer.ItemRendererHelper): Boolean {
        return type != IItemRenderer.ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType): Boolean {
        return true
    }

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == IItemRenderer.ItemRenderType.INVENTORY) super.renderItem(type, item, *data)
        else drawItem()
    }

    private fun drawItem() {
        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        base.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        swivel.draw()
        head.draw()
        bulb1off.draw()
        bulb2off.draw()
    }

    fun draw(swivelAngle: Double, headAngle: Double, bulb1: ItemStack?, bulb2: ItemStack?, powered: Boolean) {
        GL11.glTranslated(-0.5, -0.5, 0.5)
        base.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(swivelAngle + 90.0, 0.0, 1.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        swivel.draw()
        GL11.glTranslated(0.5, 0.5, -0.5)
        GL11.glRotated(-headAngle, 1.0, 0.0, 0.0)
        GL11.glTranslated(-0.5, -0.5, 0.5)
        head.draw()

        if (bulb1 != null) {
            if (powered) bulb1on.draw()
            else bulb1off.draw()
        }
        if (bulb2 != null) {
            if (powered) bulb2on.draw()
            else bulb2off.draw()
        }
    }

    override fun mustHaveFloor(): Boolean {
        return false
    }

    override fun getFrontFromPlace(side: Direction, entityLiving: EntityLivingBase?): Direction {
        placementSide = side.inverse
        return when (side) {
            Direction.XN -> side.inverse.down()
            Direction.XP -> side.inverse.down()
            Direction.YN -> entityLivingHorizontalViewDirection(entityLiving!!).inverse
            Direction.YP -> entityLivingHorizontalViewDirection(entityLiving!!).inverse
            Direction.ZN -> side.inverse.down()
            Direction.ZP -> side.inverse.down()
        }
    }

}