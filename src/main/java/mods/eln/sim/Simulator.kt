package mods.eln.sim

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent
import mods.eln.metrics.SimpleMetric
import mods.eln.metrics.UnitTypes
import mods.eln.misc.Utils.println
import mods.eln.sim.electrical.ElectricalConnection
import mods.eln.sim.electrical.ElectricalLoad
import mods.eln.sim.electrical.mna.RootSystem
import mods.eln.sim.electrical.mna.component.Component
import mods.eln.sim.electrical.mna.state.State
import mods.eln.sim.thermal.ThermalConnection
import mods.eln.sim.thermal.ThermalLoad
import mods.eln.sim.watchdogs.IDestructible
import java.util.*
import kotlin.math.abs

class Simulator (val tickPeriod: Double, val electricalPeriod: Double, val electricalInterSystemOverSampling: Int, val thermalPeriod: Double) {

    @JvmField
    var mna: RootSystem?
    private val slowProcessList: ArrayList<IProcess>
    private val slowPreProcessList: MutableList<IProcess>
    private val slowPostProcessList: MutableList<IProcess>
    private val electricalProcessList: ArrayList<IProcess>
    private val thermalFastProcessList: ArrayList<IProcess>
    private val thermalSlowProcessList: ArrayList<IProcess>
    private val thermalFastConnectionList: ArrayList<ThermalConnection>
    private val thermalSlowConnectionList: ArrayList<ThermalConnection>
    private val thermalFastLoadList: ArrayList<ThermalLoad>
    private val thermalSlowLoadList: ArrayList<ThermalLoad>
    private val destructibleSet: MutableSet<IDestructible>
    private var run: Boolean
    private var nodeCount = 0
    private var tickTimeNs = 0.0
    private var electricalNsStack: Long = 0
    private var thermalFastNsStack: Long = 0
    private var slowNsStack: Long = 0
    private var slowPreNsStack: Long = 0
    private var slowPostNsStack: Long = 0
    private var thermalSlowNsStack: Long = 0
    private var metricsSendNsStack: Long = 0
    private var timeout = 0.0
    private var electricalTimeout = 0.0
    private var thermalTimeout = 0.0
    private var printTimeCounter = 0

    /*

    Metrics Stuff!

     */
    private val sim = "sim"
    private val metricAverageTickTime = SimpleMetric("tick_ns", sim, "Tick time", UnitTypes.NANOSECONDS)
    private val metricElectricalNanoseconds = SimpleMetric("electric_sim_ns", sim, "Electric sim time", UnitTypes.NANOSECONDS)
    private val metricThermalFastNanoseconds = SimpleMetric("thermal_fast_ns", sim, "Thermal fast sim time", UnitTypes.NANOSECONDS)
    private val metricThermalSlowNanoseconds = SimpleMetric("thermal_slow_ns", sim, "Thermal slow sim time", UnitTypes.NANOSECONDS)
    private val metricSlowProcessNanoseconds = SimpleMetric("slow_process_ns", sim, "Slow Process time", UnitTypes.NANOSECONDS)
    private val metricSlowPreProcessNanoseconds = SimpleMetric("pre_slow_process_ns", sim, "Pre Slow Process time", UnitTypes.NANOSECONDS)
    private val metricSlowPostProcessNanoseconds = SimpleMetric("post_slow_process_ns", sim, "Post Slow Process time", UnitTypes.NANOSECONDS)
    private val metricSendMetricNanoseconds = SimpleMetric("metric_send_ns", sim, "Metric collection time", UnitTypes.NANOSECONDS)
    private val metricSubSystemCount = SimpleMetric("subsystem_count", sim, "Number of subsystems", UnitTypes.NO_UNITS)
    private val metricElectricalProcessCount = SimpleMetric("electric_process_count", sim, "Electrical Process count", UnitTypes.NO_UNITS)
    private val metricThermalFastLoadCount = SimpleMetric("thermal_fast_load_count", sim, "Thermal Fast Load count", UnitTypes.NO_UNITS)
    private val metricThermalFastConnectionCount = SimpleMetric("thermal_fast_connection_count", sim, "Thermal Fast Connection count", UnitTypes.NO_UNITS)
    private val metricThermalFastProcessCount = SimpleMetric("thermal_fast_process_count", sim, "Thermal Fast Process count", UnitTypes.NO_UNITS)
    private val metricThermalSlowLoadCount = SimpleMetric("thermal_slow_load_count", sim, "Thermal Slow Load count", UnitTypes.NO_UNITS)
    private val metricThermalSlowConnectionCount = SimpleMetric("thermal_slow_connection_count", sim, "Thermal Slow Connection count", UnitTypes.NO_UNITS)
    private val metricThermalSlowProcessCount = SimpleMetric("thermal_slow_process_count", sim, "Thermal Slow Process count", UnitTypes.NO_UNITS)
    private val metricSlowProcessListCount = SimpleMetric("slow_process_count", sim, "Slow Process Count", UnitTypes.NO_UNITS)
    private val metricSlowPreProcessCount = SimpleMetric("slow_pre_process_count", sim, "Slow Pre Process Count", UnitTypes.NO_UNITS)
    private val metricSlowPostProcessCount = SimpleMetric("slow_post_process_count", sim, "Slow Post Process Count", UnitTypes.NO_UNITS)

