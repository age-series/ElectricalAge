package mods.eln.sim

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.Phase
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import mods.eln.debug.DP
import mods.eln.debug.DPType
import mods.eln.sim.mna.RootSystem
import mods.eln.sim.mna.component.Component
import mods.eln.sim.mna.misc.MnaConst
import mods.eln.sim.mna.state.State
import mods.eln.sim.process.destruct.IDestructable
import net.minecraft.client.Minecraft

import java.text.DecimalFormat
import java.util.ArrayList
import java.util.HashSet

class Simulator /* ,IPacketHandler */(var callPeriod: Double, var electricalPeriod: Double, internal var electricalInterSystemOverSampling: Int, var thermalPeriod: Double) {

    var mna: RootSystem

    private val slowProcessList: ArrayList<IProcess>
    private val slowPreProcessList: MutableList<IProcess>
    private val slowPostProcessList: MutableList<IProcess>

    val electricalProcessList: ArrayList<IProcess>

    private val thermalFastProcessList: ArrayList<IProcess>
    private val thermalSlowProcessList: ArrayList<IProcess>
    private val thermalFastConnectionList: ArrayList<ThermalConnection>
    private val thermalSlowConnectionList: ArrayList<ThermalConnection>
    private val thermalFastLoadList: ArrayList<ThermalLoad>
    private val thermalSlowLoadList: ArrayList<ThermalLoad>
    private val destructableSet: MutableSet<IDestructable>

    var isRunning: Boolean = false
        internal set
    internal var nodeCount = 0

    internal var avgTickTime = 0.0
    internal var electricalNsStack: Long = 0
    internal var thermalFastNsStack: Long = 0
    internal var slowNsStack: Long = 0
    internal var thermalSlowNsStack: Long = 0

    internal var timeout = 0.0
    internal var electricalTimeout = 0.0
    internal var thermalTimeout = 0.0

    private var printTimeCounter = 0

    var pleaseCrash = false

    fun getMinimalThermalC(Rs: Double, Rp: Double): Double {
        return thermalPeriod * 3 / (1 / (1 / Rp + 1 / Rs))
    }

    fun checkThermalLoad(thermalRs: Double, thermalRp: Double, thermalC: Double): Boolean {
        if (thermalC < getMinimalThermalC(thermalRs, thermalRp)) {
            DP.println(DPType.MNA, "checkThermalLoad ERROR")
            Minecraft.getMinecraft().shutdown()
        }
        return true
    }

    init {

        FMLCommonHandler.instance().bus().register(this)

        mna = RootSystem(electricalPeriod, electricalInterSystemOverSampling)

        slowProcessList = ArrayList()
        slowPreProcessList = ArrayList()
        slowPostProcessList = ArrayList()

        electricalProcessList = ArrayList()

        thermalFastProcessList = ArrayList()
        thermalSlowProcessList = ArrayList()
        thermalFastConnectionList = ArrayList()
        thermalFastLoadList = ArrayList()
        thermalSlowConnectionList = ArrayList()
        thermalSlowLoadList = ArrayList()
        destructableSet = HashSet()

        isRunning = false
    }

    fun init() {
        nodeCount = 0

        mna = RootSystem(electricalPeriod, electricalInterSystemOverSampling)

        slowProcessList.clear()
        slowPreProcessList.clear()
        slowPostProcessList.clear()

        electricalProcessList.clear()

        thermalFastProcessList.clear()
        thermalSlowProcessList.clear()
        thermalFastConnectionList.clear()
        thermalFastLoadList.clear()
        thermalSlowConnectionList.clear()
        thermalSlowLoadList.clear()
        destructableSet.clear()

        isRunning = true

        DP.println(DPType.MNA, "${MnaConst.noImpedance}")
    }

    fun stop() {
        nodeCount = 0

        slowProcessList.clear()
        slowPreProcessList.clear()
        slowPostProcessList.clear()

        // we used to set the MNA to null here, but I question that being needed here.
        // Makes types stuff more difficult if this is just going to get GC'd anyways

        electricalProcessList.clear()

        thermalFastProcessList.clear()
        thermalSlowProcessList.clear()
        thermalFastConnectionList.clear()
        thermalFastLoadList.clear()
        thermalSlowConnectionList.clear()
        thermalSlowLoadList.clear()
        destructableSet.clear()

        isRunning = false
    }

    fun addElectricalComponent(c: Component?) {
        if (c != null) {
            mna.addComponent(c)
        }
    }

    fun removeElectricalComponent(c: Component?) {
        if (c != null) {
            mna.removeComponent(c)
        }
    }

    fun addThermalConnection(connection: ThermalConnection?) {
        if (connection != null) {
            if (connection.L1.isSlow() == connection.L2.isSlow()) {
                if (connection.L1.isSlow())
                    thermalSlowConnectionList.add(connection)
                else
                    thermalFastConnectionList.add(connection)

            } else {
                DP.println(DPType.MNA, "***** addThermalConnection ERROR ****")
            }
        }
    }

