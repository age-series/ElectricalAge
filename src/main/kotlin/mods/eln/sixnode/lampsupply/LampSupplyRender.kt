package mods.eln.sixnode.lampsupply

import mods.eln.cable.CableRenderDescriptor
import mods.eln.cable.CableRenderType
import mods.eln.misc.*
import mods.eln.node.six.SixNodeDescriptor
import mods.eln.node.six.SixNodeElementInventory
import mods.eln.node.six.SixNodeElementRender
import mods.eln.node.six.SixNodeEntity
import mods.eln.sixnode.genericcable.GenericCableDescriptor
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import java.io.DataInputStream
import java.io.IOException

class LampSupplyRender(tileEntity: SixNodeEntity, side: Direction, sixNodeDescriptor: SixNodeDescriptor) :
    SixNodeElementRender(tileEntity, side, sixNodeDescriptor) {

    override val inventory = SixNodeElementInventory(1, 64, this, LampSupplyContainer.REQUIRED_CABLE_LENGTH)
    private val descriptor = sixNodeDescriptor as LampSupplyDescriptor

    private var cableRender: CableRenderDescriptor? = null
    private val coordinate = Coordinate(tileEntity)
    private val interpolator = PhysicalInterpolator(0.4f, 8.0f, 0.9f, 0.2f)

    val entries: MutableList<LampSupplyElement.Entry> = mutableListOf()

    init {
        for (idx in 0..<LampSupplyDescriptor.CHANNEL_COUNT) {
            entries.add(LampSupplyElement.Entry("", "", 2))
        }
    }

    override fun draw() {
        super.draw()
        val pinDistances = floatArrayOf(4.98f, 4.98f, 5.98f, 5.98f)

        if (side.isY) {
            drawPowerPin(front?.rotate4PinDistances(pinDistances))
            front?.glRotateOnX()
        } else {
            drawPowerPin(pinDistances)
            LRDU.Down.glRotateOnX()
        }

        descriptor.draw(interpolator.get())
    }

    override fun refresh(deltaT: Float) {
        if (!Utils.isPlayerAround(tileEntity.getWorldObj(), coordinate.getAxisAlignedBB(0))) interpolator.target = 0f
        else interpolator.target = 1f

        interpolator.step(deltaT)
    }

    override fun getCableRender(lrdu: LRDU): CableRenderDescriptor? {
        return cableRender
    }

    override fun newGuiDraw(side: Direction, player: EntityPlayer): GuiScreen {
        return LampSupplyGui(this, player, inventory)
    }

    override fun publishUnserialize(stream: DataInputStream) {
        super.publishUnserialize(stream)

        try {
            for (e in entries) {
                e.powerChannel = stream.readUTF()
                e.wirelessChannel = stream.readUTF()
                e.aggregator = stream.readInt()
            }
            val cableStack = Utils.unserialiseItemStack(stream)
            cableRender = (Utils.getItemObject(cableStack) as? GenericCableDescriptor)?.render
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun newConnectionType(connectionType: CableRenderType?) {
        for (idx in 0..3) {
            connectionType?.startAt[idx] = 5f / 16f
        }
    }

}