    fun getMinimalThermalC(Rs: Double, Rp: Double): Double {
        return thermalPeriod * 3 / (1 / (1 / Rp + 1 / Rs))
    }

    fun checkThermalLoad(thermalRs: Double, thermalRp: Double, thermalC: Double): Boolean {
        check(thermalC >= getMinimalThermalC(thermalRs, thermalRp)) { "Thermal load outside safe limits." }
        return true
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
        destructibleSet.clear()
        run = true
    }

    fun stop() {
        nodeCount = 0
        mna = null
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
        destructibleSet.clear()
        run = false
    }

    @SubscribeEvent
    fun tick(event: ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        val startTime = System.nanoTime()
        var stackStart: Long

        stackStart = System.nanoTime()
        slowPreProcessList.forEach{it.process(tickPeriod)}
        slowPreNsStack += System.nanoTime() - stackStart

        timeout += tickPeriod
        while (timeout > 0) {
            if (timeout < electricalTimeout && timeout < thermalTimeout) {
                thermalTimeout -= timeout
                electricalTimeout -= timeout
                timeout = 0.0
                break
            }
            var dt: Double
            if (electricalTimeout <= thermalTimeout) {
                dt = electricalTimeout
                electricalTimeout += electricalPeriod
                stackStart = System.nanoTime()
                mna!!.step()
                electricalProcessList.forEach { it.process(electricalPeriod)}
                electricalNsStack += System.nanoTime() - stackStart
            } else {
                dt = thermalTimeout
                thermalTimeout += thermalPeriod
                stackStart = System.nanoTime()
                thermalStep(thermalPeriod, thermalFastConnectionList, thermalFastProcessList, thermalFastLoadList)
                thermalFastNsStack += System.nanoTime() - stackStart
            }
            thermalTimeout -= dt
            electricalTimeout -= dt
            timeout -= dt
        }


        stackStart = System.nanoTime()
        thermalStep(tickPeriod, thermalSlowConnectionList, thermalSlowProcessList, thermalSlowLoadList)
        thermalSlowNsStack += System.nanoTime() - stackStart

        stackStart = System.nanoTime()
        slowProcessList.forEach{it.process(tickPeriod)}
        destructibleSet.forEach{it.destructImpl()}
        destructibleSet.clear()
        slowNsStack += System.nanoTime() - stackStart

        stackStart = System.nanoTime()
        slowPostProcessList.forEach { it.process(tickPeriod) }
        slowPostNsStack += System.nanoTime() - stackStart

        tickTimeNs += System.nanoTime() - startTime

        if (++printTimeCounter == 20) {
            printTimeCounter = 0
            // This is because I don't want to see this in my messages all the time, put to True to enable MNA prints.
            // I find the metrics to be much better anyways since you can graph them with a local Prometheus+Graphana instance easily.
            @Suppress("ConstantConditionIf")
            if (false) {
                val simStatistics = listOf(
                    "ticks ${tickTimeNs / 1000} us",
                    "E ${electricalNsStack / 1000} us",
                    "TF ${thermalFastNsStack / 1000} us",
                    "TS ${thermalSlowNsStack / 1000} us",
                    "S ${slowNsStack / 1000} us",
                    "BS ${slowPreNsStack / 1000} us",
                    "AS ${slowPostNsStack / 1000} us",
                    "${mna!!.subSystemCount} SS",
                    "${electricalProcessList.size} EP",
                    "${thermalFastLoadList.size} TFL",
                    "${thermalFastConnectionList.size} TFC",
                    "${thermalFastProcessList.size} TFP",
                    "${thermalSlowLoadList.size} TSL",
                    "${thermalSlowConnectionList.size} TSC",
                    "${thermalSlowProcessList.size} TSP",
                    "${slowProcessList.size} SP"
                ).joinToString(" ")
                println(simStatistics)
            }
        }

        val lastMetricsSendNsStack = metricsSendNsStack
        metricsSendNsStack = 0

        stackStart = System.nanoTime()
        metricAverageTickTime.putMetric(tickTimeNs)
        metricElectricalNanoseconds.putMetric(electricalNsStack.toDouble())
        metricThermalFastNanoseconds.putMetric(thermalFastNsStack.toDouble())
        metricThermalSlowNanoseconds.putMetric(thermalSlowNsStack.toDouble())
        metricSlowProcessNanoseconds.putMetric(slowNsStack.toDouble())
        metricSlowPreProcessNanoseconds.putMetric(slowPreNsStack.toDouble())
        metricSlowPostProcessNanoseconds.putMetric(slowPostNsStack.toDouble())
        metricSubSystemCount.putMetric(mna!!.subSystemCount.toDouble())
        metricElectricalProcessCount.putMetric(electricalProcessList.size.toDouble())
        metricThermalFastLoadCount.putMetric(thermalFastLoadList.size.toDouble())
        metricThermalFastConnectionCount.putMetric(thermalFastConnectionList.size.toDouble())
        metricThermalFastProcessCount.putMetric(thermalFastProcessList.size.toDouble())
        metricThermalSlowLoadCount.putMetric(thermalSlowLoadList.size.toDouble())
        metricThermalSlowConnectionCount.putMetric(thermalSlowConnectionList.size.toDouble())
        metricThermalSlowProcessCount.putMetric(thermalSlowProcessList.size.toDouble())
        metricSlowProcessListCount.putMetric(slowProcessList.size.toDouble())
        metricSlowPreProcessCount.putMetric(slowPreProcessList.size.toDouble())
        metricSlowPostProcessCount.putMetric(slowPostProcessList.size.toDouble())
        metricSendMetricNanoseconds.putMetric(lastMetricsSendNsStack.toDouble())
        metricsSendNsStack += System.nanoTime() - stackStart

        tickTimeNs = 0.0
        electricalNsStack = 0
        thermalFastNsStack = 0
        slowNsStack = 0
        slowPreNsStack = 0
        slowPostNsStack = 0
        thermalSlowNsStack = 0
    }

