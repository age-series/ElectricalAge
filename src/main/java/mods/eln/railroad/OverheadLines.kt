package mods.eln.railroad

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.misc.LRDUMask
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils.getBlock
import mods.eln.misc.Utils.plotPower
import mods.eln.misc.Utils.plotVolt
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.railroad.PoweredMinecartSimulationSingleton.poweredMinecartSimulationData
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class OverheadLinesDescriptor(name: String, private val obj3D: Obj3D?): TransparentNodeDescriptor(name, OverheadLinesElement::class.java,
    OverheadLinesRender::class.java
) {
    override fun mustHaveFloor() = false
    override fun mustHaveWall() = false
    override fun mustHaveWallFrontInverse() = false
    override fun mustHaveCeiling() = false

    val centenaryCableRender = CableRenderDescriptor("eln", "sprites/cable.png",
        0.95f, 0.95f)

    fun drawBase() {
        obj3D?.draw("OverheadGantry")
    }
}

class OverheadLinesElement(node: TransparentNode?,
                           transparentNodeDescriptor: TransparentNodeDescriptor
): TransparentNodeElement(node, transparentNodeDescriptor), RailroadPowerInterface {

    val electricalLoad = NbtElectricalLoad("electricalLoad")

    init {
        Eln.applySmallRs(electricalLoad)
        electricalLoad.setCanBeSimplifiedByLine(true)
        electricalLoadList.add(electricalLoad)
    }

    override fun initialize() {
        connect()
    }

    private val actualSides = listOf(Direction.XN, Direction.ZN, Direction.XP, Direction.ZP)

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return if (side in actualSides && (lrdu == LRDU.Down)) {
            NodeBase.maskElectricalPower
        } else {
            0
        }
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return if (side in actualSides && (lrdu == LRDU.Down)) {
            electricalLoad
        } else {
            null
        }
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
        info[I18N.tr("Voltage")] = plotVolt("", electricalLoad.u)
        if (Eln.wailaEasyMode) {
            info[I18N.tr("Power")] = plotPower("", electricalLoad.i * electricalLoad.u)
        }
        val ss = electricalLoad.subSystem
        if (ss != null) {
            val subSystemSize = electricalLoad.subSystem.component.size
            val textColor: String = if (subSystemSize <= 8) {
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

    class MinecartResistor: Resistor()

    override fun registerCart(cart: EntityElectricMinecart) {
        if (cart !in poweredMinecartSimulationData.map { it.minecart }) {
            val resistor = MinecartResistor()
            val resistorLoad = ElectricalLoad()
            resistorLoad.setAsPrivate()
            val connection = ElectricalConnection(electricalLoad, resistorLoad)
            resistor.connectTo(resistorLoad, null)
            resistor.r = MnaConst.highImpedance
            resistorLoad.rs = MnaConst.noImpedance
            connection.r = MnaConst.noImpedance
            Eln.simulator.addElectricalLoad(resistorLoad)
            Eln.simulator.addElectricalComponent(connection)
            Eln.simulator.addElectricalComponent(resistor)
            val rrsp = RailroadResistorSlowProcess(this, cart, 0.05)
            Eln.simulator.addSlowProcess(rrsp)
            poweredMinecartSimulationData.add(PoweredMinecartSimulationData(cart, resistor, resistorLoad, connection, rrsp, this))
            this.needPublish()
        }
    }

    override fun deregisterCart(cart: EntityElectricMinecart) {
        val search = poweredMinecartSimulationData.filter { it.minecart == cart }
        if (search.isNotEmpty()) {
            search.forEach {
                if (it.owningElement == this) {
                    Eln.simulator.removeElectricalComponent(it.resistor)
                    Eln.simulator.removeElectricalComponent(it.electricalConnection)
                    Eln.simulator.removeElectricalLoad(it.resistorElectricalLoad)
                    it.resistor.breakConnection()
                    it.electricalConnection.breakConnection()
                    Eln.simulator.removeSlowProcess(it.slowProcess)
                    poweredMinecartSimulationData.remove(it)
                    this.needPublish()
                }
            }
        }
    }
}

class OverheadLinesRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor):
    TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    val desc = transparentNodeDescriptor as OverheadLinesDescriptor
    private val eConn = LRDUMask()

    private val boundedSides = mutableListOf<Coordinate>()

    init {
        val x = tileEntity.xCoord
        val y = tileEntity.yCoord
        val z = tileEntity.zCoord
        boundedSides.add(Coordinate(x + 1, y, z, 0))
        boundedSides.add(Coordinate(x -1, y, z, 0))
        boundedSides.add(Coordinate(x, y + 1, z, 0))
        boundedSides.add(Coordinate(x, y - 1, z, 0))
        boundedSides.add(Coordinate(x, y, z + 1, 0))
    }

    private fun hasBlockAnySideNotBottom(): Boolean {
        return boundedSides.any {
            getBlock(tileEntity.worldObj, it.x.toDouble(), it.y.toDouble(),it.z.toDouble()).isOpaqueCube
        }
    }

    override fun draw() {
        val locFront = front
        if (locFront != null) {
            drawCable(locFront.down(), desc.centenaryCableRender, eConn, null, true)
        }

        if (hasBlockAnySideNotBottom()) {
            desc.drawBase()
        }
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
