package mods.eln.transparentnode

import mods.eln.ghost.GhostGroup
import mods.eln.misc.*
import mods.eln.node.transparent.*
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.IProcess
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.nbt.NbtElectricalLoad
import mods.eln.sixnode.lampsupply.LampSupplyElement
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class StreetLightWallDescriptor(val name: String, val obj: Obj3D): TransparentNodeDescriptor(name, StreetLightWallElement::class.java, StreetLightWallRender::class.java) {
    private var fixture: Obj3D.Obj3DPart? = null
    private var part2: Obj3D.Obj3DPart? = null
    private var part3: Obj3D.Obj3DPart? = null

    init {
        fixture = obj.getPart("Structure_StreetLightWall_socket")
        part2 = obj.getPart("Glass_StreetLightWall_socket.002")
        part3 = obj.getPart("Light_StreetLightWall_socket.003")
        val gg = GhostGroup()
        gg.addElement(0, 1, 0)
        ghostGroup = gg
        mustHaveWall()
    }

    fun draw(front: Direction, powered: Boolean) {
        if (fixture != null && part2 != null && part3 != null) {
            front.glRotateZnRef()
            GL11.glTranslated(0.0, -0.5, -0.5)
            GL11.glRotated(90.0, 0.0, 0.0, 1.0)
            if (powered) {
                UtilsClient.drawLight(part2)
                UtilsClient.drawLight(part3)
            } else {
                part2?.draw()
                part3?.draw()
            }
            fixture?.draw()
        }
    }
}


class StreetLightWallElement(node: TransparentNode, descriptor: TransparentNodeDescriptor): TransparentNodeElement(node, descriptor) {

    val electricalLoad = NbtElectricalLoad("electricalLoad")
    val loadResistor = Resistor(electricalLoad, null)
    var powerChannel = "light" // TODO: Add a GUI in the render panes and allow the user to specify a different channel.

    init {
        loadResistor.resistance = 1000.0
        slowProcessList.add(StreetLightWallElementProcess(this))
    }

    override fun thermoMeterString(side: Direction): String {
        return ""
    }

    override fun multiMeterString(side: Direction): String {
        return ""
    }

    override fun getElectricalLoad(side: Direction, lrdu: LRDU): ElectricalLoad? {
        return null
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        return false
    }

    override fun getConnectionMask(side: Direction, lrdu: LRDU): Int {
        return 0
    }

    override fun getThermalLoad(side: Direction, lrdu: LRDU): ThermalLoad? {
        return null
    }

    override fun initialize() {
        connect()
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        try {
            stream.writeBoolean(node!!.lightValue > 4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class StreetLightWallElementProcess(val elem: StreetLightWallElement): IProcess {
        var bestChannelHandle: Pair<Double, LampSupplyElement.PowerSupplyChannelHandle>? = null

        private fun findBestSupply(here: Coordinate, forceUpdate: Boolean = false): Pair<Double, LampSupplyElement.PowerSupplyChannelHandle>? {
            val chanMap = LampSupplyElement.channelMap[elem.powerChannel] ?: return null
            val bestChanHand = bestChannelHandle
            // Here's our cached value. We just check if it's null and if it's still a thing.
            if (!(bestChanHand == null || forceUpdate || !chanMap.contains(bestChanHand.second))) {
                return bestChanHand // we good!
            }
            val list = LampSupplyElement.channelMap[elem.powerChannel]?.filterNotNull() ?: return null
            val map = list.map { Pair(it.element.sixNode!!.coordinate.trueDistanceTo(here), it) }
            val sortedBy = map.sortedBy { it.first }
            val chanHand = sortedBy.first()
            bestChannelHandle = chanHand
            return bestChannelHandle
        }

        override fun process(time: Double) {
            val lampSupplyList = findBestSupply(elem.node!!.coordinate)
            val best = lampSupplyList?.second
            if (best != null && best.element.getChannelState(best.id)) {
                best.element.addToRp(elem.loadResistor.resistance)
                elem.electricalLoad.state = best.element.powerLoad.state
            } else {
                elem.electricalLoad.state = 0.0
            }
            var lightDouble = 12 * (Math.abs(elem.loadResistor.voltage) - 180.0) / 20.0
            lightDouble *= 16
            elem.node!!.lightValue = lightDouble.toInt().coerceIn(0, 15)
        }
    }
}

class StreetLightWallRender(tileEntity: TransparentNodeEntity, transparentNodedescriptor: TransparentNodeDescriptor): TransparentNodeElementRender(tileEntity, transparentNodedescriptor) {
    var powered = false

    override fun networkUnserialize(stream: DataInputStream) {
        super.networkUnserialize(stream)
        try {
            powered = stream.readBoolean()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun draw() {
        (transparentNodedescriptor as StreetLightWallDescriptor).draw(front!!, powered)
    }

    override fun cameraDrawOptimisation(): Boolean {
        return false
    }
}