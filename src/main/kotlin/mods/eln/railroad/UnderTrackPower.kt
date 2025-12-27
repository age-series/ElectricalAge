package mods.eln.railroad

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.i18n.I18N
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.railroad.PoweredMinecartSimulationSingleton.poweredMinecartSimulationData
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.misc.MnaConst
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
        info[I18N.tr("Voltage")] = Utils.plotVolt("", electricalLoad.voltage)
        if (Eln.wailaEasyMode) {
            info[I18N.tr("Power")] = Utils.plotPower("", electricalLoad.current * electricalLoad.voltage)
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
            resistor.resistance = MnaConst.highImpedance
            resistorLoad.serialResistance = MnaConst.noImpedance
            connection.resistance = MnaConst.noImpedance
            electricalLoadList.add(resistorLoad)
            electricalComponentList.add(connection)
            electricalComponentList.add(resistor)
            val rrsp = RailroadResistorSlowProcess(this, cart, 0.05)
            Eln.simulator.addSlowProcess(rrsp)
            poweredMinecartSimulationData.add(PoweredMinecartSimulationData(cart, resistor, resistorLoad, connection, rrsp, this))
            this.electricalLoad.subSystem.invalidate()
            this.needPublish()
        }
    }

    override fun deregisterCart(cart: EntityElectricMinecart) {
        val search = poweredMinecartSimulationData.filter { it.minecart == cart }
        if (search.isNotEmpty()) {
            search.forEach {
                if (it.owningElement == this) {
                    electricalComponentList.remove(it.resistor)
                    electricalComponentList.remove(it.electricalConnection)
                    electricalLoadList.remove(it.resistorElectricalLoad)
                    it.resistor.breakConnection()
                    it.electricalConnection.breakConnection()
                    this.electricalLoad.subSystem.invalidate()
                    Eln.simulator.removeSlowProcess(it.slowProcess)
                    poweredMinecartSimulationData.remove(it)
                    this.needPublish()
                }
            }
        }
    }
}

class UnderTrackPowerRender(tileEntity: TransparentNodeEntity, transparentNodeDescriptor: TransparentNodeDescriptor):
    TransparentNodeElementRender(tileEntity, transparentNodeDescriptor) {

    val desc = transparentNodeDescriptor as UnderTrackPowerDescriptor
    private val eConn = LRDUMask()

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
