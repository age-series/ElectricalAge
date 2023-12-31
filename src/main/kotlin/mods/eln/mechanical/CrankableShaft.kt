package mods.eln.mechanical

import mods.eln.i18n.I18N.tr
import mods.eln.misc.*
import mods.eln.node.transparent.EntityMetaTag
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.sim.IProcess
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import java.io.DataOutputStream
import kotlin.math.abs

class CrankableShaftDescriptor(name: String, override val obj: Obj3D, private val nominalRads: Float, val nominalP: Float) :
    SimpleShaftDescriptor(name, CrankableShaftElement::class, ShaftRender::class, EntityMetaTag.Basic) {

    override val static = arrayOf(obj.getPart("Stand"), obj.getPart("Cowl"))
    override val rotating = arrayOf(obj.getPart("Shaft"))

    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Player crankable shaft"))
        list.add(tr("Can rotate slowly"))
        list.add(Utils.plotRads(tr("Max rads:  "), nominalRads.toDouble()))
    }
}

class CrankableShaftElement(node: TransparentNode, desc_: TransparentNodeDescriptor) :
    SimpleShaftElement(node, desc_) {
    val desc = desc_ as CrankableShaftDescriptor

    private var playerInputEnergy = 0.0
    private var lastE = 0.0
    private val shaftProcess = IProcess {time ->
        maybePublishE(playerInputEnergy / time)
        playerInputEnergy -= defaultDrag * shaft.rads.coerceAtLeast(1.0)
        if (shaft.rads <= 20.0)
            shaft.energy += (playerInputEnergy)
        playerInputEnergy = 0.0
    }

    init {
        electricalProcessList.add(shaftProcess)
    }

    private fun maybePublishE(energy: Double) {
        if (abs(energy - lastE) / desc.nominalP > 0.01) {
            lastE = energy
            needPublish()
        }
    }

    override fun onBlockActivated(player: EntityPlayer, side: Direction, vx: Float, vy: Float, vz: Float): Boolean {
        playerInputEnergy += desc.nominalP / 20.0 // TPS is 20 assumed here
        return false
    }

    override fun networkSerialize(stream: DataOutputStream) {
        super.networkSerialize(stream)
        stream.writeDouble(lastE)
    }

    override fun getWaila(): Map<String, String> {
        val info = mutableMapOf<String, String>()
        info[tr("Energy")] = Utils.plotEnergy("", shaft.energy)
        info[tr("Speed")] = Utils.plotRads("", shaft.rads)
        return info
    }

    override fun coordonate(): Coordinate {
        return node!!.coordinate
    }
}
