package mods.eln.entity

import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeElement
import mods.eln.sim.ElectricalConnection
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ITimeRemoverObserver
import mods.eln.sim.TimeRemover
import mods.eln.sim.mna.component.Resistor
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
import mods.eln.misc.LRDU
import net.minecraft.entity.ai.EntityAIBase
import net.minecraft.util.DamageSource
import java.util.Random
import kotlin.math.pow

class ReplicatorCableAI(private val entity: ReplicatorEntity) : EntityAIBase(), ITimeRemoverObserver {
    var cableCoordinate: Coordinate? = null
    private val rand = Random()
    private val lookingPerUpdate = 20

    private val load = ElectricalLoad()
    private var cableLoad: ElectricalLoad? = null
    private val resistorLoad = Resistor(load, null)
    private var connection: ElectricalConnection? = null
    private val timeRemover = TimeRemover(this)

    private var moveTimeOut = 0.0
    private val moveTimeOutReset = 20.0
    private var resetTimeout = 0.0
    private val resetTimeoutReset = 120.0

    private var preSimCheck: PreSimCheck? = null

    init {
        Eln.instance.highVoltageCableDescriptor.applyTo(load)
        load.serialResistance = load.serialResistance * 10
        mutexBits = 1
    }

    override fun shouldExecute(): Boolean {
        val nodes = NodeManager.instance!!.nodes
        if (nodes.isEmpty()) return false

        repeat(lookingPerUpdate) {
            val node = nodes[rand.nextInt(nodes.size)]
            val distance = node.coordinate.distanceTo(entity)
            if (distance > 15) return@repeat

            val sixNode = node as? SixNode ?: return@repeat
            for (element in sixNode.sideElementList) {
                val load = getInterestingCableLoad(element) ?: continue

                val path = entity.navigator.getPathToXYZ(
                    node.coordinate.x.toDouble(),
                    node.coordinate.y.toDouble(),
                    node.coordinate.z.toDouble()
                )
                    ?: continue

                entity.navigator.setPath(path, 1.0)
                cableCoordinate = node.coordinate
                cableLoad = load
                moveTimeOut = moveTimeOutReset
                resistorLoad.highImpedance()
                resetTimeout = resetTimeoutReset * (0.8 + Math.random() * 0.4)
                return true
            }
        }

        return false
    }

    override fun continueExecuting(): Boolean {
        return cableCoordinate != null
    }

    override fun updateTask() {
        moveTimeOut -= 0.05
        resetTimeout -= 0.05

        val load = getCableLoad()
        if (load == null) {
            cableCoordinate = null
            return
        }

        cableLoad = load
        val coordinate = cableCoordinate ?: return
        val distance = coordinate.distanceTo(entity)

        if (distance > 2 && (entity.navigator.path == null || entity.navigator.path.isFinished)) {
            entity.navigator.tryMoveToXYZ(coordinate.x.toDouble(), coordinate.y.toDouble(), coordinate.z.toDouble(), 1.0)
        }

        if (distance < 2) {
            val voltage = load.voltage
            val nextResistance = (voltage / Eln.LVU).pow(-0.3) * voltage * voltage / 50.0
            if (resistorLoad.resistance < 0.8 * nextResistance) {
                entity.attackEntityFrom(DamageSource.magic, 5.0f)
            } else {
                entity.eatElectricity(resistorLoad.power * 0.05)
            }

            resistorLoad.resistance = nextResistance
            timeRemover.setTimeout(0.16)
            moveTimeOut = moveTimeOutReset
        } else {
            resistorLoad.highImpedance()
        }

        if (moveTimeOut < 0 || resetTimeout < 0) {
            cableCoordinate = null
        }
    }

    private fun getInterestingCableLoad(element: SixNodeElement?): ElectricalLoad? {
        val descriptor = element?.sixNodeElementDescriptor as? ElectricalCableDescriptor ?: return null
        if (descriptor.signalWire) return null

        val load = element.getElectricalLoad(LRDU.Down, 0) ?: return null
        return if (load.voltage >= 30) load else null
    }

    private fun getCableLoad(): ElectricalLoad? {
        val coordinate = cableCoordinate ?: return null
        val node = NodeManager.instance!!.getNodeFromCoordonate(coordinate) as? SixNode ?: return null

        for (element in node.sideElementList) {
            val load = getInterestingCableLoad(element)
            if (load != null) return load
        }

        return null
    }

    override fun timeRemoverRemove() {
        Eln.simulator.removeElectricalLoad(load)
        connection?.let { Eln.simulator.removeElectricalComponent(it) }
        Eln.simulator.removeElectricalComponent(resistorLoad)
        preSimCheck?.let { Eln.simulator.removeSlowPreProcess(it) }
        connection = null
        preSimCheck = null
    }

    override fun timeRemoverAdd() {
        val currentCableLoad = cableLoad ?: return
        Eln.simulator.addElectricalLoad(load)
        val newConnection = ElectricalConnection(load, currentCableLoad)
        connection = newConnection
        Eln.simulator.addElectricalComponent(newConnection)
        Eln.simulator.addElectricalComponent(resistorLoad)
        val newPreSimCheck = PreSimCheck()
        preSimCheck = newPreSimCheck
        Eln.simulator.addSlowPreProcess(newPreSimCheck)
    }

    inner class PreSimCheck : IProcess {
        override fun process(time: Double) {
            if (!timeRemover.isArmed) return
            val currentCableLoad = cableLoad ?: return
            if (!Eln.simulator.isRegistred(currentCableLoad)) {
                timeRemover.shot()
            }
        }
    }
}