    fun removeThermalConnection(connection: ThermalConnection?) {
        if (connection != null) {
            thermalSlowConnectionList.remove(connection)
            thermalFastConnectionList.remove(connection)
        }
    }

    fun addElectricalLoad(load: State?) {
        if (load != null) {
            mna.addState(load)
        }
    }

    fun removeElectricalLoad(load: State?) {
        if (load != null) {
            mna.removeState(load)
        }
    }

    fun addThermalLoad(load: ThermalLoad?) {
        if (load != null) {
            if (load.isSlow())
                thermalSlowLoadList.add(load)
            else
                thermalFastLoadList.add(load)
        }
    }

    fun removeThermalLoad(load: ThermalLoad?) {
        if (load != null) {
            thermalSlowLoadList.remove(load)
            thermalFastLoadList.remove(load)
        }
    }

    fun addProcess(type: ProcessType, process: IProcess?) {
        if (process == null) return
        when (type) {
            ProcessType.SlowPreProcess -> slowPreProcessList.add(process)
            ProcessType.SlowProcess -> slowProcessList.add(process)
            ProcessType.SlowPostProcess -> slowPostProcessList.add(process)
            ProcessType.ThermalSlowProcess -> thermalSlowProcessList.add(process)
            ProcessType.ThermalFastProcess -> thermalFastProcessList.add(process)
            ProcessType.ElectricalProcess -> electricalProcessList.add(process)
            else -> DP.println(DPType.MNA, "Sim AP: Error: " + process)
        }
    }

    fun removeProcess(type: ProcessType, process: IProcess?) {
        if (process == null) return
        when (type) {
            ProcessType.SlowPreProcess -> slowPreProcessList.remove(process)
            ProcessType.SlowProcess -> slowProcessList.remove(process)
            ProcessType.SlowPostProcess -> slowPostProcessList.remove(process)
            ProcessType.ThermalSlowProcess -> thermalSlowProcessList.remove(process)
            ProcessType.ThermalFastProcess -> thermalFastProcessList.remove(process)
            ProcessType.ElectricalProcess -> electricalProcessList.remove(process)
            else -> DP.println(DPType.MNA, "Sim RP: Error: " + process)
        }
    }

    fun addAllProcess(type: ProcessType, processList: ArrayList<IProcess>?) {
        if (processList == null) return
        when (type) {
            ProcessType.SlowPreProcess -> slowPreProcessList.addAll(processList)
            ProcessType.SlowProcess -> slowProcessList.addAll(processList)
            ProcessType.SlowPostProcess -> slowPostProcessList.addAll(processList)
            ProcessType.ThermalSlowProcess -> thermalSlowProcessList.addAll(processList)
            ProcessType.ThermalFastProcess -> thermalFastProcessList.addAll(processList)
            ProcessType.ElectricalProcess -> electricalProcessList.addAll(processList)
            else -> DP.println(DPType.MNA, "Sim AP: Error: " + processList)
        }
    }

    fun removeAllProcess(type: ProcessType, processList: ArrayList<IProcess>?) {
        if (processList == null) return
        when (type) {
            ProcessType.SlowPreProcess -> slowPreProcessList.removeAll(processList)
            ProcessType.SlowProcess -> slowProcessList.removeAll(processList)
            ProcessType.SlowPostProcess -> slowPostProcessList.removeAll(processList)
            ProcessType.ThermalSlowProcess -> thermalSlowProcessList.removeAll(processList)
            ProcessType.ThermalFastProcess -> thermalFastProcessList.removeAll(processList)
            ProcessType.ElectricalProcess -> electricalProcessList.removeAll(processList)
            else -> DP.println(DPType.MNA, "Sim RP: Error: " + processList)
        }
    }

    fun addAllElectricalConnection(connection: Iterable<ElectricalConnection>?) {
        if (connection != null) {
            for (c in connection) {
                addElectricalComponent(c)
            }
        }
    }

    fun removeAllElectricalConnection(connection: Iterable<ElectricalConnection>?) {
        if (connection != null) {
            for (c in connection) {
                removeElectricalComponent(c)
            }
        }
    }

    fun addAllElectricalComponent(cList: Iterable<Component>?) {
        if (cList != null) {
            for (c in cList) {
                addElectricalComponent(c)
            }
        }
    }

    fun removeAllElectricalComponent(cList: Iterable<Component>?) {
        if (cList != null) {
            for (c in cList) {
                removeElectricalComponent(c)
            }
        }
    }

    fun addAllThermalConnection(connection: Iterable<ThermalConnection>?) {
        if (connection != null) {
            for (c in connection) {
                addThermalConnection(c)
            }
        }
    }

    fun removeAllThermalConnection(connection: Iterable<ThermalConnection>?) {
        if (connection != null) {
            for (c in connection) {
                removeThermalConnection(c)
            }
        }
    }

    fun addAllElectricalLoad(load: Iterable<ElectricalLoad>?) {
        if (load != null) {
            for (l in load) {
                addElectricalLoad(l)
            }
        }
    }