    fun isRegistered(load: ElectricalLoad?): Boolean {
        return mna!!.isRegistered(load)
    }

    private fun thermalStep(dt: Double, connectionList: Iterable<ThermalConnection>, processList: Iterable<IProcess>, loadList: Iterable<ThermalLoad>) {
        // Compute heat transferred over each thermal connection:
        for (c in connectionList) {
            val i: Double = (c.L2.Tc - c.L1.Tc) / (c.L2.Rs + c.L1.Rs)
            c.L1.PcTemp += i
            c.L2.PcTemp -= i
            c.L1.PrsTemp += abs(i)
            c.L2.PrsTemp += abs(i)
        }

        processList.forEach{ it.process(dt) }

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
        destructibleSet = HashSet()
        run = false
    }

    /*

    Everything here down is just accessor methods to the various parts of the sim.
    TODO: Honestly, I'd love to condense this to other parts of the game putting things into an un-nullable list.
    It would reduce most of the logic here to handling thermal connections.

     */

    fun addElectricalComponent(c: Component) {
        mna!!.addComponent(c)
    }

    fun removeElectricalComponent(c: Component) {
        mna!!.removeComponent(c)
    }

    fun addThermalConnection(connection: ThermalConnection) {
        if (connection.L1.isSlow == connection.L2.isSlow) {
            if (connection.L1.isSlow) thermalSlowConnectionList.add(connection) else thermalFastConnectionList.add(connection)
        } else {
            println("Error - cannot connect a fast thermal load to a slow thermal load.")
            /*
             * For more detail on why this is a bad thing -
             *
             * The fast thermal loads iterate very quickly so that they can iterate at the speed of the electric sim.
             * This prevents issues with the thermal system looking at the first tick of the electrical system and
             * thinking that there was over a MW of thermal power when really it was a very small amount of time.
             * The side effect is that the fast process will move power much faster than the slow process sim,
             * and this can cause issues.
             *
             * Theoretically, there is probably some good way to handle this that allows things to go fast only
             * when it is needed, but effort. I tried once and it basically broke the whole game.
             *
             * - jrddunbr
             */
        }
    }

