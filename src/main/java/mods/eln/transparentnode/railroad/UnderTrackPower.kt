package mods.eln.transparentnode.railroad

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.nbt.NbtElectricalLoad
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class UnderTrackPowerDescriptor(name: String, private val obj3D: Obj3D?): TransparentNodeDescriptor(name, UnderTrackPowerElement::class.java,
    UnderTrackPowerRender::class.java
) {
    override fun checkCanPlace(coord: Coordinate?, front: Direction): String? {
        // TODO: Require support from at least one side, except top
        return null
    }

    val thirdRailRender = CableRenderDescriptor("eln", "sprites/cable.png",
        0.95f, 0.95f)

    fun drawBase() {
        obj3D?.draw("OverheadGantry")
    }
}

class UnderTrackPowerElement(node: TransparentNode?,
                           transparentNodeDescriptor: TransparentNodeDescriptor
): GenericRailroadPowerElement(node, transparentNodeDescriptor) {

    val electricalLoad = NbtElectricalLoad("electricalLoad")

    override fun initialize() {
        Eln.applySmallRs(electricalLoad)
        electricalLoad.setCanBeSimplifiedByLine(true)
        electricalLoadList.add(electricalLoad)
        connect()
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return NodeBase.maskElectricalPower
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad {
        return electricalLoad
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            node!!.lrduCubeMask.getTranslate(front.down()).serialize(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun getWaila(): Map<String, String> {
        val info: MutableMap<String, String> = HashMap()
        info[I18N.tr("Voltage")] = Utils.plotVolt("", electricalLoad.u)
        if (Eln.wailaEasyMode) {
            info[I18N.tr("Power")] = Utils.plotPower("", electricalLoad.i * electricalLoad.u)
        }
        val ss = electricalLoad.subSystem
        if (ss != null) {
            val subSystemSize = electricalLoad.subSystem.component.size
            var textColor = ""
            textColor = if (subSystemSize <= 8) {
                "§a"
            } else if (subSystemSize <= 15) {
                "§6"
            } else {
                "§c"
            }
            info[I18N.tr("Subsystem Matrix Size")] = textColor + subSystemSize
        } else {
            info[I18N.tr("Subsystem Matrix Size")] = "§cnull SubSystem"
        }
        return info
    }
}

class UnderTrackPowerRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor):
    TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    val desc = transparentNodeDescriptor as UnderTrackPowerDescriptor
    private val eConn = LRDUMask()

    init {
        val x = tileEntity.xCoord
        val y = tileEntity.yCoord
        val z = tileEntity.zCoord
    }

    override fun draw() {
        desc.drawBase()
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            eConn.deserialize(stream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
