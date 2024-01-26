package mods.eln.sixnode

import mods.eln.cable.CableRender
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.six.*
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

class ConduitCableDescriptor(
    name: String,
    val render: CableRenderDescriptor
): SixNodeDescriptor(name, ConduitCableElement::class.java, ConduitCableRender::class.java) {

    override fun addInformation(itemStack: ItemStack, entityPlayer: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("A conduit to run your cables through"))
    }

    override fun addRealismContext(list: MutableList<String>): RealisticEnum {
        list.add(tr("Has some caveats:"))
        list.add(tr("  * Thermal Sim is disabled in the conduit"))
        return RealisticEnum.REALISTIC
    }

    fun getNodeMask(): Int {
        return NodeBase.maskConduit
    }

    fun bindCableTexture() {
        render.bindCableTexture()
    }
}


class ConduitCableElement(
    sixNode: SixNode?,
    side: Direction?,
    descriptor: SixNodeDescriptor
): SixNodeElement(
    sixNode!!, side!!, descriptor) {

    val descriptor = descriptor as ConduitCableDescriptor

    override fun getElectricalLoad(lrdu: LRDU, mask: Int) = null

    override fun getThermalLoad(lrdu: LRDU, mask: Int) = null

    override fun getConnectionMask(lrdu: LRDU) = descriptor.getNodeMask()

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[tr("Contained Cables")] = "0"
        return info
    }

    override fun multiMeterString() = ""

    override fun thermoMeterString() = ""

}

class ConduitCableRender(
    tileEntity: SixNodeEntity?,
    side: Direction?,
    descriptor: SixNodeDescriptor
) : SixNodeElementRender(tileEntity!!, side!!, descriptor) {

    val descriptor = descriptor as ConduitCableDescriptor

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor {
        return descriptor.render
    }

    override fun glListEnable() = true

    override fun glListDraw() {
        CableRender.drawCable(descriptor.render, connectedSide, CableRender.connectionType(this, side))
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side))
    }

    override fun draw() {
        Minecraft.getMinecraft().mcProfiler.startSection("ECable")
        GL11.glColor3f(1f, 1f, 1f)
        UtilsClient.bindTexture(descriptor.render.cableTexture)
        glListCall()
        GL11.glColor3f(1f, 1f, 1f)
        Minecraft.getMinecraft().mcProfiler.endSection()
    }

    override fun drawCableAuto() = false
}