    fun removeThermalConnection(connection: ThermalConnection) {
        thermalSlowConnectionList.remove(connection)
        thermalFastConnectionList.remove(connection)
    }

    fun addElectricalLoad(load: State) {
        mna!!.addState(load)
    }

    fun removeElectricalLoad(load: State) {
        mna!!.removeState(load)
    }

    fun addThermalLoad(load: ThermalLoad) {
        if (load.isSlow) thermalSlowLoadList.add(load) else thermalFastLoadList.add(load)
    }

    fun removeThermalLoad(load: ThermalLoad) {
        thermalSlowLoadList.remove(load)
        thermalFastLoadList.remove(load)
    }

    fun addSlowProcess(process: IProcess) {
        slowProcessList.add(process)
    }

    fun removeSlowProcess(process: IProcess) {
        slowProcessList.remove(process)
    }

    fun addSlowPreProcess(process: IProcess) {
        slowPreProcessList.add(process)
    }

    fun removeSlowPreProcess(process: IProcess) {
        slowPreProcessList.remove(process)
    }

    fun addSlowPostProcess(process: IProcess) {
        slowPostProcessList.add(process)
    }

    fun removeSlowPostProcess(process: IProcess) {
        slowPostProcessList.remove(process)
    }

    fun addElectricalProcess(process: IProcess) {
        electricalProcessList.add(process)
    }

    fun removeElectricalProcess(process: IProcess) {
        electricalProcessList.remove(process)
    }

    fun addThermalFastProcess(process: IProcess) {
        thermalFastProcessList.add(process)
    }

    fun removeThermalFastProcess(process: IProcess) {
        thermalFastProcessList.remove(process)
    }

    fun addThermalSlowProcess(process: IProcess) {
        thermalSlowProcessList.add(process)
    }

    fun removeThermalSlowProcess(process: IProcess) {
        thermalSlowProcessList.remove(process)
    }

    fun addAllElectricalConnection(connection: Iterable<ElectricalConnection>) {
        for (c in connection) {
            addElectricalComponent(c)
        }
    }

    fun removeAllElectricalConnection(connection: Iterable<ElectricalConnection>) {
        for (c in connection) {
            removeElectricalComponent(c)
        }
    }

    fun addAllElectricalComponent(cList: Iterable<Component>) {
        for (c in cList) {
            addElectricalComponent(c)
        }
    }

    fun removeAllElectricalComponent(cList: Iterable<Component>) {
        for (c in cList) {
            removeElectricalComponent(c)
        }
    }

    fun addAllThermalConnection(connection: Iterable<ThermalConnection>) {
        for (c in connection) {
            addThermalConnection(c)
        }
    }

    fun removeAllThermalConnection(connection: Iterable<ThermalConnection>) {
        for (c in connection) {
            removeThermalConnection(c)
        }
    }

    fun addAllElectricalLoad(load: Iterable<ElectricalLoad>) {
        for (l in load) {
            addElectricalLoad(l)
        }
    }

    fun removeAllElectricalLoad(load: Iterable<ElectricalLoad>) {
        for (l in load) {
            removeElectricalLoad(l)
        }
    }

    fun addAllThermalLoad(load: Iterable<ThermalLoad>) {
        for (c in load) {
            addThermalLoad(c)
        }
    }

    fun removeAllThermalLoad(load: Iterable<ThermalLoad>) {
        for (c in load) {
            removeThermalLoad(c)
        }
    }

    fun addAllSlowProcess(process: List<IProcess>) {
        slowProcessList.addAll(process)
    }

    fun removeAllSlowProcess(process: List<IProcess>) {
        slowProcessList.removeAll(process)
    }

    fun addAllElectricalProcess(process: List<IProcess>) {
        electricalProcessList.addAll(process)
    }

    fun removeAllElectricalProcess(process: List<IProcess>) {
        electricalProcessList.removeAll(process)
    }

    fun addAllThermalFastProcess(process: List<IProcess>) {
        thermalFastProcessList.addAll(process)
    }

    fun removeAllThermalFastProcess(process: List<IProcess>) {
        thermalFastProcessList.removeAll(process)
    }

    fun addAllThermalSlowProcess(process: List<IProcess>) {
        thermalSlowProcessList.addAll(process)
    }

    fun removeAllThermalSlowProcess(process: List<IProcess>) {
        thermalSlowProcessList.removeAll(process)
    }
}
