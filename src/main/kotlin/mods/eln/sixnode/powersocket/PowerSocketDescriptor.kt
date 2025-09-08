package mods.eln.sixnode.powersocket

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils.setGlColorFromDye
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.wiki.Data
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

class PowerSocketDescriptor(subID: Int, name: String, obj: Obj3D) :
    SixNodeDescriptor(name, PowerSocketElement::class.java, PowerSocketRender::class.java) {
    private var base: Obj3DPart? = null
    private var socket: Obj3DPart? = null

    init {
        base = obj.getPart("SocketBase")
        when (subID) {
            1 -> {
                socket = obj.getPart("Socket50V") // Type J socket - 10 amps specification
                voltageLevelColor = VoltageLevelColor.Neutral
            }

            2 -> {
                socket = obj.getPart("Socket200V") // Type E socket - 16 amps specification
                voltageLevelColor = VoltageLevelColor.Neutral
            }
            else -> socket = null
        }
    }

    override fun setParent(item: Item, damage: Int) {
        super.setParent(item, damage)
        Data.addLight(newItemStack(1))
    }

    @JvmOverloads
    fun draw(color: Int = 0) {
        if (base != null) base!!.draw()
        if (socket != null) {
            setGlColorFromDye(color, 0.7f, 0.3f)
            socket!!.draw()
            GL11.glColor3f(1f, 1f, 1f)
        }
    }

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun handleRenderType(item: ItemStack, type: ItemRenderType): Boolean {
        return true
    }

    override fun shouldUseRenderHelperEln(
        type: ItemRenderType?,
        item: ItemStack?,
        helper: ItemRendererHelper?
    ): Boolean {
        return type != ItemRenderType.INVENTORY
    }

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, *data)
        } else {
            draw()
        }
    }

    override fun addInformation(
        itemStack: ItemStack?,
        entityPlayer: EntityPlayer?,
        list: MutableList<String>?,
        par4: Boolean
    ) {
        super.addInformation(itemStack, entityPlayer, list, par4)

        list?.addAll(tr("Supplies any device\nplugged in with energy.").split("\n".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray())
    }

    override fun addRealismContext(list: MutableList<String?>): RealisticEnum {
        super.addRealismContext(list)
        list.add(tr("Homes have power sockets. These are not them."))
        return RealisticEnum.UNREALISTIC
    }

    override fun getFrontFromPlace(side: Direction, player: EntityPlayer): LRDU? {
        return LRDU.Down
    }
}