    fun removeAllElectricalLoad(load: Iterable<ElectricalLoad>?) {
        if (load != null) {
            for (l in load) {
                removeElectricalLoad(l)
            }
        }
    }

    fun addAllThermalLoad(load: Iterable<ThermalLoad>?) {
        if (load != null) {
            for (c in load) {
                addThermalLoad(c)
            }
        }
    }

    fun removeAllThermalLoad(load: Iterable<ThermalLoad>?) {
        if (load != null) {
            for (c in load) {
                removeThermalLoad(c)
            }
        }
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != Phase.START) return
        if (pleaseCrash) throw StackOverflowError()
        var stackStart: Long

        val startTime = System.nanoTime()

        for (o in slowPreProcessList.toTypedArray()) {
            o.process(1.0 / 20)
        }

        timeout += callPeriod

        while (timeout > 0) {
            if (timeout < electricalTimeout && timeout < thermalTimeout) {
                thermalTimeout -= timeout
                electricalTimeout -= timeout
                timeout = 0.0
                break
            }

            val dt: Double

            if (electricalTimeout <= thermalTimeout) {
                dt = electricalTimeout
                electricalTimeout += electricalPeriod

                stackStart = System.nanoTime()

                mna.step()
                for (p in electricalProcessList) {
                    p.process(electricalPeriod)
                }

                electricalNsStack += System.nanoTime() - stackStart
            } else {
                dt = thermalTimeout
                thermalTimeout += thermalPeriod

                stackStart = System.nanoTime()
                // / Utils.print("*");

                thermalStep(thermalPeriod, thermalFastConnectionList, thermalFastProcessList, thermalFastLoadList)

                thermalFastNsStack += System.nanoTime() - stackStart
            }
            thermalTimeout -= dt
            electricalTimeout -= dt
            timeout -= dt
        }

        run {
            stackStart = System.nanoTime()
            thermalStep(0.05, thermalSlowConnectionList, thermalSlowProcessList, thermalSlowLoadList)
            thermalSlowNsStack += System.nanoTime() - stackStart
        }

        stackStart = System.nanoTime()

        for (o in slowProcessList.toTypedArray()) {
            o.process(0.05)
        }

        for (d in destructableSet) {
            d.destructImpl()
        }
        destructableSet.clear()

        slowNsStack += System.nanoTime() - stackStart
        avgTickTime += 1.0 / 20 * ((System.nanoTime() - startTime).toInt() / 1000)

        if (++printTimeCounter == 20) {
            printTimeCounter = 0
            electricalNsStack /= 20
            thermalFastNsStack /= 20
            thermalSlowNsStack /= 20
            slowNsStack /= 20

            DP.print(DPType.CONSOLE, "ticks " + DecimalFormat("#").format(avgTickTime.toInt().toLong()) + " us" + "  E " + electricalNsStack / 1000 + "  TF " + thermalFastNsStack / 1000 + "  TS " + thermalSlowNsStack / 1000 + "  S " + slowNsStack / 1000

                    + "    " + mna.subSystemCount + " SS"
                    + "    " + electricalProcessList.size + " EP"
                    + "    " + thermalFastLoadList.size + " TFL"
                    + "    " + thermalFastConnectionList.size + " TFC"
                    + "    " + thermalFastProcessList.size + " TFP"
                    + "    " + thermalSlowLoadList.size + " TSL"
                    + "    " + thermalSlowConnectionList.size + " TSC"
                    + "    " + thermalSlowProcessList.size + " TSP"
                    + "    " + slowProcessList.size + " SP"
            )

            avgTickTime = 0.0

            electricalNsStack = 0
            thermalFastNsStack = 0
            slowNsStack = 0
            thermalSlowNsStack = 0
        }

        for (o in slowPostProcessList) {
            o.process(1 / 20.0)
        }
    }

    fun isRegistred(load: ElectricalLoad): Boolean {
        return mna.isRegistred(load)
    }

    internal fun thermalStep(dt: Double, connectionList: Iterable<ThermalConnection>, processList: Iterable<IProcess>?, loadList: Iterable<ThermalLoad>) {
        for (c in connectionList) {
            val i: Double = (c.L2.Tc - c.L1.Tc) / (c.L2.Rs + c.L1.Rs)
            c.L1.PcTemp += i
            c.L2.PcTemp -= i

            c.L1.PrsTemp += Math.abs(i)
            c.L2.PrsTemp += Math.abs(i)
        }
        if (processList != null) {
            for (process in processList) {
                process.process(dt)
            }
        }
        for (load in loadList) {
            load.PcTemp -= load.Tc / load.Rp

            load.Tc += load.PcTemp * dt / load.C

            load.Pc = load.PcTemp
            load.Prs = load.PrsTemp
            load.Psp = load.PspTemp
            load.PcTemp = 0.0
            load.PrsTemp = 0.0
            load.PspTemp = 0.0
        }
    }
}

enum class ProcessType {
    SlowProcess,
    SlowPreProcess,
    SlowPostProcess,
    ThermalSlowProcess,
    ThermalFastProcess,
    ElectricalProcess
}
