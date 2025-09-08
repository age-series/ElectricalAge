package mods.eln.node.six

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.ghost.GhostGroup
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.Utils.entityLivingHorizontalViewDirection
import mods.eln.misc.UtilsClient.drawIcon
import mods.eln.misc.VoltageLevelColor
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper

open class SixNodeDescriptor : GenericItemBlockUsingDamageDescriptor, IItemRenderer {
    @JvmField
    var ElementClass: Class<*>
    @JvmField
    var RenderClass: Class<*>
    @JvmField
    var voltageLevelColor = VoltageLevelColor.None

    constructor(name: String?, ElementClass: Class<*>, RenderClass: Class<*>) : super(name) {
        this.ElementClass = ElementClass
        this.RenderClass = RenderClass
    }

    constructor(name: String?, ElementClass: Class<*>, RenderClass: Class<*>, iconName: String?) : super(name, iconName) {
        this.ElementClass = ElementClass
        this.RenderClass = RenderClass
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return voltageLevelColor !== VoltageLevelColor.None
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return false
    }

    open fun shouldUseRenderHelperEln(type: ItemRenderType?, item: ItemStack?, helper: ItemRendererHelper?): Boolean {
        return false
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (icon == null) return
        voltageLevelColor.drawIconBackground(type)

        // remove "eln:" to add the full path replace("eln:", "textures/blocks/") + ".png";
        val icon = icon.iconName.substring(4)
        drawIcon(type, ResourceLocation("eln", "textures/blocks/$icon.png"))
    }

    open fun hasVolume(): Boolean {
        return false
    }

    open fun canBePlacedOnSide(player: EntityPlayer?, c: Coordinate?, side: Direction): Boolean {
        return canBePlacedOnSide(player, side)
    }

    open fun canBePlacedOnSide(player: EntityPlayer?, side: Direction): Boolean {
        if (placeDirection != null) {
            for (d in placeDirection!!) {
                if (d === side) return true
            }
            addChatMessage(player!!, tr("You can't place this block at this side"))
            return false
        }
        return true
    }

    var ghostGroup: GhostGroup? = null

    fun hasGhostGroup(): Boolean {
        return ghostGroup != null
    }

    fun getGhostGroup(side: Direction?, front: LRDU?): GhostGroup? {
        return if (ghostGroup == null) null else ghostGroup!!.newRotate(side, front)
    }

    val ghostGroupUuid: Int
        get() = -1

    fun setPlaceDirection(d: Direction) {
        placeDirection = arrayOf(d)
    }

    fun setPlaceDirection(d: Array<Direction>) {
        placeDirection = d
    }

    private var placeDirection: Array<Direction>? = null

    fun checkCanPlace(coord: Coordinate?, direction: Direction, front: LRDU?): String? {
        if (placeDirection != null) {
            var ok = false
            for (d in placeDirection!!) {
                if (d === direction) {
                    ok = true
                    break
                }
            }
            if (!ok) return tr("You can't place this block at this side")
        }
        val ghostGroup = getGhostGroup(direction, front)
        return if (ghostGroup != null && !ghostGroup.canBePloted(coord!!)) tr("Not enough space for this block") else null
    }

    open fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU? {
        return when (side) {
            Direction.YN, Direction.YP -> {
                val viewDirection = entityLivingHorizontalViewDirection(player)
                val front = side.getLRDUGoingTo(viewDirection)
                if (side === Direction.YN) front else front!!.inverse()
            }
            else -> LRDU.Up
        }
    }
}
