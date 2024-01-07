package mods.eln.gridnode

import mods.eln.Eln
import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.NodeBase
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalGateInput
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sim.process.destruct.VoltageStateWatchDog
import mods.eln.sim.process.destruct.WorldExplosion
import mods.eln.sound.LoopedSound
import net.minecraft.item.ItemStack
import net.minecraft.util.Vec3
import net.minecraftforge.client.IItemRenderer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.math.abs

class GridSwitchDescriptor(
    name: String
): GridDescriptor(
    name,
    Eln.obj.getObj("ElnGridSwitch"),
    GridSwitchElement::class.java,
    GridSwitchRender::class.java,
    "textures/wire.png",
    Eln.instance.highVoltageCableDescriptor,
    12
) {
    var rebound: Double = 0.0  // coef. of restitution
    var nominalAccel: Double = 1.0  // rad/s^2
    var damping: Double = 4.0
    var drag: Double = 0.2  // 1/s
    var nominalU: Double = Eln.MVU
    val resistance = Eln.instance.highVoltageCableDescriptor.electricalRs
    var sinkMin = 20  // Essentially: full load
    var sinkMax = 1000  // Essentially: leakage
    var arcSound = "eln:arc"
    var nominalGridU = 51200
    var separationHigh = 0.5
    var separationLow = 0.4
    var separationMin = 0.01
    var maxVolume = 5.0
    var nominalGridP = 8000
    init {
        renderOffset = Vec3.createVectorHelper(2.5, -0.5, 1.5)
    }
    companion object {
        // val QUARTER_TURN = PI / 2  // WHY does OpenGL use degrees?!
        val QUARTER_TURN = 90.0
    }

    val objectList = listOf<String>(
        "Lid_1_LidMesh_1",
        "Lid_2_LidMesh_2",
        "SwitchBase_SwitchBaseMesh",
        "Belt_1_BeltMesh_1",
        "Belt_2_BeltMesh_2"
    ).map { obj.getPart(it) }

    val rotors = mapOf(  // Name to Pair(Origin,cw?)
        "Contact_M1_ContactMMesh_1" to Pair(Vec3.createVectorHelper(4.5, 1.75, 0.5), true),
        "Contact_M2_ContactMMesh_2.001" to Pair(Vec3.createVectorHelper(4.5, 1.75, 2.5), false),
        "Contact_F1_ContactFMesh_1" to Pair(Vec3.createVectorHelper(0.5, 1.75, 0.5), false),
        "Contact_F2_ContactFMesh_2" to Pair(Vec3.createVectorHelper(0.5, 1.75, 2.5), true),
        "Belt_pulley_1_BeltPulleyMesh_1" to Pair(Vec3.createVectorHelper(4.0, 1.75, 0.5), false),
        "Belt_pulley_2_BeltPulleyMesh_2" to Pair(Vec3.createVectorHelper(4.0, 1.75, 2.5), true)
    ).mapKeys { obj.getPart(it.key) }

    init {
        plus.clear()
        gnd.clear()
        plus.addAll(listOf(
            "p0_Cube",
            "p1_Cube"
        ).map { obj.getPart(it) })
        gnd.addAll(listOf(
            "g0_Cube",
            "g1_Cube"
        ).map { obj.getPart(it) })
        rotating_parts.clear()
        static_parts.clear()
        static_parts.addAll(objectList)
        static_parts.addAll(rotors.keys)
    }

    fun draw(angle: Double, isInventory: Boolean = false) {
        if (!isInventory) {
            GL11.glRotated(90.0, 0.0, 1.0, 0.0)
            GL11.glTranslated(
                renderOffset.xCoord,
                renderOffset.yCoord,
                renderOffset.zCoord
            )
        } else {
            // what a darn hack this is. Would love to figure out what is *actually* going on here.
            GL11.glScaled(0.2, 0.2, 0.2)
            GL11.glTranslated(0.0, -4.0, 0.0)
        }
        objectList.forEach {
            it.draw()
        }
        rotors.forEach {
            val part = it.key
            val origin = it.value.first
            val cw = it.value.second

            preserveMatrix {
                // Strictly speaking, since our rotations are only on Y, we only need to translate to/from that axis.
                // But we might as well do the whole thing since it's all in one call anyway.
                GL11.glTranslated(-origin.xCoord, -origin.yCoord, -origin.zCoord)
                GL11.glRotated(if (cw) {
                    -angle
                } else {
                    angle
                }, 0.0, 1.0, 0.0)
                GL11.glTranslated(origin.xCoord, origin.yCoord, origin.zCoord)
                part.draw()
            }
        }
    }

    override fun rotationIsFixed() = true

    override fun hasCustomIcon() = false

    override fun handleRenderType(item: ItemStack, type: IItemRenderer.ItemRenderType) = true

    override fun renderItem(type: IItemRenderer.ItemRenderType, item: ItemStack, vararg data: Any) {
        draw(0.0, true)
    }
}

class GridSwitchElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): GridElement(node, descriptor, 12) {

    val desc = descriptor as GridSwitchDescriptor
    val interp = PhysicalInterpolator(
        (1.0 / desc.nominalAccel).toFloat(),
        desc.damping.toFloat(),
        desc.drag.toFloat(),
        desc.rebound.toFloat()
    )

    val control = NbtElectricalGateInput("control")
    val power = NbtElectricalLoad("power").apply {
        Eln.instance.meduimVoltageCableDescriptor.applyTo(this)
    }
    val powerSink = Resistor(power, null).apply { resistance = desc.sinkMax.toDouble() }

    val grida = NbtElectricalLoad("grida")
    val gridb = NbtElectricalLoad("gridb")
    val transfer = Resistor(grida, gridb).apply { resistance = desc.resistance }

    val explosion = WorldExplosion(this).machineExplosion()
    val powerWatchdog = VoltageStateWatchDog(power).apply {
        setNominalVoltage(desc.nominalU)
        setDestroys(explosion)
        slowProcessList.add(this)
    }

    val gridaWatchdog = VoltageStateWatchDog(grida).apply {
        setNominalVoltage(51200.0)
        setDestroys(explosion)
        slowProcessList.add(this)
    }

    val gridbWatchdog = VoltageStateWatchDog(gridb).apply {
        setNominalVoltage(51200.0)
        setDestroys(explosion)
        slowProcessList.add(this)
    }

    var lastPos = interp.get()
    var lastTarget = interp.target
    var closed = true

    inner class SlowProcess: IProcess {
        override fun process(time: Double) {
            interp.ff = ((power.voltage / desc.nominalU) * desc.nominalAccel).toFloat()
            interp.target = control.normalized.toFloat()
            interp.step(time.toFloat())
            powerSink.resistance = desc.sinkMin.toDouble() + (desc.sinkMax - desc.sinkMin).toDouble() * (1.0 - abs(interp.get() - interp.target))
            if(abs(lastPos - interp.get()) > 0.1 || abs(lastTarget - interp.target) > 0.001) {
                lastPos = interp.get()
                lastTarget = interp.target
                needPublish()
            }
            val maxU = arrayOf(grida.voltage, gridb.voltage).max()
            if(closed) {
                if(interp.get() > desc.separationHigh * (maxU / desc.nominalGridU)) {
                    closed = false
                    transfer.ultraImpedance()
                }
            } else {
                if(interp.get() < desc.separationLow * (maxU / desc.nominalGridU)) {
                    closed = true
                    transfer.resistance = desc.resistance
                }
            }
        }
    }
    val slowProcess = SlowProcess()

    var ghostPower: GhostPowerNode? = null
    var ghostControl: GhostPowerNode? = null

    init {
        electricalLoadList.addAll(listOf(power, control, grida, gridb))
        electricalComponentList.addAll(listOf(powerSink, transfer))
        slowProcessList.add(slowProcess)
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU) = null

    override fun getThermalLoad(side: Direction, lrdu: LRDU) = null

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun thermoMeterString(side: Direction): String = ""
    override fun multiMeterString(side: Direction): String =
        Utils.plotUIP(grida.voltage, grida.current) + " / " + Utils.plotUIP(gridb.voltage, gridb.current) + " @" + interp.get() + "/" +
            control.normalized + " " + Utils.plotUIP(power.voltage, power.current)

