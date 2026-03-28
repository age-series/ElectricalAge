package mods.eln.sixnode.electricalcable

import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.UtilsClient.bindTexture
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElement
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

class MoltenMetalPileDescriptor(
    name: String,
    @JvmField val material: UtilityCableMaterial,
    @JvmField val render: CableRenderDescriptor
) : SixNodeDescriptor(name, MoltenMetalPileElement::class.java, MoltenMetalPileRender::class.java) {

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("A puddle of molten %1$ from an overheated cable.", material.label.lowercase()))
    }
}

class MoltenMetalPileElement(
    sixNode: SixNode?,
    side: Direction?,
    descriptor: SixNodeDescriptor
) : SixNodeElement(sixNode!!, side!!, descriptor) {

    override fun getElectricalLoad(lrdu: LRDU, mask: Int) = null

    override fun getThermalLoad(lrdu: LRDU, mask: Int) = null

    override fun getConnectionMask(lrdu: LRDU) = 0

    override fun multiMeterString() = ""

    override fun thermoMeterString() = ""

    override fun getWaila() = mapOf(tr("State") to tr("Molten metal"))
}

class MoltenMetalPileRender(
    tileEntity: SixNodeEntity?,
    side: Direction?,
    descriptor: SixNodeDescriptor
) : SixNodeElementRender(tileEntity!!, side!!, descriptor) {

    private val descriptor = descriptor as MoltenMetalPileDescriptor

    override fun drawCableAuto() = false

    override fun glListEnable() = true

    override fun glListDraw() {
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side))
    }

    override fun getCableRender(lrdu: LRDU) = descriptor.render

    override fun draw() {
        Minecraft.getMinecraft().mcProfiler.startSection("MoltenMetalPile")
        when (descriptor.material) {
            UtilityCableMaterial.COPPER -> GL11.glColor3f(0.92f, 0.38f, 0.10f)
            UtilityCableMaterial.ALUMINUM -> GL11.glColor3f(0.83f, 0.85f, 0.88f)
        }
        bindTexture(descriptor.render.cableTexture)
        glListCall()
        GL11.glColor3f(1f, 1f, 1f)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }
}