    override fun initialize() {
        ghostPower = GhostPowerNode(
            node!!.coordinate, front,
            Coordinate(-1, 0, -1, 0),
            power, NodeBase.maskElectricalPower
        )
        ghostPower!!.initialize()
        ghostControl = GhostPowerNode(
            node!!.coordinate, front,
            Coordinate(-1, 0, 0, 0),
            control, NodeBase.maskElectricalGate
        )
        ghostControl!!.initialize()
        Utils.println("GS.i: ghost power at ${ghostPower!!.coord}, control at ${ghostControl!!.coord}")
        super.initialize()
    }

    override fun hasGui() = false

    override fun getGridElectricalLoad(side: Direction): ElectricalLoad? = when (side) {
        front.left() -> grida
        front.right() -> gridb
        else -> null
    }

    override fun getRenderCablePoint(side: Direction, i: Int): Vec3 {
        assert(i in 0..1) { "Index out of bounds" }
        val idx = when(side) {
            front.left() -> 0
            front.right() -> 1
            else -> error("Invalid side")
        }
        val part = (if(i == 0) { desc.plus } else { desc.gnd })[idx]
        val ro = desc.renderOffset
        val pt = part.boundingBox().centre().addVector(
            ro.xCoord, ro.yCoord, ro.zCoord
        )
        // Rotate this point by a quarter turn around y to correspond to our rendering offset
        return Vec3.createVectorHelper(
            pt.zCoord, pt.yCoord, -pt.xCoord
        )
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeFloat(interp.ff)
        stream.writeFloat(interp.target)
        stream.writeFloat(interp.factorFiltered)
        stream.writeFloat(interp.factorPos)
        stream.writeFloat(interp.factorSpeed)
        stream.writeDouble(arrayOf(abs(grida.voltage * grida.current), abs(gridb.voltage * gridb.current)).max())
        stream.writeBoolean(closed)
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        if (Eln.wailaEasyMode) {
            info[tr("Left")] = Utils.plotUIP(grida.voltage, grida.current)
            info[tr("Right")] = Utils.plotUIP(gridb.voltage, gridb.current)
            info[tr("Transfer")] = Utils.plotPower(transfer.power)
        }
        info[tr("Drive")] = Utils.plotUIP(power.voltage, power.current, powerSink.resistance) + " " + Utils.plotOhm(powerSink.resistance)
        info[tr("Signal")] = Utils.plotSignal(control.voltage)
        info[tr("Closed?")] = if(closed) { "Yes" } else { "No" }
        return info
    }
}

class GridSwitchRender(entity: TransparentNodeEntity, descriptor: TransparentNodeDescriptor) : GridRender(entity, descriptor) {
    init {
        this.transparentNodedescriptor = descriptor as GridSwitchDescriptor
    }

    val desc = descriptor as GridSwitchDescriptor
    val interp = PhysicalInterpolator(
        (1.0 / desc.nominalAccel).toFloat(),
        desc.damping.toFloat(),
        desc.drag.toFloat(),
        desc.rebound.toFloat()
    )
    var power = 0.0
    var closed = true

    inner class ArcSound(samp: String, coord: Coordinate): LoopedSound(samp, coord) {
        override fun getVolume(): Float = if(closed && interp.get() > desc.separationMin && power > 0) {
            arrayOf(desc.maxVolume.toFloat(), ((0.5 + interp.get()) * power / desc.nominalGridP).toFloat()).min()
        } else {
            0.0f
        }
    }

    init {
        addLoopedSound(ArcSound(desc.arcSound, coordinate()))
    }

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        interp.ff = stream.readFloat()
        interp.target = stream.readFloat()
        interp.factorFiltered = stream.readFloat()
        interp.factorPos = stream.readFloat()
        interp.factorSpeed = stream.readFloat()
        power = stream.readDouble()
        closed = stream.readBoolean()
    }

    override fun refresh(deltaT: Float) {
        super.refresh(deltaT)
        interp.step(deltaT)
    }

    override fun draw() {
        preserveMatrix {
            front?.glRotateXnRef()
            desc.draw(interp.get() * GridSwitchDescriptor.QUARTER_TURN)
        }
        drawCables()
    }
}
